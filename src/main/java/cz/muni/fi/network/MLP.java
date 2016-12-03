package cz.muni.fi.network;

import javax.swing.*;
import java.util.List;

/**
 * Created by MiHu on 17.11.2016.
 */
public class MLP {

    public double learningRate;
    public double momentumInfluence;
    public int numLearningSteps;
    public boolean showGraph;

    private double hiddenWeights;
    private double outputWeights;

    public Layer outputLayer;
    public Layer hiddenLayer;

    private int printStatusFreq;
    private double errors[];
    private double errorDerivatives[];

    public MLP(int numInputNeurons, int numHiddenNeurons, int numOutputNeurons, int numLearningSteps, boolean showGraph,
               double learningRate, boolean glorotBengioWeights, int printStatusFreq, double momentumInfluence) {

        if (glorotBengioWeights) {
            this.hiddenWeights = Math.sqrt(6 / (numInputNeurons + numOutputNeurons));
            this.outputWeights = Math.sqrt(6 / (numHiddenNeurons + 1));
        } else {
            this.hiddenWeights = Math.sqrt(3) / Math.sqrt(numInputNeurons);
            this.outputWeights = Math.sqrt(3) / Math.sqrt(numHiddenNeurons);
        }
        System.out.println("--------- WEIGHTS BEING INITIALIZED RANDOMLY ---------------");
        System.out.println("Hidden weights: " + hiddenWeights);
        System.out.println("Output weights: " + outputWeights);
        this.momentumInfluence = momentumInfluence;
        this.learningRate = learningRate;
        this.printStatusFreq = printStatusFreq;
        this.numLearningSteps = numLearningSteps;
        this.errors = new double[numLearningSteps];
        this.errorDerivatives = new double[numLearningSteps];
        this.showGraph = showGraph;

        outputLayer = new Layer(this, numOutputNeurons, numHiddenNeurons, false);
        hiddenLayer = new Layer(this, numHiddenNeurons, numInputNeurons, true);
        outputLayer.inputs = hiddenLayer.outputs;
    }

    private void initWeights() {
        hiddenLayer.initWeights(-hiddenWeights, hiddenWeights);
        outputLayer.initWeights(-outputWeights, outputWeights);
    }

    public void training(List<Sample> samples) {
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(1024, 1024);
        f.setLocation(10, 10);
        initWeights();
        printWeights();
        int learningStep = 0;
        double invertedSampleCount = 1d / samples.size();
        do {
            resetErrorDsRespectW();

            for (Sample sample : samples) {
                feedForward(sample.inputs, false);
                for (int i = 0; i < outputLayer.outputs.length; i++) {
                    outputLayer.errorDsRespectY[i] = outputLayer.outputs[i] - sample.desiredOutputs[i];
                    errors[learningStep] += invertedSampleCount * Math.pow(outputLayer.errorDsRespectY[i], 2);  // --------------------------- Len na vypisy
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
                        outputLayer.errorDsRespectW[i][j] += invertedSampleCount * (outputLayer.errorDsRespectY[i] * hiddenLayer.outputs[j] * outputLayer.dTanh(outputLayer.outputs[i])); // TODO optimalizovat, sigmoid sa uz pocital
                    }
                }

                for (int i = 0; i < hiddenLayer.weights.length; i++) {
                    for (int j = 0; j < hiddenLayer.inputs.length; j++) {
                        hiddenLayer.errorDsRespectW[i][j] += invertedSampleCount * (hiddenLayer.errorDsRespectY[i] * hiddenLayer.inputs[j] * hiddenLayer.dTanh(hiddenLayer.outputs[i]));
                    }
                    hiddenLayer.errorDsRespectW[i][hiddenLayer.inputs.length] += invertedSampleCount * (hiddenLayer.errorDsRespectY[i] * hiddenLayer.dTanh(hiddenLayer.outputs[i]));
                }
            }
            errorDerivatives[learningStep] = errorDerivative();
            updateWeights(learningRate);
            if (learningStep % printStatusFreq == 0) {  // ------------------------------------------ Len na vypisy
                System.out.println(String.format("Lning rate: %.7f ", learningRate) + String.format("| Error derivative: %.8f ", Math.sqrt(errorDerivatives[learningStep])) + String.format("| Err: %.8f", errors[learningStep]));
            }
            if (showGraph && learningStep % (5 * printStatusFreq) == 0) {
                f.getContentPane().add(new Graph(errors, errorDerivatives, numLearningSteps));
                f.setVisible(true);
                System.out.println("Printing graph");
            }
//            if (learningStep % 30 == 0) {
//                learningRate *= 0.98;
//            }
            learningStep++;
        } while (learningStep < numLearningSteps);
        System.out.println("---------    TRAINING FINISHED   ----------");
    }

    public double[] feedForward(double[] inputs, boolean printPotentials) {
        hiddenLayer.inputs = inputs;
        hiddenLayer.evaluate();
        double[] output = outputLayer.evaluate();

        if (printPotentials) {  //----------------------------- Len výpis
            System.out.println("-");
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
        }
        return output;
    }

    private void updateWeights(double learningRate) {
        hiddenLayer.updateWeights(learningRate);
        outputLayer.updateWeights(learningRate);
    }

    private void resetErrorDsRespectW() {
        hiddenLayer.resetErrorDsRespectW();
        outputLayer.resetErrorDsRespectW();
    }

    private double errorDerivative() {
        double errorDerivative = 0;
        for (int i = 0; i < outputLayer.weights.length; i++) {
            for (int j = 0; j < outputLayer.weights[i].length; j++) {
                errorDerivative += Math.pow(outputLayer.errorDsRespectW[i][j], 2);
            }
        }
        for (int i = 0; i < hiddenLayer.weights.length; i++) {
            for (int j = 0; j < hiddenLayer.inputs.length; j++) {
                errorDerivative += Math.pow(hiddenLayer.errorDsRespectW[i][j], 2);
            }
            errorDerivative += Math.pow(hiddenLayer.errorDsRespectW[i][hiddenLayer.inputs.length], 2);
        }
        return Math.sqrt(errorDerivative);
    }

    private void printWeights() {
        System.out.println("---------   WEIGHTS     -------");
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
