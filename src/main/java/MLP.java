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

        outputLayer = new Layer(this, numOutputNeurons, numHiddenNeurons);
        hiddenLayer = new Layer(this, numHiddenNeurons, numInputNeurons);
        outputLayer.inputs = hiddenLayer.outputs;
    }

    public void training(ArrayList<Sample> samples) {
        initWeights();
        boolean continueTraining = true;
        int learningStep = 0;
        do {
            learningStep++;
            resetDeltaWeights();
            for (Sample sample : samples) {
                feedForward(sample.inputs);

                for (int i = 0; i < outputLayer.outputs.length; i++) {
                    outputLayer.errorDsRespectY[i] = outputLayer.outputs[i] - sample.desiredOutputs[i];
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
                    }
                }

                for (int i = 0; i < hiddenLayer.weights.length; i++) {
                    for (int j = 0; j < hiddenLayer.weights[i].length; j++) {
                        hiddenLayer.deltaWeights[i][j] += hiddenLayer.errorDsRespectY[i] * hiddenLayer.inputs[j] * hiddenLayer.dSigmoid(hiddenLayer.outputs[i]);
                    }
                }
            }
            updateWeights(learningRate);

        } while (learningStep < 100);
    }

    public double[] feedForward(double[] inputs) {
        hiddenLayer.inputs = inputs;
        hiddenLayer.evaluate();
        return outputLayer.evaluate();
    }

    private void initWeights() {
        hiddenLayer.initWeights();
        outputLayer.initWeights();
    }

    private void updateWeights(double learningRate) {
        hiddenLayer.updateWeights(learningRate);
        outputLayer.updateWeights(learningRate);
    }

    private void resetDeltaWeights() {
        hiddenLayer.resetDeltaWeights();
        outputLayer.resetDeltaWeights();
    }
}
