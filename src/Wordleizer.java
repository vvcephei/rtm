import Types.Topic;
import Types.WeightedWord;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jroesler
 * Date: 7/10/11
 * Time: 8:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class Wordleizer {

    private String _wordCloudJarPath = "/opt/wordcloud/ibm-word-cloud.jar";
    private String _wordCloudConfigurationPath = "/opt/wordcloud/myconf.txt";
    private Integer _width = 800;
    private Integer _height = 600;
    private String _dest = "/home/jroesler/exampleWordles/";
    private boolean _clearDest = true;

    public Wordleizer(){}

    public void setDest(String dest){
        _dest = dest;
    }

    public String makeWordlesCommand(String source, String dest) {
        StringBuilder command = new StringBuilder();
        command.append("java -jar ");
        command.append(_wordCloudJarPath);

        command.append(" -c ");
        command.append(_wordCloudConfigurationPath);

        command.append(" -w ");
        command.append(_width.toString());

        command.append(" -h ");
        command.append(_height.toString());

        command.append(" -i ");
        command.append(source);

        command.append(" -o ");
        command.append(dest);

        return command.toString();
    }

    public int makeWordles(Iterable<Topic> topics)
            throws IOException, InterruptedException {
        File destDirectory = new File(_dest);
        if (_clearDest) {
            destDirectory.delete();
            destDirectory.mkdir();
        }
        if (!destDirectory.isDirectory()) {
            System.err.println("Could not find destination directory: " + _dest);
            return 255;
        }

        List<Process> children = new ArrayList();
        // kick off image generation in parallel
        for (Topic topic : topics) {
            File sourceFile = new File(destDirectory, topic.getName() + ".txt");
            File destFile = new File(destDirectory, topic.getName() + ".png");
            topic.dumpToCSV(sourceFile);

            assert sourceFile.exists() && sourceFile.canRead();
            System.err.println(sourceFile.getAbsolutePath() + " exists.");
            String command = makeWordlesCommand(sourceFile.getAbsolutePath(), destFile.getAbsolutePath());
            System.err.println("Spawning: " + command);
            children.add(Runtime.getRuntime().exec(command));
        }
        int result = 0;
        // wait for all images to be generated before continuing
        for (Process p : children) {
            int e = p.waitFor();

            BufferedReader output = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            int c = output.read();
            if (c == -1) {
                System.err.println("");
            }
            do {
                System.err.write(c);
            } while ((c = output.read()) != -1);

            System.err.println("Process exited with code " + e);

            result += e;

        }
        return result;
    }

    private String wordleize_word(Object word, double weight) {
        StringBuilder res = new StringBuilder();
        res.append(word);
        res.append(":");
        res.append(Double.toString(weight));

        return res.toString();
    }
}
