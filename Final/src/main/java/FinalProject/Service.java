package FinalProject;

import FinalProject.BL.Agents.SmartHomeAgent;
import FinalProject.BL.Agents.SmartHomeAgentBehaviour;
import FinalProject.BL.DataCollection.AlgorithmProblemResult;
import FinalProject.BL.Experiment;
import FinalProject.BL.ExperimentBuilder;
import FinalProject.BL.Problems.Problem;
import FinalProject.DAL.DataAccessController;
import FinalProject.DAL.DataAccessControllerInterface;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class Service {

    private ExperimentBuilder experimentBuilder;
    private DataAccessControllerInterface dalController;
    public Experiment currExperiment;

    private final static Logger logger = Logger.getLogger(Service.class);

    private static Service instance = null;

    public Service(DataAccessControllerInterface dalController)
    {
        this.experimentBuilder = new ExperimentBuilder(this);
        this.dalController = dalController;
    }

    public void addAlgorithmsToExperiment(List<String> algorithmNames, int iterationNumber)
    {
        //TODO gal
        List<SmartHomeAgentBehaviour> loadedAlgorithms = this.dalController.getAlgorithms(algorithmNames);
        this.experimentBuilder.addAlgorithms(loadedAlgorithms);
        this.experimentBuilder.setNumOfIterations(iterationNumber);
    }

    public void addProblemsToExperiment(List<String> problemNames)
    {
        //TODO gal
        List<Problem> loadedProblems = this.dalController.getProblems(problemNames);
        this.experimentBuilder.addProblems(loadedProblems);
    }

    public void runExperiment()
    {
        //TODO gal
        this.currExperiment = this.experimentBuilder.createExperiment();
        this.currExperiment.runExperiment();
    }

    public void stopExperiment()
    {
        //TODO gal
        this.currExperiment.stopExperiment();
    }

    public List<AlgorithmProblemResult> getExperimentResults()
    {
        List<AlgorithmProblemResult> results = new ArrayList<>();
        if (!this.currExperiment.experimentCompleted())
        {
            //decide what to return
        }
        //TODO gal
        return results;
    }

    public void experimentEnded(List<AlgorithmProblemResult> results)
    {
        //TODO gal
    }

    public void experimentEndedWithError(Exception e)
    {
        //TODO gal
        logger.error("error", e);
    }

    public void saveExperimentResult(List<AlgorithmProblemResult> results)
    {
        //TODO
    }

}