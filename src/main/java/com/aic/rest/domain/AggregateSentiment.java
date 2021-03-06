package com.aic.rest.domain;

import com.aic.sentiment_analysis.classification.Sentiment;
import com.aic.sentiment_analysis.fetch.TwitterStatus;

import java.util.Map;

public class AggregateSentiment {
    private Map<TwitterStatus, Sentiment> sentiments;
    private double sentimentRatio;
    private double dateWeightedSentimentRatio;

    public AggregateSentiment(com.aic.sentiment_analysis.AggregateSentiment aggregateSentiment) {
        this.sentiments = aggregateSentiment.getSentimentAggregation();
        this.sentimentRatio = aggregateSentiment.calculateAggregateSentimentRatio();
        this.dateWeightedSentimentRatio = aggregateSentiment.calculateDateWeightedAggregateSentimentRatio();
    }

    /**
     * Returns a map of tweets with the corresponding sentiment.
     *
     * @return a map of tweets with the corresponding sentiment
     */
    public Map<TwitterStatus, Sentiment> getSentimentAggregation() {
        return sentiments;
    }

    public double getSentimentRatio() {
        return sentimentRatio;
    }

    public double getDateWeightedSentimentRatio() {
        return dateWeightedSentimentRatio;
    }
}
