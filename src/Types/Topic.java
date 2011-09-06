package Types;

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
public class Topic{
    private String _name;
    private Iterable<WeightedWord> _collection;

    public Topic(String name, Iterable<WeightedWord> collection) {
        _name = name;
        _collection = collection;
    }

    public String getName() {
        return _name;
    }

    public Iterable<WeightedWord> getCollection() {
        return _collection;
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
