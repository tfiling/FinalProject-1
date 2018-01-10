package FinalProject.BL.DataCollection;

import org.apache.log4j.Logger;

import java.util.*;

public class IterationAgentsPrice {
    private Map<Integer, List<AgentPrice>> iterationToAgentsPrice;
    private Map<Integer,List<NeighborhoodEpeak>> iterationsToNeighborhoodsPeak;
    private static final Logger logger = Logger.getLogger(IterationAgentsPrice.class);

    public IterationAgentsPrice() {
        iterationToAgentsPrice = new HashMap<Integer, List<AgentPrice>>();
        iterationsToNeighborhoodsPeak = new HashMap<Integer,List<NeighborhoodEpeak>>();
    }

    public List<AgentPrice> getAgentsPrices(int iterationNum){
        return iterationToAgentsPrice.get(iterationNum);
    }

    public boolean isIterationOverNoEpeak(int iterationNum, int numOfAgents){
        List<AgentPrice> agentsPrices = iterationToAgentsPrice.get(iterationNum);
        if (agentsPrices != null){
            return agentsPrices.size() == numOfAgents; //&& epeakCalculated(iterationNum);
        }
        return false;
    }

    public boolean ePeakCalculated(int iterNum) {
        List<NeighborhoodEpeak> ne = iterationsToNeighborhoodsPeak.get(iterNum);
        if (ne == null){return false;}
        for (NeighborhoodEpeak e: ne){
            if (e.getEpeak() == -1 || !e.gotAllEpeaks()){
                return false;
            }
        }
        return true;
    }

    public void addAgentPrice(int iterationNum, AgentPrice agentPrice ){
        if (iterationToAgentsPrice.containsKey(iterationNum)){
            iterationToAgentsPrice.get(iterationNum).add(agentPrice);
        }
        else{
            List<AgentPrice> agentsPrices = new LinkedList<AgentPrice>();
            agentsPrices.add(agentPrice);
            iterationToAgentsPrice.put(iterationNum, agentsPrices);
        }
    }

    public void addNeighborhoodAndEpeak(int iterationNum, double epeak, Set<String> neighborhood){
        List<NeighborhoodEpeak> neigEpeak = iterationsToNeighborhoodsPeak.get(iterationNum);
        boolean exist = false;
        if (neigEpeak == null){
            neigEpeak = new LinkedList<NeighborhoodEpeak>();
            neigEpeak.add(new NeighborhoodEpeak(neighborhood, epeak));
            iterationsToNeighborhoodsPeak.put(iterationNum, neigEpeak);
        }
        else{
            for (NeighborhoodEpeak ne: neigEpeak) {
                if (ne.getNeighborhood().containsAll(neighborhood)){
                    if(epeak != -1 && epeak != ne.getEpeak()){
                        logger.warn("got different Epeak on same neighborhood");
                        ne.setEpeak(epeak);
                    }
                    ne.addEpeak();
                    exist = true;
                    break;
                }
            }
            if (!exist){
                neigEpeak.add(new NeighborhoodEpeak(neighborhood, epeak));
            }
        }
    }

    public Map<Integer, List<AgentPrice>> getIterationToAgentsPrice() {
        return iterationToAgentsPrice;
    }

    public void setIterationToAgentsPrice(Map<Integer, List<AgentPrice>> iterationToAgentsPrice) {
        this.iterationToAgentsPrice = iterationToAgentsPrice;
    }

    public Map<Integer, List<NeighborhoodEpeak>> getIterationsToNeighborhoodsPeak() {
        return iterationsToNeighborhoodsPeak;
    }

    public void setIterationsToNeighborhoodsPeak(Map<Integer, List<NeighborhoodEpeak>> iterationsToNeighborhoodsPeak) {
        this.iterationsToNeighborhoodsPeak = iterationsToNeighborhoodsPeak;
    }


    public double getTotalEpeakInIter(int iterNum) {
        double totalEpeak = 0;
        List<NeighborhoodEpeak> ne = iterationsToNeighborhoodsPeak.get(iterNum);
        for (NeighborhoodEpeak n : ne){
            if (n.getEpeak() == -1){
                logger.warn("getTotalEpeakInIter encountered -1 in epeak property");
                continue;
            }
            totalEpeak += n.getEpeak();
        }
        return totalEpeak;
    }
}
