import java.util.AbstractMap;
import java.util.ArrayList;

/**
 * Created by MiHu on 17.11.2016.
 */
public class MLP {

    public static double sigmoidSteepness;


    private Neuron outputNeuron;
    private ArrayList<Neuron> hiddenLayer = new ArrayList<>();
    private ArrayList<Neuron> inputLayer = new ArrayList<>();

    public MLP(int numInputNeurons, int numHiddenNeurons, double sigmoidSteepness) {
        MLP.sigmoidSteepness = sigmoidSteepness;

        for (int i = 0; i < numInputNeurons; i++) {
            inputLayer.add(new Neuron(0));
        }

        for (int i = 0; i < numHiddenNeurons; i++) {
            hiddenLayer.add(new Neuron(inputLayer));
        }
        outputNeuron = new Neuron(hiddenLayer);
    }

    public void trainWeigthts(ArrayList<AbstractMap.SimpleImmutableEntry<ArrayList<Double>, Double>> trainingSet) {

        for (Connection connection : outputNeuron.connections) {

        }

        for (AbstractMap.SimpleImmutableEntry<ArrayList<Double>, Double> sample : trainingSet) {


        }
    }

    public double getMovieRating(ArrayList<Double> inputs) {
        if (inputLayer.size() != inputs.size()) {
            throw new IllegalArgumentException("Input is not of same length as there are input neurons");
        }
        for (int i = 0; i < inputs.size(); i++) {
            inputLayer.get(i).value = inputs.get(i);
        }
        for (Neuron neuron : hiddenLayer) {
            neuron.evaluate();
        }
        outputNeuron.evaluate();
        return outputNeuron.value;
    }

    private double learningRate(int learningStep) {
        return 0.5;
    }
}
