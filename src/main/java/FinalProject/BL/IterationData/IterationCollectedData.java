package FinalProject.BL.IterationData;

import java.util.Set;

public class IterationCollectedData extends AgentIterationData {

    private String problemId;
    private String algorithm;
    private Set<String> neighborhood;
    private double epeak;

    public IterationCollectedData(int iterNum, String agentName, double price, double[] powerConsPerDevice,
                                  String problemId, String algo, Set<String> neighborhood, double epeak ) {
        super(iterNum, agentName, price, powerConsPerDevice);
        this.problemId = problemId;
        this.algorithm = algo;
        this.neighborhood = neighborhood;
        this.epeak = epeak;
    }

    public String getProblemId() {
        return problemId;
    }

    public void setProblemId(String problemId) {
        this.problemId = problemId;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public void setNeighborhood(Set<String> neighborhood) {
        this.neighborhood = neighborhood;
    }

    public Set<String> getNeighborhood() {
        return neighborhood;
    }

    public double getEpeak() {
        return epeak;
    }

    public void setEpeak(double epeak) {
        this.epeak = epeak;
    }

}
