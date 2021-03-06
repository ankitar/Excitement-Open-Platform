package eu.excitementproject.eop.biutee.rteflow.systems.excitement;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;

import eu.excitementproject.eop.biutee.classifiers.ClassifierException;
import eu.excitementproject.eop.biutee.plugin.PluginAdministrationException;
import eu.excitementproject.eop.biutee.rteflow.systems.rtepairs.PairData;
import eu.excitementproject.eop.biutee.rteflow.systems.rtepairs.PairResult;
import eu.excitementproject.eop.biutee.rteflow.systems.rtepairs.RTEPairsETETrainer;
import eu.excitementproject.eop.biutee.script.ScriptException;
import eu.excitementproject.eop.biutee.utilities.BiuteeException;
import eu.excitementproject.eop.biutee.utilities.SystemInformationLog;
import eu.excitementproject.eop.common.EDABasic;
import eu.excitementproject.eop.common.EDAException;
import eu.excitementproject.eop.common.TEDecision;
import eu.excitementproject.eop.common.configuration.CommonConfig;
import eu.excitementproject.eop.common.exception.ComponentException;
import eu.excitementproject.eop.common.exception.ConfigurationException;
import eu.excitementproject.eop.common.representation.coreference.TreeCoreferenceInformationException;
import eu.excitementproject.eop.common.utilities.configuration.ConfigurationFileDuplicateKeyException;
import eu.excitementproject.eop.lap.biu.lemmatizer.LemmatizerException;
import eu.excitementproject.eop.transformations.generic.truthteller.AnnotatorException;
import eu.excitementproject.eop.transformations.operations.OperationException;
import eu.excitementproject.eop.transformations.operations.rules.RuleBaseException;
import eu.excitementproject.eop.transformations.utilities.TeEngineMlException;

/**
 * BIUTEE implementation of {@link EDABasic}.
 * 
 * @author Asher Stern
 * @since Jan 23, 2013
 *
 */
public class BiuteeEDA implements EDABasic<TEDecision>
{
	public static final String TEMPORARY_CONFIGURATION_FILE_PREFIX = "biutee_configuration_file";
	public static final String TEMPORARY_CONFIGURATION_FILE_SUFFIX = ".xml";

	/*
	 * (non-Javadoc)
	 * @see eu.excitementproject.eop.common.EDABasic#initialize(eu.excitementproject.eop.common.configuration.CommonConfig)
	 */
	@Override
	public void initialize(CommonConfig config) throws ConfigurationException, EDAException, ComponentException
	{
		logger.info(BiuteeEDA.class.getSimpleName()+": initialization for interactive textual entailment recognition.");
		if (trainingMode!=null)
		{
			if (false == trainingMode.booleanValue()) throw new EDAException("The method initialize must not run twice for the same EDA object.");
			else throw new EDAException("Method initialize must not be called if startTraining was called.");
		}
		trainingMode = false;
		
		try
		{
			new SystemInformationLog(config.getConfigurationFileName()).log();
			logger.info("Initializing BIUTEE underlying system...");
			underlyingSystem = new BiuteeEdaUnderlyingSystem(config.getConfigurationFileName());
			underlyingSystem.init();
			logger.info("Initializing BIUTEE underlying system - done.");
		}
		catch (Exception e)
		{
			throw new EDAException("Initialization failure. See nested exception.",e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see eu.excitementproject.eop.common.EDABasic#process(org.apache.uima.jcas.JCas)
	 */
	@Override
	public TEDecision process(JCas aCas) throws EDAException, ComponentException
	{
		if (hasBeenShutDown) throw new EDAException("After calling the method shutdown(), no method should be called.");
		if (null == underlyingSystem) throw new EDAException("Method initialize must be called before calling method process.");
		String pairId = null;
		try
		{
			logger.info("Building PairData from the given JCas...");
			pairId = BiuteeEdaUtilities.getPairIdFromJCas(aCas);
			PairData pairData = BiuteeEdaUtilities.convertJCasToPairData(aCas);
			logger.info("Building PairData from the given JCas - done.");
			logger.info(String.format("Processing T-H pair (Pair ID %s)...", pairId));
			PairResult pairResult = underlyingSystem.process(pairData);
			logger.info(String.format("Processing T-H pair (Pair ID %s)- done.", pairId));
			return BiuteeEdaUtilities.createDecisionFromPairResult(pairId,pairResult,underlyingSystem.getClassifierForPredictions());
		}
		catch (TeEngineMlException | AnnotatorException | OperationException | ClassifierException | MalformedURLException | TreeCoreferenceInformationException | ScriptException | RuleBaseException | LemmatizerException | InterruptedException | ExecutionException e)
		{
			String pairIdDetail = "";
			if (pairId == null) {
				pairIdDetail = "(Pair ID unknown)";
			}
			else {
				pairIdDetail = String.format("(Pair ID: %s)", pairId);
			}
			throw new EDAException(String.format("Failed to process given CAS %s. See nested exception.", pairIdDetail),e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see eu.excitementproject.eop.common.EDABasic#shutdown()
	 */
	@Override
	public void shutdown()
	{
		try
		{
			if (underlyingSystem!=null)
			{
				underlyingSystem.cleanUp();
			}
		}
		finally
		{
			hasBeenShutDown = true;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see eu.excitementproject.eop.common.EDABasic#startTraining(eu.excitementproject.eop.common.configuration.CommonConfig)
	 */
	@Override
	public void startTraining(CommonConfig config) throws ConfigurationException, EDAException, ComponentException
	{
		logger.info(BiuteeEDA.class.getSimpleName()+": training.");
		if (trainingMode!=null)
		{
			if (trainingMode.booleanValue()) throw new EDAException("The method startTraining must not run twice for the same EDA object.");
			else throw new EDAException("Method startTraining must not be called if initialize was called.");
		}
		trainingMode = true;
		try
		{
			RTEPairsETETrainerWithInitAndCleanUp trainer = new RTEPairsETETrainerWithInitAndCleanUp(config.getConfigurationFileName());
			logger.info("Training system - initialization...");
			trainer.init();
			try
			{
				logger.info("Training system - initialization - done.");
				logger.info("Training system - training...");
				trainer.train();
				logger.info("Training system - training - done.");
			}
			finally
			{
				trainer.cleanUp();
			}
		}
		catch (BiuteeException | TeEngineMlException | PluginAdministrationException | LemmatizerException | IOException e)
		{
			throw new EDAException("Training failed. See nested exception.",e);
		}
	}

	
	private static class RTEPairsETETrainerWithInitAndCleanUp extends RTEPairsETETrainer
	{
		public RTEPairsETETrainerWithInitAndCleanUp(String configurationFileName){super(configurationFileName);}
		public void init() throws ConfigurationFileDuplicateKeyException, eu.excitementproject.eop.common.utilities.configuration.ConfigurationException, MalformedURLException, LemmatizerException, TeEngineMlException, IOException, PluginAdministrationException {super.init();}
		public void cleanUp(){super.cleanUp();}
	}
	
	private BiuteeEdaUnderlyingSystem underlyingSystem = null;
	private Boolean trainingMode = null;
	private boolean hasBeenShutDown = false;
	
	private static final Logger logger = Logger.getLogger(BiuteeEDA.class);
}
