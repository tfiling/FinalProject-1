package FinalProject.BL.IterationData;

import java.io.Serializable;

public class AgentIterationData implements Serializable {

    private int iterNum;
    private String agentName;
    private double price;
    private double[] powerConsumptionPerTick;

    public AgentIterationData(int iterNum, String agentName, double price, double[] powerConsPerDevice)
    {
        this.iterNum = iterNum;
        this.agentName = agentName;
        this.price = price;
        this.powerConsumptionPerTick = powerConsPerDevice;
    }

    public int getIterNum() {
        return iterNum;
    }

    public void setIterNum(int iterNum) {
        this.iterNum = iterNum;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double[] getPowerConsumptionPerTick() {
        return powerConsumptionPerTick;
    }

    public void setPowerConsumptionPerTick(double[] powerConsumptionPerTick) {
        this.powerConsumptionPerTick = powerConsumptionPerTick;
    }


}
