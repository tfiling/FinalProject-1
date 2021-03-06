package FinalProject.BL.DataCollection;

import FinalProject.BL.IterationData.IterationCollectedData;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class DataCollector {

    private Map<String, Integer> numOfAgentsInProblems;
    private Map<ProblemAlgorithm, IterationAgentsPrice> probAlgoToItAgentPrice;
    private Map<ProblemAlgorithm, AlgorithmProblemResult> probAlgoToResult;
    private Map<String, double[]> probToPriceScheme;
    private static final Logger logger = Logger.getLogger(DataCollector.class);

    public DataCollector(Map<String, Integer> numOfAgentsInProblems, Map<String, double[]> prices) {
        this.numOfAgentsInProblems = numOfAgentsInProblems;
        this.probToPriceScheme = prices;
        this.probAlgoToItAgentPrice = new HashMap<ProblemAlgorithm, IterationAgentsPrice>();
        this.probAlgoToResult = new HashMap<ProblemAlgorithm, AlgorithmProblemResult>();
    }

    public double addData (IterationCollectedData data) {
        ProblemAlgorithm tempPA = new ProblemAlgorithm(data.getProblemId(), data.getAlgorithm());
        IterationAgentsPrice tempIAP = addAgentPrice(data, tempPA);
        addNeighborhoodIfNotExist(data, tempPA);

        if (isIterationFinished(tempPA, tempIAP, data)) { //last agent finished iteration
            addProbResult(tempPA, tempIAP, data);
            pupulateTotalGradeForIteration(data, tempPA, tempIAP);
            return -1.0;
        }
        return 0;
    }

    private void pupulateTotalGradeForIteration(IterationCollectedData data, ProblemAlgorithm pa, IterationAgentsPrice iap) {
        AlgorithmProblemResult apr = probAlgoToResult.get(pa);
        if(apr == null){
            logger.error("AlgorithmProblemResult is null when trying to calc Total grade for iter: " + data.getIterNum());
        }
        double totalGrade = calculateCsumForAllAgents(pa, iap, data.getIterNum());
        //now we add all the ePeaks
        totalGrade += iap.getTotalEpeakInIter(data.getIterNum());
        apr.setTotalGradeToIter(data.getIterNum(), totalGrade);
        if (totalGrade < apr.getBestGrade()){
            apr.setBestGrade(totalGrade);
            apr.setIterationsTillBestPrice(data.getIterNum());
            setLowestHighestInBestIter(pa, apr);
            setAvgPriceInIter(pa, apr, data.getIterNum());
        }else{ //not the best iter
            setAvgPriceInIter(pa, apr, data.getIterNum());
        }
    }

    private IterationAgentsPrice addAgentPrice(IterationCollectedData data, ProblemAlgorithm tempPA) {
        IterationAgentsPrice tempIAP;
        if (probAlgoToItAgentPrice.containsKey(tempPA)){
            tempIAP = probAlgoToItAgentPrice.get(tempPA);
            tempIAP.addAgentPrice(data.getIterNum(),
                    new AgentPrice(data.getAgentName(), data.getPrice(),
                            data.getPowerConsumptionPerTick()));
        }else{
            tempIAP = new IterationAgentsPrice();
            tempIAP.addAgentPrice(data.getIterNum(),
                    new AgentPrice(data.getAgentName(), data.getPrice(),
                            data.getPowerConsumptionPerTick()));
            probAlgoToItAgentPrice.put(tempPA, tempIAP);
        }
        return tempIAP;
    }

    private void addNeighborhoodIfNotExist(IterationCollectedData data, ProblemAlgorithm tempPA) {
        Set<String> neighborhood = data.getNeighborhood();
        String name = data.getAgentName();
        int shtrudel = name.indexOf('@');
        if (shtrudel != -1){
            name = name.substring(0, shtrudel);
        }
        neighborhood.add(name);
        IterationAgentsPrice IAP = probAlgoToItAgentPrice.get(tempPA);
        if (IAP == null){
            logger.warn("IAP is null when adding Neighborhood");
            return;
        }
        IAP.addNeighborhoodAndEpeak(data.getIterNum(), data.getEpeak(), neighborhood);
    }

    private void setAvgPriceInIter(ProblemAlgorithm PA, AlgorithmProblemResult result, int iterNum) {
        List<AgentPrice> prices;
        IterationAgentsPrice iter = probAlgoToItAgentPrice.get(PA);
        double avg = 0;
        double price = 0;

        if (iter != null) {
            prices = iter.getAgentsPrices(iterNum);
            for (AgentPrice ag : prices) {
                price = ag.getPrice();
                avg += price;
            }
            result.getAvgPricePerIteration().put(
                    iterNum, avg/prices.size());
        }
    }

    private void setLowestHighestInBestIter(ProblemAlgorithm tempPA, AlgorithmProblemResult result) {
        double avg = 0;
        double min = Double.MAX_VALUE;
        double max = 0;
        double price = 0;
        List<AgentPrice> prices;
        IterationAgentsPrice iter = probAlgoToItAgentPrice.get(tempPA);

        if (iter != null){
            prices = iter.getAgentsPrices(result.getIterationsTillBestPrice());
            for (AgentPrice ag: prices) {
                price = ag.getPrice();
                avg += price;
                min = Double.min(min, price);
                max = Double.max(max, price);
                if (price == min){ //changed min
                    result.setLowestCostForAgentInBestIteration(price);
                    result.setLowestCostForAgentInBestIterationAgentName(ag.getAgentName());
                }
                if (price == max){ //changed max
                    result.setHighestCostForAgentInBestIteration(price);
                    result.setHighestCostForAgentInBestIterationAgentName(ag.getAgentName());
                }
            }
            result.getAvgPricePerIteration().put(
                    result.getIterationsTillBestPrice(), avg/prices.size());
        }
    }

    private double calculateCsumForAllAgents(ProblemAlgorithm tempPA, IterationAgentsPrice IAP, int iterNum) {
        List<AgentPrice> prices = IAP.getAgentsPrices(iterNum);
        List<double[]> agentPrices = prices.stream().map(a -> a.getSchedule()).collect(Collectors.toList());
        double[] priceScheme = probToPriceScheme.get(tempPA.getProblemId());
        return PowerConsumptionUtils.calculateCSum(agentPrices, priceScheme);
    }



    private boolean isIterationFinished(ProblemAlgorithm PA, IterationAgentsPrice IAP,
                                        IterationCollectedData data) {
        if(IAP == null) {return false;}
        List<AgentPrice> prices = IAP.getAgentsPrices(data.getIterNum());
        Integer numOfAgents = numOfAgentsInProblems.get(PA.getProblemId());
        if (prices != null && numOfAgents != null &&
                prices.size() == numOfAgents){ //iteration is over
            return true;
        }
        return false;
    }


    //if first then create new probResult
    private void addProbResult(ProblemAlgorithm PA, IterationAgentsPrice IAP,
                                     IterationCollectedData data) {
        if(IAP == null) {return;}
        List<AgentPrice> prices = IAP.getAgentsPrices(data.getIterNum());
        Integer numOfAgents = numOfAgentsInProblems.get(PA.getProblemId());
        if (prices != null && numOfAgents != null &&
                prices.size() == numOfAgents){ //iteration is over  with no epeak calculated
            if (!probAlgoToResult.containsKey(PA)){ //no prob result yet
                AlgorithmProblemResult result = new AlgorithmProblemResult(PA);
                result.setIterationsTillBestPrice(data.getIterNum());
                probAlgoToResult.put(PA, result);
            }
        }
    }

    public AlgorithmProblemResult getAlgoProblemResult(String problemID, String algoName){
        ProblemAlgorithm PA = new ProblemAlgorithm(problemID, algoName);
        return probAlgoToResult.get(PA);
    }

    public int getNumOfAgentsInProblem(String problemName){
        if (numOfAgentsInProblems.containsKey(problemName)){
            return numOfAgentsInProblems.get(problemName);
        }
        return 0;
    }

    public Map<String, Integer> getNumOfAgentsInProblems() {
        return numOfAgentsInProblems;
    }

    public void setNumOfAgentsInProblems(Map<String, Integer> numOfAgentsInProblems) {
        this.numOfAgentsInProblems = numOfAgentsInProblems;
    }

    public Map<ProblemAlgorithm, IterationAgentsPrice> getProbAlgoToItAgentPrice() {
        return probAlgoToItAgentPrice;
    }

    public void setProbAlgoToItAgentPrice(Map<ProblemAlgorithm, IterationAgentsPrice> probAlgoToItAgentPrice) {
        this.probAlgoToItAgentPrice = probAlgoToItAgentPrice;
    }

}
