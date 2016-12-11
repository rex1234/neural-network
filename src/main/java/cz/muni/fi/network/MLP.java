package cz.muni.fi.network;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static cz.muni.fi.network.Graph.getScreenShot;

/**
 * Created by MiHu on 17.11.2016.
 */
public class MLP {

    public double learningRate;
    public double momentumInfluence;
    public int numLearningSteps;
    private String imgName;
    public boolean showGraph;
    private int decLearningRateFreq;
    private int minibatchSize;
    private double hiddenWeights;
    private double outputWeights;
    private boolean dropoutOn;
    private boolean minibatchOn;
    private Random r = new Random();

    public Layer outputLayer;
    public Layer hiddenLayer;

    private int printStatusFreq;
    private double errors[];
    private double errorDerivatives[];
    private JFrame f;

    public MLP(int numInputNeurons, int numHiddenNeurons, int numOutputNeurons, int numLearningSteps, boolean showGraph, String imgName,
               double learningRate, boolean glorotBengioWeights, int printStatusFreq, double momentumInfluence, int decLearningRateFreq,
               boolean dropoutOn, boolean minibatchOn, int minibatchSize) {
        System.out.println("----------------------------- NEW MLP INIT -----------------------------");
        System.out.println("----------------------------------  batch" + imgName + "  -----------------------------------");
        System.out.println("-----------------------------------  -----------------------------------");
        System.out.println("-----------------------------------  -----------------------------------");
        System.out.println("Num learning steps: " + numLearningSteps);
        System.out.println("Learning rate: " + learningRate);
        System.out.println("Frequency of decreasing learning rate: " + decLearningRateFreq);
        System.out.println("Glorot and Bengio Weights: " + glorotBengioWeights);
        System.out.println("Momentum influence: " + momentumInfluence);
        System.out.println("Print status frequency: " + printStatusFreq);
        System.out.println("Dropout " + (dropoutOn ? "ON" : "OFF"));
        System.out.println("Minibatch " + (minibatchOn ? "ON " + minibatchSize + " size" : "OFF"));
        if (glorotBengioWeights) {
            this.hiddenWeights = Math.sqrt(6d / (numInputNeurons + numOutputNeurons));
            this.outputWeights = Math.sqrt(6d / (numHiddenNeurons + 1));
        } else {
            this.hiddenWeights = Math.sqrt(3) / Math.sqrt(numInputNeurons);
            this.outputWeights = Math.sqrt(3) / Math.sqrt(numHiddenNeurons);
        }
        System.out.println("--------- WEIGHTS BEING INITIALIZED RANDOMLY ---------------");
        System.out.println("Hidden weights: " + hiddenWeights);
        System.out.println("Output weights: " + outputWeights);
        this.momentumInfluence = momentumInfluence;
        this.dropoutOn = dropoutOn;
        this.imgName = imgName;
        this.minibatchOn = minibatchOn;
        this.minibatchSize = minibatchSize;
        this.decLearningRateFreq = decLearningRateFreq;
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
        f = prepareGraph();
        initWeights();
        int learningStep = 0;
        double invertedSampleCount = minibatchOn ? 1d / minibatchSize : 1d / samples.size();
        double preprocessed[] = new double[outputLayer.outputs.length];
        int batchIteration = 0;
        List<Sample> minibatch = samples;
        do {
            resetErrorDsRespectW();
            if (minibatchOn) {
                if ((batchIteration + 1) * minibatchSize > samples.size()) {
                    Collections.shuffle(samples, r);
                    batchIteration = 0;
                }
                minibatch = samples.subList(batchIteration * minibatchSize, (batchIteration + 1) * minibatchSize);
                batchIteration++;
            }
            for (Sample sample : minibatch) {
                feedForward(sample.inputs, false);
                for (int i = 0; i < outputLayer.outputs.length; i++) {
                    outputLayer.errorDsRespectY[i] = outputLayer.outputs[i] - sample.desiredOutputs[i];
                    errors[learningStep] += invertedSampleCount * Math.pow(outputLayer.errorDsRespectY[i], 2);  // --------------------------- Len na vypisy
                }

                for (int j = 0; j < outputLayer.outputs.length; j++) {
                    preprocessed[j] = outputLayer.errorDsRespectY[j] * outputLayer.dTanh(outputLayer.outputs[j]);
                }
                for (int i = 0; i < hiddenLayer.outputs.length; i++) {
                    double errDRespectYi = 0;
                    for (int j = 0; j < outputLayer.outputs.length; j++) {
                        errDRespectYi += preprocessed[j] * outputLayer.weights[j][i];
                    }
                    hiddenLayer.errorDsRespectY[i] = errDRespectYi;
                }

                for (int i = 0; i < outputLayer.weights.length; i++) {
                    for (int j = 0; j < outputLayer.weights[i].length; j++) {
                        outputLayer.errorDsRespectW[i][j] += invertedSampleCount * (preprocessed[i] * hiddenLayer.outputs[j]);
                    }
                }

                for (int i = 0; i < hiddenLayer.weights.length; i++) {
                    for (int j = 0; j < hiddenLayer.inputs.length; j++) {
                        hiddenLayer.errorDsRespectW[i][j] += invertedSampleCount * (hiddenLayer.errorDsRespectY[i] * hiddenLayer.inputs[j] * hiddenLayer.dTanh(hiddenLayer.outputs[i]));
                    }
                    hiddenLayer.errorDsRespectW[i][hiddenLayer.inputs.length] += invertedSampleCount * (hiddenLayer.errorDsRespectY[i] * hiddenLayer.dTanh(hiddenLayer.outputs[i]));
                }
            }
            errorDerivatives[learningStep] = errorDerivative();//---------------Len vypis
            updateWeights(learningRate);
            if (learningStep % printStatusFreq == 0) {  //------------------------------------------ Len na vypisy
                System.out.println(String.format("Lning rate: %.6f ", learningRate) + String.format("| Error derivative: %.8f ", errorDerivatives[learningStep]) + String.format("| Err: %.8f", errors[learningStep]));
            }
            if (showGraph && learningStep % (5 * printStatusFreq) == 0) {
                drawGraph();
            }
            if (learningStep % decLearningRateFreq == 0) {
                learningRate *= 0.99;
            }
            learningStep++;
        } while (learningStep < numLearningSteps);
        System.out.println("--------------    TRAINING FINISHED   --------------");
        drawGraphAndWriteImg(imgName);
    }

    public double[] feedForward(double[] inputs, boolean printPotentials) {
        hiddenLayer.inputs = inputs;
        hiddenLayer.evaluate();
        double[] output = outputLayer.evaluate();

//        if (printPotentials) {  //----------------------------- Len výpis
//            System.out.println("-");
//            StringBuilder s = new StringBuilder("Output potentials: ");
//            for (int i = 0; i < outputLayer.potentials.length; i++) {
//                s.append(outputLayer.potentials[i]).append(", ");
//            }
//            System.out.println(s);
//            s = new StringBuilder("Hidden potentials: ");
//            for (int i = 0; i < hiddenLayer.potentials.length; i++) {
//                s.append(hiddenLayer.potentials[i]).append(", ");
//            }
//            System.out.println(s);
//        }
        return output;
    }

    private void updateWeights(double learningRate) {
        hiddenLayer.updateWeights(learningRate, dropoutOn);
        outputLayer.updateWeights(learningRate, dropoutOn);
    }

    private void drawGraph() {
        f.getContentPane().add(new Graph(errors, errorDerivatives, numLearningSteps));
        f.setVisible(true);
    }

    private void drawGraphAndWriteImg(String filename) {
        drawGraph();
        BufferedImage img = getScreenShot(f.getContentPane());
        File outputfile = new File(filename + ".jpg");
        try {
            ImageIO.write(img, "jpg", outputfile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resetErrorDsRespectW() {
        hiddenLayer.resetErrorDsRespectW();
        outputLayer.resetErrorDsRespectW();
    }

    private JFrame prepareGraph() {
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(1024, 1024);
        f.setLocation(10, 10);
        return f;
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
