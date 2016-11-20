package cz.muni.fi.network;

import java.util.ArrayList;

/**
 * Created by MiHu on 11.11.2016.
 */
public class Main {

    public static void main(String[] args) {

        ArrayList<Sample> samples = new ArrayList<>();
        samples.add(new Sample(new double[]{1, 1}, new double[]{0}));
        samples.add(new Sample(new double[]{1, 0}, new double[]{1}));
        samples.add(new Sample(new double[]{0, 1}, new double[]{1}));
        samples.add(new Sample(new double[]{0, 0}, new double[]{0}));

        //                 Num Inputs,  Num Hidden,  Num Outputs,  Sigmoid steepness,
        MLP mlp = new MLP( 2,           2,           1,            2.0,
        //     Learning rate,  Weight init min,  Weight init max,  Print status frequency
               0.5,              -0.2,             0.2,                10   );

        mlp.training(samples);
        System.out.println("----------------------------------------------------------------------");
        System.out.println("Vstup: [1,0] Výstup: " + mlp.feedForward(new double[]{1, 0}, true)[0]);
        System.out.println("Vstup: [1,1] Výstup: " + mlp.feedForward(new double[]{1, 1}, true)[0]);
        System.out.println("Vstup: [0,1] Výstup: " + mlp.feedForward(new double[]{0, 1}, true)[0]);
        System.out.println("Vstup: [0,0] Výstup: " + mlp.feedForward(new double[]{0, 0}, true)[0]);


    }
}
