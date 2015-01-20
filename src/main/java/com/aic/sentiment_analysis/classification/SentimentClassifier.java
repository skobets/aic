package com.aic.sentiment_analysis.classification;

import com.aic.sentiment_analysis.feature.Feature;
import com.aic.sentiment_analysis.feature.FeatureVector;
import com.aic.sentiment_analysis.preprocessing.ISentimentPreprocessor;
import com.aic.sentiment_analysis.preprocessing.PreprocessingException;
import com.aic.sentiment_analysis.preprocessing.SentimentTwitterPreprocessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LibSVM;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

import java.util.*;

/**
 * Implementation of {@link com.aic.sentiment_analysis.classification.ISentimentClassifier} based
 * on the WEKA classification framework.
 *
 * @see <a href="http://www.cs.waikato.ac.nz/~ml/weka/">Official WEKA Website</a>
 * @see <a href="http://weka.sourceforge.net/doc.dev/">WEKA API Reference (Javadoc)</a>
 */
public class SentimentClassifier implements ISentimentClassifier {

	private static final Log logger = LogFactory.getLog(SentimentClassifier.class);

	private final Classifier classifier;
	private final Instances trainingInstances;
	private final ArrayList<Attribute> featureList;
	private final Map<String, Integer> featureIndexMap;

	/**
	 * @param trainingSamples the data that should be used for training the classifier
	 * @param algorithm the concrete classification algorithm to use
	 * @throws ClassificationException
	 */
	public SentimentClassifier(Iterable<TrainingSample> trainingSamples, Classifier algorithm)
			throws ClassificationException {
		classifier = new LibSVM();
		featureList = loadFeatureList(trainingSamples);
		featureIndexMap = initFeatureIndexMap(featureList);
		trainingInstances = loadInstances("train", trainingSamples, featureList);

		// debug output
		StringBuilder featureString = new StringBuilder();
		for (Attribute feature : featureList) {
			featureString.append('{');
			featureString.append(feature);
			featureString.append("}, ");
		}
		featureString.delete(featureString.length() - 2, featureString.length());
		logger.debug("The following features are used for classification: [" + featureString + "]");

		train();
	}

	private Map<String, Integer> initFeatureIndexMap(ArrayList<Attribute> featureList) {
		Map<String, Integer> featureIndexMap = new HashMap<>();
		for (int i = 0; i < featureList.size() - 1; i++) {
			Attribute feature = featureList.get(i);
			featureIndexMap.put(feature.name(), i);
		}
		return featureIndexMap;
	}

	private ArrayList<Attribute> loadFeatureList(Iterable<TrainingSample>trainingSamples)
			throws ClassificationException {
		try {
			Set<String> featureStrings = loadDistinctFeatures(trainingSamples);
			ArrayList<Attribute> featureList = new ArrayList<>();
			for (String feature : featureStrings) {
				featureList.add(new Attribute(feature));
			}

			List<String> sentiments = new ArrayList<>();
			sentiments.add("negative");
			sentiments.add("positive");
			Attribute sentimentAttribute = new Attribute("sentiment", sentiments);
			featureList.add(sentimentAttribute);

			return featureList;
		} catch (PreprocessingException e) {
			throw new ClassificationException(e);
		}
	}

	private Set<String> loadDistinctFeatures(Iterable<TrainingSample> trainingSamples)
			throws PreprocessingException {
		HashSet<String> features = new HashSet<>();
		for (TrainingSample trainingSample : trainingSamples) {
			FeatureVector featureVector = trainingSample.getFeatureVector();
			for (Feature feature : featureVector.getFeatures()) {
				logger.debug("Add feature for word '" + feature.getWord() + "'");
				features.add(feature.getWord());
			}
		}
		return features;
	}

	private void train() throws ClassificationException {
		try {
			classifier.buildClassifier(trainingInstances);
		} catch (Exception e) {
			throw new ClassificationException(e);
		}
	}

	private Instances loadInstances(String name,
	                                Iterable<TrainingSample> trainingSamples,
	                                ArrayList<Attribute> featureList) {
		Instances instances = new Instances(name, featureList, 0);
		instances.setClassIndex(featureList.size() - 1);

		for (TrainingSample trainingSample : trainingSamples) {
			FeatureVector featureVector = trainingSample.getFeatureVector();
			Sentiment sentiment = trainingSample.getSentiment();
			Instance instance = loadInstance(featureVector, sentiment);
			instances.add(instance);
			instance.setDataset(instances);
		}

		return instances;
	}

	private Instance loadInstance(FeatureVector featureVector, Sentiment sentiment) {
		logger.debug("loadInstance");

		TreeMap<Integer, Double> featureMap = new TreeMap<>();
		for (Feature feature : featureVector.getFeatures()) {
			if (isUsedAsFeature(feature.getWord())) {
				featureMap.put(featureIndexMap.get(feature.getWord()), 1.0);
				logger.debug("Use feature '" + feature.getWord() + "' for classification");
			} else {
				logger.debug("Discard feature '" + feature.getWord() + "'");
			}
		}

		int indices[] = new int[featureMap.size() + 1];
		double values[] = new double[featureMap.size() + 1];
		int i = 0;
		for (Map.Entry<Integer, Double> entry : featureMap.entrySet()) {
			indices[i] = entry.getKey();
			values[i] = entry.getValue();
			i++;
		}

		if (sentiment != null) {
			indices[i] = featureList.size() - 1;
			values[i] = intFromSentiment(sentiment);
		}

		Instance instance = new SparseInstance(1.0, values, indices,
				featureList.size() - 1);
		return instance;
	}

	private boolean isUsedAsFeature(String token) {
		return featureIndexMap.get(token) != null;
	}

	private int intFromSentiment(Sentiment sentiment) {
		if (sentiment == Sentiment.NEGATIVE) {
			return 0;
		} else {
			return 1;
		}
	}

	@Override
	public Sentiment classify(FeatureVector featureVector) throws ClassificationException {
		Instances instances = new Instances("live", featureList, 0);
		instances.setClassIndex(featureList.size() - 1);
		Instance instanceToClassify = loadInstance(featureVector);
		instances.add(instanceToClassify);
		instanceToClassify.setDataset(instances);
		try {
			double classification = classifier.classifyInstance(instanceToClassify);
			return sentimentFromClassification(classification);
		} catch (Exception e) {
			throw new ClassificationException(e);
		}
	}

	private Instance loadInstance(FeatureVector featureVector) {
		return loadInstance(featureVector, null);
	}

	private Sentiment sentimentFromClassification(double classification) {
		if (classification == 0) {
			return Sentiment.NEGATIVE;
		} else {
			return Sentiment.POSITIVE;
		}
	}

	public Evaluation evaluate(Iterable<TrainingSample> testSamples)
			throws ClassificationException {
		Instances testInstances = loadInstances("test", testSamples, featureList);
		try {
			Evaluation evaluate = new Evaluation(trainingInstances);
			evaluate.evaluateModel(classifier, testInstances);
			return evaluate;
		} catch (Exception e) {
			throw new ClassificationException(e);
		}
	}
}
