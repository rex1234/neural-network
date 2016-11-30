package cz.muni.fi.network;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MiHu on 17.11.2016.
 */
public class MLP {

    public double sigmoidSteepness;
    public double learningRate;

    private double weightInitMin;
    private double weightInitMax;

    private Layer outputLayer;
    private Layer hiddenLayer;

    private int printStatusFreq;

    public MLP(int numInputNeurons, int numHiddenNeurons, int numOutputNeurons, double sigmoidSteepness,
               double learningRate, double weightInitMin, double weightInitMax, int printStatusFreq) {

        this.weightInitMax = weightInitMax;
        this.weightInitMin = weightInitMin;
        this.sigmoidSteepness = sigmoidSteepness;
        this.learningRate = learningRate;
        this.printStatusFreq = printStatusFreq;

        outputLayer = new Layer(this, numOutputNeurons, numHiddenNeurons, false);
        hiddenLayer = new Layer(this, numHiddenNeurons, numInputNeurons, true);
        outputLayer.inputs = hiddenLayer.outputs;
    }

    private void initWeights() {
        hiddenLayer.initWeights(-.5, .5);
        outputLayer.initWeights(-.3, .3);
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

    public void training(List<Sample> samples) {
        initWeights();
        boolean continueTraining = true;
        int learningStep = 0;
        double error = 0; // ------------------------------------------ Len na vypisy
        double previousError = 99; // ------------------------------------------ Len na vypisy
        double deltaWeightsVectorLength = 0; // ------------------------------------------ Len na vypisy

        do {
            resetDeltaWeights();
            error = 0;  // ---------------------------------------------- Len na vypisy
            deltaWeightsVectorLength = 0;// ---------------------------------------------- Len na vypisy

            List<Sample> miniBatch = new ArrayList<>(samples);
//            Collections.shuffle(miniBatch, new Random(learningStep));
//            miniBatch = miniBatch.subList(0, samples.size() / 10);

            for (Sample sample : miniBatch) {
                feedForward(sample.inputs, false);

                for (int i = 0; i < outputLayer.outputs.length; i++) {
                    outputLayer.errorDsRespectY[i] = outputLayer.outputs[i] - sample.desiredOutputs[i];
                    error += 0.5 * Math.pow(outputLayer.errorDsRespectY[i], 2);  // --------------------------- Len na vypisy
                }

                for (int i = 0; i < hiddenLayer.outputs.length; i++) {
                    double errDRespectYi = 0;
                    for (int j = 0; j < outputLayer.outputs.length; j++) {
                        errDRespectYi += outputLayer.errorDsRespectY[j] * outputLayer.weights[j][i] * outputLayer.dTanh(outputLayer.outputs[j]);
                    }
                    hiddenLayer.errorDsRespectY[i] = errDRespectYi;
                }

                for (int i = 0; i < outputLayer.weights.length; i++) {
                    for (int j = 0; j < outputLayer.weights[i].length; j++) {
                        outputLayer.deltaWeights[i][j] += outputLayer.errorDsRespectY[i] * hiddenLayer.outputs[j] * outputLayer.dTanh(outputLayer.outputs[i]); // TODO optimalizovat, sigmoid sa uz pocital
                        deltaWeightsVectorLength += Math.pow(outputLayer.deltaWeights[i][j], 2);  // --------------------------- Len na vypisy
                    }
                }

                for (int i = 0; i < hiddenLayer.weights.length; i++) {
                    for (int j = 0; j < hiddenLayer.inputs.length; j++) {
                        hiddenLayer.deltaWeights[i][j] += hiddenLayer.errorDsRespectY[i] * hiddenLayer.inputs[j] * hiddenLayer.dTanh(hiddenLayer.outputs[i]);
                        deltaWeightsVectorLength += Math.pow(hiddenLayer.deltaWeights[i][j], 2);  // --------------------------- Len na vypisy
                    }
                    hiddenLayer.deltaWeights[i][hiddenLayer.inputs.length] += hiddenLayer.errorDsRespectY[i] * hiddenLayer.dTanh(hiddenLayer.outputs[i]);
                    deltaWeightsVectorLength += Math.pow(hiddenLayer.deltaWeights[i][hiddenLayer.inputs.length], 2);  // --------------------------- Len na vypisy
                }
            }

            updateWeights(learningRate);

//            if (error >= previousError) {
//                System.out.println("**********************************************************************************");
//                System.out.println("*******************PREVIOUS ERROR: " + previousError + "**************************");
//                System.out.println("****************************ERROR: " + error + "**********************************");
//                System.out.println("**********************************************************************************");
//                System.out.println("");
//            }


//            if(learningStep % 10 == 0) {
//                System.out.println("Error: " + error);
//            }

            previousError = error;
            if (learningStep % printStatusFreq == 0) {  // ------------------------------------------ Len na vypisy
                System.out.println("Delta weigth length: " + Math.sqrt(deltaWeightsVectorLength) + "    Error: " + error);
            }
            learningStep++;

            if(learningStep % 100 == 0) {
                learningRate *= 0.9;
            }

//            System.out.println();
//            System.out.println("-----");
//            System.out.println();
        } while (learningStep < 1_000);
    }

    public double[] feedForward(double[] inputs, boolean printPotentials) {
        hiddenLayer.inputs = inputs;
        hiddenLayer.evaluate();
        double[] output = outputLayer.evaluate();

        //----------------------------- Len výpis
        if (printPotentials) {
            System.out.println("--------------------------------------");
            StringBuilder s = new StringBuilder("Output potentials: ");
            for (int i = 0; i < outputLayer.potentials.length; i++) {
                s.append(outputLayer.potentials[i]).append(", ");
            }
            System.out.println(s);
            s = new StringBuilder("Hidden potentials: ");
            for (int i = 0; i < hiddenLayer.potentials.length; i++) {
                s.append(hiddenLayer.potentials[i]).append(", ");
            }
            System.out.println(s);
            System.out.println("-");
        }
        return output;
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
