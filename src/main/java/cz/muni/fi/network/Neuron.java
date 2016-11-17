package cz.muni.fi.network;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by MiHu on 17.11.2016.
 */
public class Neuron {

    private static double WEIGHT_INIT = 0.3;

    public ArrayList<Connection> connections = new ArrayList<>();
    public HashMap<Neuron, Connection> connectionsMap = new HashMap<>();

    public double innerPotential;
    public double value;

    public Neuron(ArrayList<Neuron> lowerLayer) {
        for (Neuron n : lowerLayer) {
            Connection conn = new Connection(n, WEIGHT_INIT);
            connections.add(conn);
            connectionsMap.put(n, conn);
        }
        Neuron neuron = new Neuron(1);
        Connection connection = new Connection(neuron, WEIGHT_INIT);
        connections.add(connection);
        connectionsMap.put(neuron, connection);
    }

    public Neuron(double value) {
        this.value = value;
    }

    public void evaluate() {
        innerPotential = 0;
        for (Connection connection : connections) {
            innerPotential += connection.incomingNeuron.value * connection.weight;
        }
        value = 1 / (1 + Math.exp(-MLP.sigmoidSteepness * innerPotential));
    }

    public double sigmaPrime() {
        return MLP.sigmoidSteepness * value * (1 - value);
    }

    public void updateWeights() {
        for (Connection connection : connections) {
            connection.weight += connection.deltaWeight;
        }
    }

}
