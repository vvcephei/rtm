package Types;

import org.apache.commons.collections.keyvalue.MultiKey;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Created by IntelliJ IDEA.
 * User: jroesler
 * Date: 7/10/11
 * Time: 8:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class Topic extends MultiKey {
    public Topic(String name, Iterable<WeightedWord> collection) {
        super(name, collection);
    }

    public String getName() {
        return (String) super.getKey(0);
    }

    public Iterable<WeightedWord> getCollection() {
        return (Iterable<WeightedWord>) super.getKey(1);
    }

    public void dumpToCSV(File file)
            throws IOException {
        Writer fileWriter = new BufferedWriter(new FileWriter(file));
        for (WeightedWord wword : this.getCollection()) {
            fileWriter.write(wword.getWord() + "\t" + wword.getWeight() + "\n");
        }
        fileWriter.close();
    }

}
