import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by MiHu on 17.11.2016.
 */
public class Neuron {

    private static double WEIGHT_INIT = 0.3;

    public ArrayList<Connection> connections = new ArrayList<>();

    public double value;

    public Neuron(ArrayList<Neuron> lowerLayer) {
        for (Neuron n : lowerLayer) {
            connections.add(new Connection(n, WEIGHT_INIT));
        }
        connections.add(new Connection(new Neuron(1), WEIGHT_INIT));
    }

    public Neuron(double value) {
        this.value = value;
    }

    public void evaluate() {
        double innerPotential = 0;
        for (Connection connection : connections) {
            innerPotential += connection.incomingNeuron.value * connection.weight;
        }
        value = 1 / (1 + Math.exp(MLP.sigmoidSteepness * innerPotential));
    }

}
