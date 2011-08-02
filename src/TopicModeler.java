/**
 * Created by IntelliJ IDEA.
 * User: jroesler
 * Date: 7/5/11
 * Time: 7:44 AM
 * To change this template use File | Settings | File Templates.
 */

import Types.Topic;
import Types.WeightedWord;
import cc.mallet.util.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

import cc.mallet.types.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.topics.*;

import com.ibm.wordcloud.Main;
import org.apache.commons.collections.keyvalue.MultiKey;


public class TopicModeler {

    private static String _sourceTextPath = "/data/reviews/bb_eq/train/";
    private static String _outputDir = "/home/jroesler/exampleWordles/";
    private static boolean _outputDocSummaryToFile = true;
    private static int _numTopics = 10;
    private static int _numThreads = 10;
    private static int _numTokens = Integer.MAX_VALUE;
    private static int _numIterations = 500;
    private static double _alphaSum = 100;
    private static double _beta = 0.01;

    private InstanceList _instances;


    public String getSourceTextPath() {
        return _sourceTextPath;
    }

    public void setSourceTextPath(String sourceTextPath) {
        _sourceTextPath = sourceTextPath;
    }

    public TopicModeler() {
    }

    public TopicModeler(String sourceTextPath, String wordCloudJarPath, String wordCloudConfigurationPath, Integer width, Integer height, String dest, boolean clearDest) {
    }


    private TopicModel modelTopics(int modelType)
            throws IOException, UnsupportedEncodingException {


        TopicModel model;
        switch (modelType) {
            case 0: //PAM
//                model = (TopicModel) new PAM4L(_numTopics / 2, _numTopics, _alphaSum, _beta);
//                model.estimate(_instances, _numIterations, 50, 50, 0, null, new Randoms());
//                model.estimate();
                throw new RuntimeException();
//                break;
            default: //LDA
                model = new ParallelTopicModel();
                Map params = new HashMap();
                params.put(ParallelTopicModel.NUM_TOPICS, _numTopics);
                params.put(ParallelTopicModel.NUM_THREADS, _numThreads);
                params.put(ParallelTopicModel.NUM_ITERATIONS, _numIterations);
                params.put(ParallelTopicModel.ALPHA_SUM, _alphaSum);
                params.put(ParallelTopicModel.BETA, _beta);
                model.setParams(params);
                model.addInstances(_instances);
                model.estimate();
                break;
        }

        return model;
    }

    public List<Topic> getTopics(TopicModel model) {

        Alphabet dataAlphabet = _instances.getDataAlphabet();

        // Get an array of sorted sets of word ID/count pairs
        ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();

        // Show top 5 words in topics with proportions for the first document

        List<Topic> itTopics = new ArrayList<Topic>();
        for (int topic = 0; topic < _numTopics; topic++) {
            List<WeightedWord> itTopic = new ArrayList<WeightedWord>();

            Iterator iterator = topicSortedWords.get(topic).iterator();
            int rank = 0;
            while (iterator.hasNext() && rank < _numTokens) {
                IDSorter idCountPair = (IDSorter) iterator.next();
                itTopic.add(new WeightedWord(dataAlphabet.lookupObject(idCountPair.getID()).toString(), idCountPair.getWeight()));
                rank++;
            }
            itTopics.add(new Topic("Topic_" + topic, itTopic));
        }
        return itTopics;
    }

    private void setInstances() {
        List pipeList = new ArrayList();
        pipeList.add(new CharSequenceLowercase());
        pipeList.add(new CharSequence2TokenSequence());
        pipeList.add(new TokenSequenceRemoveStopwords());
        pipeList.add(new TokenSequence2FeatureSequence());


        _instances = new InstanceList(new SerialPipes(pipeList));

    }

    public void readDir(String dirName) {
        ArrayList<Pipe> pipeList = new ArrayList<Pipe>();
        pipeList.add(new Target2Label());
        pipeList.add(new SaveDataInSource());
        pipeList.add(new Input2CharSequence(Charset.defaultCharset().displayName()));
        pipeList.add(new CharSequenceLowercase());
        pipeList.add(new CharSequence2TokenSequence());
        pipeList.add(new TokenSequenceRemoveStopwords());
        pipeList.add(new TokenSequence2FeatureSequence());
/*


        Pattern tokenPattern = null;
        tokenPattern = CharSequenceLexer.LEX_NONWHITESPACE_CLASSES;
        pipeList.add(new CharSequence2TokenSequence(tokenPattern));
        pipeList.add(new TokenSequenceLowercase());

        TokenSequenceRemoveStopwords stopwordFilter =
            new TokenSequenceRemoveStopwords(false, false);
        pipeList.add(stopwordFilter);
        pipeList.add( new TokenSequenceNGrams(new int[]{1}) );
        pipeList.add( new TokenSequence2FeatureSequence() );
*/
//        pipeList.add( new FeatureSequence2AugmentableFeatureVector(false) );
        _instances = new InstanceList(new SerialPipes(pipeList));


        boolean removeCommonPrefix = true;
        _instances.addThruPipe(new FileIterator(new File[] {new File(dirName)},
                FileIterator.STARTING_DIRECTORIES, removeCommonPrefix));
    }

    public void readSingleFile(String fileName)
            throws FileNotFoundException, UnsupportedEncodingException {
        setInstances();
        Reader fileReader = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
        Pattern lineRegex = Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$");
        _instances.addThruPipe(new CsvIterator(fileReader, lineRegex, 3, 2, 1));
    }

    public static void main(String[] args)
            throws IOException, UnsupportedEncodingException, InterruptedException {
        int exitCode = 0;
        TopicModeler tm = new TopicModeler(); //TODO Parse args
        Wordleizer wr = new Wordleizer();
        wr.setDest(_outputDir);

        File sourcePath = new File(_sourceTextPath);
        if (sourcePath.isDirectory()) {
            tm.readDir(_sourceTextPath);
        } else {
            tm.readSingleFile(_sourceTextPath);
        }

        TopicModel topics = tm.modelTopics(1);//LDA
        exitCode = wr.makeWordles(tm.getTopics(topics));

        PrintStream docSummary;
        String separator;
        if (_outputDocSummaryToFile) {
            File docSummaryFile = new File(_outputDir, "docSummary.csv");
            docSummary = new PrintStream(new FileOutputStream(docSummaryFile));
            separator = ",";
            System.err.println("Printing document summary to "+docSummaryFile.getAbsolutePath());
        } else {
            docSummary = System.out;
            separator = " ";
        }
        for (TopicModelResult doc : topics.getDocumentTopics()) {
            docSummary.println(doc.toString(0.0, _numTopics, separator));
        }
        if (_outputDocSummaryToFile){
            docSummary.close();
        }
        System.exit(exitCode);
    }

}
