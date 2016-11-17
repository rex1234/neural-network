package cz.muni.fi.network;

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

    public void trainWeights(ArrayList<Sample> trainingSet) {
        boolean continueLearning = false;
        int learningStep = 0;
        do {
            continueLearning = false;
            // UPPER WEIGHTS
            for (Connection connection : outputNeuron.connections) {
                double errorSum = 0;
                for (Sample sample : trainingSet) {
                    double errorDerRespValue = getEstimate(sample.inputs) - sample.desiredOutput;
                    errorSum += errorDerRespValue * outputNeuron.sigmaPrime() * connection.incomingNeuron.value;
                }
                if (Math.abs(errorSum) > 0.00_000_1){
                    continueLearning = true;
                }
                connection.deltaWeight = -learningRate(learningStep) * errorSum;
            }
            // LOWER WEIGHTS
            for (Neuron neuron : hiddenLayer) {
                for (Connection connection : neuron.connections) {

                    double errorSum = 0;
                    for (Sample sample : trainingSet) {
                        double errorDerOutputNeuronRespValue = getEstimate(sample.inputs) - sample.desiredOutput;
                        double errorDerRespValue = errorDerOutputNeuronRespValue * outputNeuron.sigmaPrime() * outputNeuron.connectionsMap.get(neuron).weight;
                        errorSum += errorDerOutputNeuronRespValue * neuron.sigmaPrime() * connection.incomingNeuron.value;
                    }
                    if (Math.abs(errorSum) > 0.00_000_1){
                        continueLearning = true;
                    }
                    connection.deltaWeight = -learningRate(learningStep) * errorSum;
                }
            }
            updateWeights();
            learningStep++;
        } while (continueLearning);
    }

    public void updateWeights() {
        for (Neuron neuron : hiddenLayer) {
            neuron.updateWeights();
        }
        outputNeuron.updateWeights();
    }

    public double getEstimate(ArrayList<Double> inputs) {
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
