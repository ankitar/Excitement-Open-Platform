package eu.excitementproject.eop.core.component.alignment.nemex;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.junit.Test;
import org.uimafit.util.JCasUtil;

import eu.excitement.type.alignment.Link;
import eu.excitementproject.eop.common.component.alignment.PairAnnotatorComponentException;
import eu.excitementproject.eop.lap.dkpro.OpenNLPTaggerEN;
import eu.excitementproject.eop.lap.implbase.LAP_ImplBase;

public class NemexAlignerTestEN {

	static Logger logger;

	private NemexAligner aligner;

	@Test
	public void test() {
		try {

			logger = Logger.getLogger(NemexAligner.class.getName());

			// prepare JCas
			
			OpenNLPTaggerEN tokenizer = null;
			tokenizer = new OpenNLPTaggerEN();
			
			JCas aJCas1 = tokenizer.generateSingleTHPairCAS("I saw a car.",
					"I saw an automobile.");

			Logger.getRootLogger().setLevel(Level.INFO); // main log setting:
															// set as DEBUG to
															// see what's going
															// & debug.

			logger.info("Starting alignment for test JCas pair 1");
			alignAndPrint(aJCas1);
			logger.info("Finished alignment of test JCas pair 1");
			
			/*JCas aJCas2 = tokenizer
					.generateSingleTHPairCAS(
							"Judge Drew served as Justice until Kennon returned to claim his seat in 1945.",
							"Kennon served as Justice.");

			logger.info("Starting alignment for test JCas pair 2");
			alignAndPrint(aJCas2);
			logger.info("Finished alignment of test JCas pair 2");


			JCas aJCas3 = tokenizer
					.generateSingleTHPairCAS(
							"Ms. Minton left Australia in 1961 to pursue her studies in London.",
							"Ms. Minton was born in Australia.");
							
			logger.info("Starting alignment for test JCas pair 3");
			alignAndPrint(aJCas3);
			logger.info("Finished alignment of test JCas pair 3");

			
			JCas aJCas4 = tokenizer
					.generateSingleTHPairCAS(
							"Robinson's garden style can be seen today at Gravetye Manor, West Sussex, England, though it is more manicured than it was in Robinson's time.",
							"Gravetye Manor is located in West Sussex.");
			
			logger.info("Starting alignment for test JCas pair 4");
			alignAndPrint(aJCas4);
			logger.info("Finished alignment of test JCas pair 4");*/

		} catch (Exception e) {
			logger.info("Could not align the JCas test pair");
		}
	}

	private void alignAndPrint(JCas aJCas)
			throws PairAnnotatorComponentException {
		try {

			logger.info("Initialize the Nemex Aligner");

			aligner = new NemexAligner(
					"src/test/resources/gazetteer/nemexAligner.txt", "#", true,
					3, false, "DICE_SIMILARITY_MEASURE", 0.8);
			logger.info("Initialization finished");

			// align test JCas pair

			aligner.annotate(aJCas);

			// Print the alignment of JCas pair

			JCas hypoView = aJCas.getView(LAP_ImplBase.HYPOTHESISVIEW);

			for (Link link : JCasUtil.select(hypoView, Link.class)) {

				logger.info(String.format("Text phrase: %s, "
						+ "hypothesis phrase: %s, "
						+ "id: %s, confidence: %f, direction: %s", link
						.getTSideTarget().getCoveredText(), link
						.getHSideTarget().getCoveredText(), link.getID(), link
						.getStrength(), link.getDirection().toString()));

			}
		} catch (Exception e) {
			logger.info("Alignment failed");
			e.printStackTrace();

		}
	}
}
