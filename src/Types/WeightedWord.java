package Types;
public class WeightedWord{
    private String _word;
    private Double _weight;

    public WeightedWord(String word, Double weight) {
        _word = word;
        _weight = weight;
    }

    public String getWord() {
        return _word;
    }

    public Double getWeight() {
        return _weight;
    }
}
