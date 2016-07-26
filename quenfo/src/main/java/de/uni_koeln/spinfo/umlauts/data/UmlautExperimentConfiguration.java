package de.uni_koeln.spinfo.umlauts.data;

import java.io.File;

import de.uni_koeln.spinfo.classification.core.classifier.AbstractClassifier;
import de.uni_koeln.spinfo.classification.core.data.ExperimentConfiguration;
import de.uni_koeln.spinfo.classification.core.data.FeatureUnitConfiguration;
import de.uni_koeln.spinfo.classification.core.featureEngineering.featureWeighting.AbstractFeatureQuantifier;

public class UmlautExperimentConfiguration extends ExperimentConfiguration{
	
	private boolean storeFullSentences;
	private int contextBefore;
	private int contextAfter;

	public UmlautExperimentConfiguration(FeatureUnitConfiguration fuc,
			AbstractFeatureQuantifier fq, AbstractClassifier classifier,
			File dataFile, String outputFolder, boolean storeFullSentences, int contextBefore, int contextAfter) {
		super(fuc, fq, classifier, dataFile, outputFolder);
		this.storeFullSentences = storeFullSentences;
		this.contextBefore = contextBefore;
		this.contextAfter = contextAfter;
	}

	public boolean getStoreFullSentences() {
		return storeFullSentences;
	}

	public int getContextBefore() {
		return contextBefore;
	}

	public int getContextAfter() {
		return contextAfter;
	}

}
