package cz.muni.fi.network;

import java.util.ArrayList;

/**
 * Created by MiHu on 17.11.2016.
 */
public class MLP {

    public double sigmoidSteepness;
    public double learningRate;

    private Layer outputLayer;
    private Layer hiddenLayer;

    public MLP(int numInputNeurons, int numHiddenNeurons, int numOutputNeurons, double sigmoidSteepness, double learningRate) {

        this.sigmoidSteepness = sigmoidSteepness;
        this.learningRate = learningRate;

        outputLayer = new Layer(this, numOutputNeurons, numHiddenNeurons, false);
        hiddenLayer = new Layer(this, numHiddenNeurons, numInputNeurons, true);
        outputLayer.inputs = hiddenLayer.outputs;
    }

    public void initWeights() {
        hiddenLayer.initWeights(-1, 1);
        outputLayer.initWeights(-1, 1);


//        outputLayer.weights[0][0] = -3;
//        outputLayer.weights[0][1] = 1;
//        outputLayer.weights[0][2] = -1;
//
//        hiddenLayer.weights[0][0] = 0.5;
//        hiddenLayer.weights[0][1] = 0.5;
//        hiddenLayer.weights[0][2] = 0;
//        hiddenLayer.weights[1][0] = 1;
//        hiddenLayer.weights[1][1] = 1;
//        hiddenLayer.weights[1][2] = 0;

    }

    public void training(ArrayList<Sample> samples) {
        initWeights();
        boolean continueTraining = true;
        int learningStep = 0;
        double error = 0; // ------------------------------------------ Len na vypisy
        double deltaWeightsVectorLength = 0; // ------------------------------------------ Len na vypisy
        do {
            if (learningStep % 100 == 0) {  // ------------------------------------------ Len na vypisy
//                System.out.println("Error: " + error);
                System.out.println("Delta weights vector length: " + Math.sqrt(deltaWeightsVectorLength));
            }
            learningStep++;
            resetDeltaWeights();
            error = 0;  // ---------------------------------------------- Len na vypisy
            deltaWeightsVectorLength = 0;// ---------------------------------------------- Len na vypisy
            for (Sample sample : samples) {
                feedForward(sample.inputs);
                for (int i = 0; i < outputLayer.outputs.length; i++) {
                    outputLayer.errorDsRespectY[i] = outputLayer.outputs[i] - sample.desiredOutputs[i];
                    error += 0.5 * Math.pow(outputLayer.errorDsRespectY[i], 2);  // --------------------------- Len na vypisy
                }

                for (int i = 0; i < hiddenLayer.outputs.length; i++) {
                    double errDRespectYi = 0;
                    for (int j = 0; j < outputLayer.outputs.length; j++) {
                        errDRespectYi += outputLayer.errorDsRespectY[j] * outputLayer.weights[j][i] * outputLayer.dSigmoid(outputLayer.outputs[j]);
                    }
                    hiddenLayer.errorDsRespectY[i] = errDRespectYi;
                }

                for (int i = 0; i < outputLayer.weights.length; i++) {
                    for (int j = 0; j < outputLayer.weights[i].length; j++) {
                        outputLayer.deltaWeights[i][j] += outputLayer.errorDsRespectY[i] * hiddenLayer.outputs[j] * outputLayer.dSigmoid(outputLayer.outputs[i]); // TODO optimalizovat, sigmoid sa uz pocital
                        deltaWeightsVectorLength += Math.pow(outputLayer.deltaWeights[i][j], 2);  // --------------------------- Len na vypisy
                    }
                }

                for (int i = 0; i < hiddenLayer.weights.length; i++) {
                    for (int j = 0; j < hiddenLayer.inputs.length; j++) {
                        hiddenLayer.deltaWeights[i][j] += hiddenLayer.errorDsRespectY[i] * hiddenLayer.inputs[j] * hiddenLayer.dSigmoid(hiddenLayer.outputs[i]);
                        deltaWeightsVectorLength += Math.pow(hiddenLayer.deltaWeights[i][j], 2);  // --------------------------- Len na vypisy
                    }
                    hiddenLayer.deltaWeights[i][hiddenLayer.inputs.length] += hiddenLayer.errorDsRespectY[i] * hiddenLayer.dSigmoid(hiddenLayer.outputs[i]);
                    deltaWeightsVectorLength += Math.pow(hiddenLayer.deltaWeights[i][hiddenLayer.inputs.length], 2);  // --------------------------- Len na vypisy
                }
            }
            updateWeights(learningRate);
        } while (learningStep < 100000);
    }

    public double[] feedForward(double[] inputs) {
        hiddenLayer.inputs = inputs;
        hiddenLayer.evaluate();
        return outputLayer.evaluate();
    }


    private void updateWeights(double learningRate) {
        hiddenLayer.updateWeights(learningRate);
        outputLayer.updateWeights(learningRate);
    }

    private void resetDeltaWeights() {
        hiddenLayer.resetDeltaWeights();
        outputLayer.resetDeltaWeights();
    }

    private void printWeights() {
        System.out.println("----------");
        for (int i = 0; i < outputLayer.weights.length; i++) {
            StringBuilder s = new StringBuilder("Output layer weights: ");
            for (int j = 0; j < outputLayer.weights[i].length; j++) {
                s.append(outputLayer.weights[i][j]).append(", ");
            }
            System.out.println(s);
        }

        System.out.println("Hidden layer weights:");
        for (int i = 0; i < hiddenLayer.weights.length; i++) {
            StringBuilder s = new StringBuilder("Neuron [");
            s.append(i).append("]: ");
            for (int j = 0; j < hiddenLayer.weights[i].length; j++) {
                s.append(hiddenLayer.weights[i][j]).append(", ");
            }
            System.out.println(s);
        }
    }
}
