package com.aic;

import com.aic.sentiment_analysis.classification.*;
import com.aic.sentiment_analysis.preprocessing.ISentimentPreprocessor;
import com.aic.sentiment_analysis.preprocessing.PreprocessingException;
import com.aic.sentiment_analysis.preprocessing.SentimentTwitterPreprocessor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.functions.SMO;
import weka.classifiers.lazy.IBk;
import weka.core.SelectedTag;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

@Configuration
@EnableCaching
public class ApplicationConfig {

	@Bean
	public List<TrainingSample> trainingSamples() throws URISyntaxException,
			PreprocessingException, FileNotFoundException {
		//ICSVTrainingLoader sampleLoader = new CSVTrainingSampleLoader();
		ICSVTrainingLoader sampleLoader = new CSVTrainingSTSLoader();
		URI trainingSetUri = getClass().getClassLoader().
				getResource(Constants.CLASSIFIER_TRAINING_FILE_PATH).toURI();
		List<TrainingSample> samples = sampleLoader.load(trainingSetUri);
		return samples;
	}

	@Bean
	public twitter4j.conf.Configuration twitterConfiguration() throws IOException {
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true);
		Properties props = new Properties();
		props.load(getClass().getClassLoader().getResourceAsStream("twitter.properties"));
		if (props.containsKey("consumerKey")
				&& props.containsKey("consumerSecret")
				&& props.containsKey("accessToken")
				&& props.containsKey("accessTokenSecret")) {
			cb.setOAuthConsumerKey(props.getProperty("consumerKey"));
			cb.setOAuthConsumerSecret(props.getProperty("consumerSecret"));
			cb.setOAuthAccessToken(props.getProperty("accessToken"));
			cb.setOAuthAccessTokenSecret(props.getProperty("accessTokenSecret"));
			return cb.build();
		} else {
			throw new IOException("Credentials incomplete!");
		}
	}

    @Bean
    public ISentimentPreprocessor sentimentPreprocessor(){
        return new SentimentTwitterPreprocessor();
    }

	@Bean
	public CacheManager cacheManager() {
		return new ConcurrentMapCacheManager("sentiments", "aggregateSentiment");
	}

	@Bean
	public ISentimentClassifier svm_C_SVC() throws FileNotFoundException, PreprocessingException, URISyntaxException, ClassificationException {
		return new SentimentClassifier(trainingSamples(), new LibSVM());
	}

	@Bean
	public ISentimentClassifier svm_NU_SVC() throws FileNotFoundException, PreprocessingException, URISyntaxException, ClassificationException {
		LibSVM svm = new LibSVM();
		svm.setSVMType(new SelectedTag(LibSVM.SVMTYPE_NU_SVC, LibSVM.TAGS_SVMTYPE));
		return new SentimentClassifier(trainingSamples(), svm);
	}

	@Bean
	public ISentimentClassifier smo() throws FileNotFoundException, PreprocessingException, URISyntaxException, ClassificationException {
		return new SentimentClassifier(trainingSamples(), new SMO());
	}

	@Bean
	public ISentimentClassifier naiveBayes() throws FileNotFoundException, PreprocessingException, URISyntaxException, ClassificationException {
		return new SentimentClassifier(trainingSamples(), new NaiveBayes());
	}

	@Bean
	public ISentimentClassifier bayesNet() throws FileNotFoundException, PreprocessingException, URISyntaxException, ClassificationException {
		return new SentimentClassifier(trainingSamples(), new BayesNet());
	}

	@Bean
	public ISentimentClassifier kNN() throws FileNotFoundException, PreprocessingException, URISyntaxException, ClassificationException {
		return new SentimentClassifier(trainingSamples(), new IBk());
	}
}
