package com.aic.classification;

import edu.stanford.nlp.ling.TaggedWord;

import java.util.List;

/**
 * Classifier for performing sentiment analysis.
 */
public interface ISentimentClassifier {

	/**
	 * Assign a sentiment to the piece of text represented by the given feature
	 * vector.
	 *
	 * @param featureVector the feature vector used for classification
	 * @return the assigned sentiment
	 */
	public Sentiment classify(List<? extends TaggedWord> featureVector) throws ClassificationException;
}
