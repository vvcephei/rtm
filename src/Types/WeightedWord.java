package Types;

import org.apache.commons.collections.keyvalue.MultiKey;

/**
 * Created by IntelliJ IDEA.
 * User: jroesler
 * Date: 7/10/11
 * Time: 8:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class WeightedWord extends MultiKey {
    public WeightedWord(String word, Double weight) {
        super(word, weight);
    }

    public String getWord() {
        return (String) super.getKey(0);
    }

    public Double getWeight() {
        return (Double) super.getKey(1);
    }
}
