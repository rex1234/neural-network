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

        //                 Input,  Hidden,  Output,  Steepness,  Learning rate
        MLP mlp = new MLP( 2,      5,       1,       1.0,        5);

        mlp.training(samples);

        System.out.println("Vstup: [1,0] Výstup: " + mlp.feedForward(new double[]{1, 0})[0]);
        System.out.println("Vstup: [1,1] Výstup: " + mlp.feedForward(new double[]{1, 1})[0]);
        System.out.println("Vstup: [0,1] Výstup: " + mlp.feedForward(new double[]{0, 1})[0]);
        System.out.println("Vstup: [0,0] Výstup: " + mlp.feedForward(new double[]{0, 0})[0]);


    }
}
