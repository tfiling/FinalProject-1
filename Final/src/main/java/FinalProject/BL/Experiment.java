package FinalProject.BL;

import FinalProject.BL.Agents.SmartHomeAgent;
import FinalProject.BL.Agents.SmartHomeAgentBehaviour;
import FinalProject.BL.DataCollection.AlgorithmProblemResult;
import FinalProject.BL.DataCollection.DataCollector;
import FinalProject.BL.Problems.AgentData;
import FinalProject.BL.Problems.Problem;
import FinalProject.Service;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class Experiment extends Thread{
    public static int maximumIterations = 0;
    private Service service;
    private DataCollector dataCollector;
    private List<Problem> problems;
    private List<SmartHomeAgentBehaviour> algorithms;
    private List<AlgorithmProblemResult> algorithmProblemResults;

    private boolean experimentCompleted = false;

    private static Logger logger = Logger.getLogger(Experiment.class);

    private Thread experimentThread;

    public Experiment(Service service, List<Problem> problems, List<SmartHomeAgentBehaviour> algorithms)
    {
        //TODO gal
        this.service = service;
        this.problems = problems;
        this.algorithms = algorithms;
        this.algorithmProblemResults = new ArrayList<>();
        experimentThread = new Thread(new ExperimentRunnable());
    }

    public void runExperiment()
    {
        //TODO gal
        this.experimentThread.start();
    }

    public void algorithmRunEnded(AlgorithmProblemResult result)
    {
        //TODO gal
        algorithmProblemResults.add(result);
    }

    public void stopExperiment()
    {
        //TODO gal
        try
        {
            this.experimentThread.interrupt();
        }
        finally
        {
            //ignore a possible SecurityException
        }
    }

    private class ExperimentRunnable implements Runnable, PlatformController.Listener
    {
        private AgentContainer mainContainer;
        private List<AgentController> agentControllers;
        private int aliveAgents = 0;
        private CyclicBarrier waitingBarrier;// used by the experiment thread to wait for the current configuration to end before starting a new one
        private boolean experimentConfigurationRunning = false;


        @Override
        public void run() {
            //TODO gal
            try
            {
                initialize();

                for (Problem currentProblem : problems)
                {
                    for (SmartHomeAgentBehaviour currentAlgorithmBehaviour : algorithms)
                    {
                        AlgorithmProblemResult result = null;
                        for (AgentData agentData : currentProblem.getAgentsData())
                        {
                            Object[] agentInitializationArgs = new Object[2];
                            agentInitializationArgs[0] = currentAlgorithmBehaviour.cloneBehaviour();
                            agentInitializationArgs[1] = agentData;
                            AgentController agentController = this.mainContainer.createNewAgent(agentData.getName(),
                                    SmartHomeAgent.class.getName(),
                                    agentInitializationArgs);
                            this.agentControllers.add(agentController);
                        }

                        this.aliveAgents = this.agentControllers.size();
                        this.experimentConfigurationRunning = true;
                        for (AgentController controller : this.agentControllers)
                        {//start all agents
                            controller.start();
                        }

                        try {
//                                Thread.sleep(4000);
                            this.waitingBarrier.await();
                        } catch (InterruptedException | BrokenBarrierException e) {
                            logger.error("an exception was thrown while experiment was waiting for current configuration run to end", e);
                            this.experimentConfigurationRunning = false;
                            killAllAgents();
                            this.waitingBarrier.reset();
                        }
                        algorithmRunEnded(result);
                    }
                }
            }
            catch (ControllerException e)
            {
                // end the experiment
                stopRun();
                service.experimentEndedWithError(e);
            }
        }

        private void initialize() throws ControllerException {
            // Get a hold on JADE runtime
            Runtime rt = Runtime.instance();

            // Exit the JVM when there are no more containers around
            rt.setCloseVM(true);

            // Create a default profile
            Profile profile = new ProfileImpl(true);

            //has to be created even if not used
            this.mainContainer = rt.createMainContainer(profile);
            this.mainContainer.addPlatformListener(this);
            this.agentControllers = new ArrayList<>();

            this.waitingBarrier = new CyclicBarrier(2);
        }

        private void stopRun()
        {
            try
            {
                this.mainContainer.kill();
            }
            catch (StaleProxyException e)
            {
                //ignore the exception
            }

            //not used for now since container.kill might be a better choice
            // TODO gal remove when surely not needed
            /*
            killAllAgents();
            */
        }

        private void killAllAgents()
        {
            for (AgentController controller : this.agentControllers)
            {
                try
                {
                    controller.kill();
                }
                catch (StaleProxyException e)
                {
                    //ignore the exception
                }
            }
        }


        //PlatformController.Listener methods
        @Override
        public void bornAgent(PlatformEvent platformEvent) {
            System.out.println(platformEvent.getAgentGUID() + " born");
        }

        @Override
        public void deadAgent(PlatformEvent platformEvent) {
            System.out.println(platformEvent.getAgentGUID() + " dead");

            this.aliveAgents--;
            if (this.experimentConfigurationRunning == true &&
                    this.aliveAgents == 0)
            {//all agents are dead(completed their run)
                try
                {
                    this.waitingBarrier.await();
                }
                catch (InterruptedException | BrokenBarrierException e) {
                    logger.error("failed waking the experiment thread for another run of algorithm-problem configuration",
                            e);
                }
            }
        }

        @Override
        public void startedPlatform(PlatformEvent platformEvent) {
            System.out.println("startedPlatform");

        }

        @Override
        public void suspendedPlatform(PlatformEvent platformEvent) {
            System.out.println("suspendedPlatform");
        }

        @Override
        public void resumedPlatform(PlatformEvent platformEvent) {
            System.out.println("resumedPlatform");
        }

        @Override
        public void killedPlatform(PlatformEvent platformEvent) {
            System.out.println("killedPlatform");
        }
    }

    public boolean experimentCompleted()
    {
        return this.experimentCompleted;
    }


}