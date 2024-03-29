import Types.Topic;
import Types.WeightedWord;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

import cc.mallet.types.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.topics.*;


public class TopicModeler {

    private static String _sourceTextPath = "/data/tweets/1/train/";
    private static String _outputDir = "/home/john/repos/ReviewTopicModeler/output/";
    private static boolean _outputDocSummaryToFile = true;
    private static int _numTopics = 4;
    private static int _numThreads = 10;
    private static int _numTokens = Integer.MAX_VALUE;
    private static int _numIterations = 1000;
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


        Map params;
        TopicModel model;
        switch (modelType) {
            case 0: //PAM
//                model = (TopicModel) new PAM4L(_numTopics / 2, _numTopics, _alphaSum, _beta);
//                model.estimate(_instances, _numIterations, 50, 50, 0, null, new Randoms());
//                model.estimate();
//                break;
                throw new RuntimeException();
            case 1: //topical n_grams //FIXME broken
                model = new TopicalNGrams(_numTopics);
                params = new HashMap();
                params.put(TopicalNGrams.NUM_ITERATIONS, _numIterations);
                params.put(TopicalNGrams.SHOW_TOPICS_INTERVAL, 100);
                params.put(TopicalNGrams.OUTPUT_MODEL_INTERVAL, 0);
                params.put(TopicalNGrams.OUTPUT_MODEL_FILENAME, "");
                model.setParams(params);
                model.addInstances(_instances);
                model.estimate();
                break;
            default: //LDA
                model = new ParallelTopicModel();
                params = new HashMap();
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
        /*
        ParallelTopicModel mymodel = (ParallelTopicModel) model;
        for (int i = 0; i< 5; i++){
            System.out.println(mymodel.getData().get(i).instance.getSource());
            System.out.println(Arrays.toString(mymodel.getTopicProbabilities(i)));
        }
        System.out.println("Total:");
        System.out.println(Arrays.toString(mymodel.getTopicProbabilities()));*/
        /*InstanceList test1 = readDir("/data/reviews/0/test/", _instances.getPipe());
        for (Instance instance : test1){
            System.out.println(instance.getSource());
            System.out.println(Arrays.toString(mymodel.getInferencer().getSampledDistribution(instance, _numIterations, 1, 1)));
        }*/
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

    public InstanceList readDir(String dirName, Pipe pipe){
        InstanceList instanceList = new InstanceList(pipe);

        boolean removeCommonPrefix = true;
        instanceList.addThruPipe(new FileIterator(new File[] {new File(dirName)},
                FileIterator.STARTING_DIRECTORIES, removeCommonPrefix));
        return instanceList;
    }

    public InstanceList readDir(String dirName) {
        ArrayList<Pipe> pipeList = new ArrayList<Pipe>();
        pipeList.add(new Target2Label());
        pipeList.add(new SaveDataInSource());
        pipeList.add(new Input2CharSequence(Charset.defaultCharset().displayName()));
        pipeList.add(new CharSequenceLowercase());
//        pipeList.add(new CharSequence2TokenSequence());
        String url = "(?i)\\b((?:[a-z][\\w-]+:(?:/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))";
        String alpha = "\\p{Alpha}+";
        String ht = "#"+alpha;
        String mention = "@"+alpha;
        pipeList.add(new CharSequence2TokenSequence(Pattern.compile(url+"|"+ht+"|"+mention+"|"+alpha)));
        pipeList.add(new TokenSequenceRemoveStopwords());
        pipeList.add(new TokenSequence2FeatureSequence());
        return readDir(dirName, new SerialPipes(pipeList));
    }

    public InstanceList readSingleFile(String fileName)
            throws FileNotFoundException, UnsupportedEncodingException {
        List pipeList = new ArrayList();
        pipeList.add(new CharSequenceLowercase());
        pipeList.add(new CharSequence2TokenSequence());
        pipeList.add(new TokenSequenceRemoveStopwords());
        pipeList.add(new TokenSequence2FeatureSequence());

        InstanceList instanceList = new InstanceList(new SerialPipes(pipeList));

        Reader fileReader = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
        Pattern lineRegex = Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$");
        instanceList.addThruPipe(new CsvIterator(fileReader, lineRegex, 3, 2, 1));
        return instanceList;
    }

    public static void main(String[] args)
            throws IOException, InterruptedException {
        int exitCode = 0;
        TopicModeler tm = new TopicModeler(); //TODO Parse args
        Wordleizer wr = new Wordleizer();
        wr.setDest(_outputDir);

        File sourcePath = new File(_sourceTextPath);
        if (sourcePath.isDirectory()) {
            tm._instances = tm.readDir(_sourceTextPath);
        } else {
            tm._instances = tm.readSingleFile(_sourceTextPath);
        }

        TopicModel topics = tm.modelTopics(2);
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
