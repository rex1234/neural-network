/**
 * Created by MiHu on 17.11.2016.
 */
public class Connection {

    public Neuron incomingNeuron;
    public double weight;
    public double deltaWeight;

    public Connection(Neuron incomingNeuron, double weight) {
        this.incomingNeuron = incomingNeuron;
        this.weight = weight;
    }
}
