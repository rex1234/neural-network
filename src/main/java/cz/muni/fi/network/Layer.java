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
    public double potentials[];
    public double weights[][];
    public double errorDsRespectW[][];
    public double previousDeltaWs[][];
    public double errorDsRespectY[];
    public double inputs[];
    private double twoThirds = 2d / 3d;
    private double c1 = twoThirds * 1.7159d;
    private double c2 = twoThirds / 1.7159d;


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
        potentials = new double[numNeurons];
        weights = new double[numNeurons][numInputs + 1];
        errorDsRespectW = new double[numNeurons][numInputs + 1];
        previousDeltaWs = new double[numNeurons][numInputs + 1];
    }

    public void initWeights(double min, double max) {
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                double random;
                do {
                    random = r.nextDouble();
                } while (Math.abs(min + (max - min) * random) < 0.008);
                weights[i][j] = min + (max - min) * random;
            }
        }
    }

    public void resetErrorDsRespectW() {
        for (int i = 0; i < errorDsRespectW.length; i++) {
            for (int j = 0; j < errorDsRespectW[i].length; j++) {
                errorDsRespectW[i][j] = 0.0;
            }
        }
    }

    public double[] evaluate() {
        if (isHidden) {
            for (int i = 0; i < weights.length; i++) {
                double innerPotential = 0;
                for (int j = 0; j < inputs.length; j++) {
                    innerPotential += weights[i][j] * inputs[j];
                }
                innerPotential += weights[i][inputs.length];
//                potentials[i] = innerPotential; // ---------------------------  Len pre výpisy
                outputs[i] = tanh(innerPotential);
            }
        } else {
            for (int i = 0; i < weights.length; i++) {
                double innerPotential = 0;
                for (int j = 0; j < inputs.length; j++) {
                    innerPotential += weights[i][j] * inputs[j];
                }
//                potentials[i] = innerPotential; // ---------------------------  Len pre výpisy
                outputs[i] = tanh(innerPotential);
            }
        }
        return outputs;
    }

    public void updateWeights(double learningRate) {
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[i].length; j++) {
                double deltaWeight = (-learningRate * errorDsRespectW[i][j]) + (mlp.momentumInfluence * previousDeltaWs[i][j]);
                weights[i][j] += deltaWeight;
                previousDeltaWs[i][j] = deltaWeight;
            }
        }
    }

    public double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    public double dSigmoid(double y) {
        return y * (1 - y);
    }

    public double tanh(double x) {
        return 1.7159 * Math.tanh(twoThirds * x);
    }

    public double dTanh(double y) {
        return c1 - (c2 * Math.pow(y, 2));
    }

}
