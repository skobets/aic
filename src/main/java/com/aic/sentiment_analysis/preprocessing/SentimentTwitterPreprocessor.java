package com.aic.sentiment_analysis.preprocessing;

import com.aic.sentiment_analysis.feature.Feature;
import com.aic.sentiment_analysis.feature.FeatureVector;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of {@link com.aic.sentiment_analysis.preprocessing.ISentimentPreprocessor} based
 * on the Stanford NLP framework.
 *
 * @see <a href="http://nlp.stanford.edu/software/index.shtml">Stanford NLP</a>
 * @see <a href="https://gate.ac.uk/wiki/twitter-postagger.html">POS Tagger Model file</a>
 */
public class SentimentTwitterPreprocessor implements ISentimentPreprocessor {
    static final Logger logger = Logger.getLogger(SentimentTwitterPreprocessor.class.getName());

    private static final String URL_PATTERN = "((https?|ftp|gopher|telnet|file|Unsure|http):" +
            "((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
    private static final String HASHTAG_PATTERN = "#";
    private static final String TAGGER_PATH = "gate-EN-twitter-fast.model";

    private PTBTokenizer.PTBTokenizerFactory<CoreLabel> tokenizer;
    private MaxentTagger tagger;

    public SentimentTwitterPreprocessor() {
        tagger = new MaxentTagger(TAGGER_PATH);
        tokenizer = PTBTokenizer.PTBTokenizerFactory.newPTBTokenizerFactory(false, false);
    }

    @Override
    public FeatureVector preprocess(String message) throws PreprocessingException {

        List<Feature> features = new ArrayList<>();
        List<CoreLabel> coreLabels = tokenizer.getTokenizer(new StringReader(message)).tokenize();

        // tag labels
        tagger.tagCoreLabels(coreLabels);

        for (CoreLabel label : coreLabels) {
            String word = normalizeWord(label.word(), label.tag());
            if (!word.isEmpty() && containsAllowedTag(label.tag())) {
                Feature feature = new Feature();
                feature.setWord(word);
                feature.setTag(label.tag());
                features.add(feature);
            }
        }
        FeatureVector featureVector = new FeatureVector();
        featureVector.setFeatures(features);
        featureVector.setRawMessage(message);

        return featureVector;
    }

    /**
     * Boolean function for filtering unnecessary tags
     * Allow only tags of type verb (VB), adjective (JJ)
     * or hashtag (HT).
     *
     * @param tag the tag that needs to get checked
     * @return boolean value
     * @see <a href="http://www.comp.leeds.ac.uk/ccalas/tagsets/upenn.html">Tagset Overview</a>
     */
    private boolean containsAllowedTag(String tag) {
        if (tag.startsWith("VB") || tag.startsWith("HT") || tag.startsWith("JJ") ||
                tag.startsWith("VBD") || tag.startsWith("VBG") || tag.startsWith("VBN") ||
                tag.startsWith("VBP") || tag.startsWith("VBZ")) {
            return true;
        }

        return false;
    }

    private String normalizeWord(String word, String tag) {
        // lowercase word
        word = word.toLowerCase();

        // remove URL
        word = removeUrl(word);

        // remove hashtag character
        if (tag.equals("HT")) {
            word = removeHashtagCharacter(word);
        }

        return word;
    }

    private String removeHashtagCharacter(String word) {
        return word.replaceAll(HASHTAG_PATTERN,"");
    }

    private String removeUrl(String word) {
        Pattern p = Pattern.compile(URL_PATTERN, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(word);
        int i = 0;

        while (m.find()) {
            word = word.replaceAll(m.group(i), "").trim();
            i++;
        }

        return word;
    }
}
