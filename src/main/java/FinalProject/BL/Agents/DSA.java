package FinalProject.BL.Agents;
import FinalProject.BL.Experiment;
import FinalProject.BL.IterationData.AgentIterationData;
import FinalProject.BL.IterationData.IterationCollectedData;
import FinalProject.BL.DataObjects.*;
import FinalProject.Utils;
import jade.lang.acl.ACLMessage;
import org.apache.log4j.Logger;
import java.util.*;
import java.util.stream.Collectors;

import static FinalProject.BL.DataCollection.PowerConsumptionUtils.calculateTotalConsumptionWithPenalty;

public class DSA extends SmartHomeAgentBehaviour {

    private final static Logger logger = Logger.getLogger(DSA.class);

    public DSA()
    {
        super();//invoke the Behaviour default constructor
    }

    public DSA(SmartHomeAgent agent)
    {
        this.agent = agent;
        this.currentNumberOfIter =0;
        this.FINAL_TICK = agent.getAgentData().getBackgroundLoad().length;
        this.helper = new AlgorithmDataHelper(agent);

    }

    @Override
    public void doIteration() {
        if (agent.isZEROIteration()) {
            logger.info("Starting work on Iteration: 0");
            buildScheduleFromScratch();
            agent.setZEROIteration(false);
            logger.info("FINISH ITER 0");
        }
        else {
            receivedAllMessagesAndHandleThem();
            logger.info("Starting work on Iteration: " + this.currentNumberOfIter);
            tryBuildSchedule();
            logger.info("FINISHed ITER " + currentNumberOfIter);
        }
        this.currentNumberOfIter ++;
    }

    private void receivedAllMessagesAndHandleThem() {
        List<ACLMessage> messageList = waitForNeighbourMessages();
        parseMessages(messageList);
        helper.calcPriceSchemeForAllNeighbours();
        helper.calcTotalPowerConsumption(agent.getcSum());
        sentEpeakToDC(currentNumberOfIter-1);
    }


    private void sentEpeakToDC(int iterationNum) {
        IterationCollectedData agentIterSum = new IterationCollectedData(iterationNum, agent.getName(),agentIterationData.getPrice(), agentIterationData.getPowerConsumptionPerTick(), agent.getProblemId(), agent.getAlgoId(),(agent.getAgentData().getNeighbors().stream().map(AgentData::getName).collect(Collectors.toSet())), (helper.totalPriceConsumption - this.agent.getcSum()));
        this.agentIteraionCollected = agentIterSum;
        sendIterationToCollector();
    }

    private void tryBuildSchedule() {
        helper.goBackToStartValues();
        tryBuildScheduleBasic();
        beforeIterationIsDone();
    }

    private void beforeIterationIsDone()
    {
        addBackgroundLoadToPriceScheme(this.iterationPowerConsumption);
        double price = calcPrice(this.iterationPowerConsumption);
        double[] arr = helper.clonArray(this.iterationPowerConsumption);
        logger.info("my PowerCons is: " + arr[0] + "," +  arr[1] + "," + arr[2] +"," + arr[3] + "," + arr[4] +"," + arr[5] + "," +arr[6] );
        logger.info("my PRICE is: " + price);
        agentIterationData = new AgentIterationData(currentNumberOfIter, agent.getName(),price, arr);
        agent.setCurrIteration(agentIterationData);
        agentIteraionCollected = new IterationCollectedData(currentNumberOfIter, agent.getName(),price, arr, agent.getProblemId(), agent.getAlgoId(), (agent.getAgentData().getNeighbors().stream().map(AgentData::getName).collect(Collectors.toSet())), -1);
    }

    private int drawCoin() {
        int[] notRandomNumbers = new int [] {0,0,0,0,1,1,1,1,1,1};
        double idx = Math.floor(Math.random() * notRandomNumbers.length);
        return notRandomNumbers[(int) idx];
    }

    public void buildScheduleFromScratch() {
        initHelper();
        tryBuildScheduleBasic();
        beforeIterationIsDone();
    }

    private void tryBuildScheduleBasic() {
        this.iterationPowerConsumption = new double[this.agent.getAgentData().getBackgroundLoad().length];
        List<PropertyWithData> helperNonPassiveOnlyProps = helper.getAllProperties().stream()
                .filter(p -> !p.isPassiveOnly())
                .collect(Collectors.toList());
        for(PropertyWithData prop : helperNonPassiveOnlyProps) {
            if (prop.getPrefix() == Prefix.BEFORE) {
                specialCaseOfBefore(prop);
            }
            //lets see what is the state of the curr & related sensors till then
            prop.calcAndUpdateCurrState(prop.getMin(),START_TICK, this.iterationPowerConsumption, true);
            double ticksToWork = helper.calcHowLongDeviceNeedToWork(prop);
            Map<String, Double> sensorsToCharge = new HashMap<>();
            //check if there is sensor in the same ACT that is negative (usually related to charge)
            for (Map.Entry<String,Double> entry : prop.getRelatedSensorsDelta().entrySet())
            {
                if (entry.getValue() < 0) {
                    double rateOfCharge = calcHowOftenNeedToCharge(entry.getKey(),entry.getValue(), ticksToWork, prop.getTargetTick());
                    if (rateOfCharge > 0) {
                        sensorsToCharge.put(entry.getKey(), rateOfCharge);
                    }
                }
            }

            //TODO: move from here
            if (agent.isZEROIteration()) {
                startWorkZERO(prop, sensorsToCharge, ticksToWork);
            }
            else{
              startWorkNonZeroIter(prop, sensorsToCharge, ticksToWork);
            }
        }


    }

    private void specialCaseOfBefore(PropertyWithData prop) {
        prop.calcAndUpdateCurrState(prop.getTargetValue(),START_TICK, this.iterationPowerConsumption, true);

    }

    private void startWorkNonZeroIter(PropertyWithData prop, Map<String, Double> sensorsToCharge, double ticksToWork) {
        boolean buildNewShed = drawCoin() == 1 ? true : false;
        if (buildNewShed)
        {
            prop.activeTicks.clear();
            List<Set<Integer>> subsets;
            List<Integer> newTicks;

            if (ticksToWork <= 0)
            {
                subsets = checkAllOptions(prop);
                if (subsets == null) return;
            }
            else{
                List<Integer> rangeForWork =  calcRangeOfWork(prop);
                subsets = helper.getSubsets(rangeForWork, (int) ticksToWork);
            }
            newTicks = calcBestPrice(prop, subsets);
            updateTotals(prop, newTicks, sensorsToCharge);
        }
        else{
            updateTotals(prop, prop.activeTicks, sensorsToCharge);
        }

    }

    private List<Integer> calcRangeOfWork(PropertyWithData prop) {
        List<Integer> rangeForWork = new ArrayList<>();

        switch (prop.getPrefix())
        {
            case BEFORE: //NOT Include the hour
                for (int i=0; i< prop.getTargetTick(); ++i)
                {
                    rangeForWork.add(i);
                }
                break;
            case AFTER:
                for (int i= (int) prop.getTargetTick() ; i< agent.getAgentData().getBackgroundLoad().length ; ++i)
                {
                    rangeForWork.add(i);
                }
                break;
            case AT:
                rangeForWork.add((int) prop.getTargetTick());
                break;
        }

        return rangeForWork;
    }

    private List<Integer> calcBestPrice(PropertyWithData prop, List<Set<Integer>> subsets)
    {
        double bestPrice = helper.totalPriceConsumption;
        List<Integer> newTicks = new ArrayList<>();
        double [] prevPowerConsumption = helper.clonArray(agent.getCurrIteration().getPowerConsumptionPerTick());
        double [] newPowerConsumption = helper.clonArray(agent.getCurrIteration().getPowerConsumptionPerTick());
        //get the specific tick this device work in
        List<Integer> prevTicks = helper.getDeviceToTicks().get(prop.getActuator());
        //remove them from the array
        for (Integer tick : prevTicks)
        {
            newPowerConsumption[tick] = newPowerConsumption[tick] -  prop.getPowerConsumedInWork();
        }

        double [] copyOfNew = helper.clonArray(newPowerConsumption);
        boolean improved = false;
        for(Set<Integer> ticks : subsets)
        {
            //Adding the ticks to array
            for (Integer tick : ticks)
            {
                double temp = newPowerConsumption[tick];
                newPowerConsumption[tick] = Double.sum(temp,  prop.getPowerConsumedInWork());
            }
            double res = calculateTotalConsumptionWithPenalty(agent.getcSum(), newPowerConsumption, prevPowerConsumption
                    ,helper.getNeighboursPriceConsumption(), agent.getAgentData().getPriceScheme());

            if (res <= helper.totalPriceConsumption && res <= bestPrice)
            {
                bestPrice = res;
                newTicks.clear();
                newTicks.addAll(ticks);
                improved = true;
            }

            //goBack
            newPowerConsumption = helper.clonArray(copyOfNew);
        }

        if(!improved)
        {
            newTicks = helper.getDeviceToTicks().get(prop.getActuator());
        }

        return newTicks;
    }

    private List<Set<Integer>> checkAllOptions(PropertyWithData prop) {
        List<Integer> rangeForWork =  calcRangeOfWork(prop);
         double currState = prop.getSensor().getCurrentState();
         double minVal = prop.getTargetValue();
         double deltaIfNoActiveWorkIsDone = (currState - minVal) - ((Math.abs(prop.getDeltaWhenWorkOffline())) * rangeForWork.size());
         int ticksToWork = 0;
         if (deltaIfNoActiveWorkIsDone>0) return null;
         for (int i= 0; i<rangeForWork.size(); ++i)
         {
             ticksToWork++;
             deltaIfNoActiveWorkIsDone = Double.sum(deltaIfNoActiveWorkIsDone, prop.getDeltaWhenWork());
             if(deltaIfNoActiveWorkIsDone > 0)
             {
                 break;
             }
         }
        return helper.getSubsets(rangeForWork, ticksToWork);
    }

    private double calcHowOftenNeedToCharge(String key, double delta, double ticksToWork, double targetTick) {
        double tick=0;
        PropertyWithData prop = null;
        try{
            prop = helper.getAllProperties().stream().filter(x->x.getName().equals(key)).findFirst().get();
        }
        catch (Exception e)
        {
            logger.warn(agent.getAgentData().getName() + "Try to look for the related sensros , but not found like this");
            return -1;
        }
        double currState = prop.getSensor().getCurrentState();
        //first, lets charge to the max
        if (currState< prop.getMax())
        {
            double howLong = Math.ceil((prop.getMax()- currState) / prop.getDeltaWhenWork());
        //    if (targetTick - howLong >0) {
        //        prop.updateValueToSensor(this.iterationPowerConsumption, currState, howLong, (int) targetTick- (int)howLong, true);
           //     currState = prop.getMax();
         //   }

        }
        //lets see how many time we'll need to charge it.
        for (int i=0 ; i< ticksToWork; ++i)
        {
           currState += delta;
           if (currState < prop.getMin())
           {
               tick ++;
               currState = prop.getMax();
           }
        }

        //no need to charge it between the work. lets just update the sensor
        if (tick==0)
        {
            Map<Sensor, Double> toSend = new HashMap<>();
            toSend.put(prop.getSensor(), currState);
            prop.getActuator().act(toSend);
        }

        return tick;
    }

    @Override
    public boolean done() {
        boolean agentFinishedExperiment = (this.currentNumberOfIter > Experiment.maximumIterations);
        if (agentFinishedExperiment)
        {
            logger.info(Utils.parseAgentName(this.agent) + " ended its final iteration");
            logger.info(Utils.parseAgentName(this.agent) + " about to send data to DataCollector");

            receivedAllMessagesAndHandleThem();
            logger.info(Utils.parseAgentName(this.agent) + " Just sent to DataCollector final calculations");

            this.agent.doDelete();
        }
        return agentFinishedExperiment;
    }

    public AlgorithmDataHelper getHelper() {
        return helper;
    }

    @Override
    public DSA cloneBehaviour() {
        DSA newInstance = new DSA();
        newInstance.finished = this.finished;
        newInstance.currentNumberOfIter = this.currentNumberOfIter;
        newInstance.FINAL_TICK = this.FINAL_TICK;
        newInstance.agentIterationData = null;//will be created as part of the behaviour run(see buildScheduleFromScratch)
        return newInstance;
    }

    public double[] getPowerConsumption() { return this.iterationPowerConsumption;}
}
