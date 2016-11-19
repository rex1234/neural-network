package cz.muni.fi.network;

import java.util.Random;

/**
 * Created by MiHu on 19.11.2016.
 */
public class Layer {

    private MLP mlp;

    Random r = new Random();

    private boolean isHidden;
    public double outputs[];
    public double weights[][];
    public double deltaWeights[][];
    public double errorDsRespectY[];
    public double inputs[];

    public Layer(MLP mlp, int numNeurons, int numInputs, boolean isHidden) {
        this.mlp = mlp;
        this.isHidden = isHidden;
        if (isHidden) {
            outputs = new double[numNeurons + 1];
            outputs[numNeurons] = 1.0;
            errorDsRespectY = new double[numNeurons + 1];
        } else {
            outputs = new double[numNeurons];
            errorDsRespectY = new double[numNeurons];
        }
        weights = new double[numNeurons][numInputs + 1];
        deltaWeights = new double[numNeurons][numInputs + 1];
    }

    public void initWeights(double min, double max) {
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                weights[i][j] = min + (max - min) * r.nextDouble();
            }
        }
    }

    public void resetDeltaWeights() {
        for (int i = 0; i < deltaWeights.length; i++) {
            for (int j = 0; j < deltaWeights[i].length; j++) {
                deltaWeights[i][j] = 0.0;
            }
        }
    }

    public double[] evaluate() {
        for (int i = 0; i < weights.length; i++) {
            double innerPotential = 0;
            for (int j = 0; j < inputs.length; j++) {
                innerPotential += weights[i][j] * inputs[j];
            }
            if (isHidden) {
                innerPotential += weights[i][inputs.length]; //TODO optimalizovat if podmienku
            }
            outputs[i] = sigmoid(innerPotential);
        }
        return outputs;
    }

    public void updateWeights(double learningRate) {
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                weights[i][j] += -learningRate * deltaWeights[i][j];
            }
        }
    }

    public double sigmoid(double x) {
        return 1 / (1 + Math.exp(-mlp.sigmoidSteepness * x));
    }

    public double dSigmoid(double y) {
        return mlp.sigmoidSteepness * y * (1 - y);
    }

}
