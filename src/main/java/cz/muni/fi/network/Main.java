package cz.muni.fi.network;

import cz.muni.fi.datascrapper.DataTools;
import cz.muni.fi.datascrapper.model.Movie;
import cz.muni.fi.datascrapper.model.Person;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by MiHu on 11.11.2016.
 */
public class Main {

    public static void main(String[] args) throws Exception {

        trainOnMovies();

        //testTraining();
    }

    private static void testTraining() {
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

    private static void trainOnMovies() throws IOException{
        List<Person> actors = DataTools.getBaseActors();
        List<Movie> movies = DataTools.getMoviesFromJson();

        List<Sample> samples = new ArrayList<>();

        Collections.shuffle(movies, new Random(55));

        int trainingSize = 500;

        for (Movie movie : movies) {


            if(--trainingSize == 0) {
                break;
            }

            //error
            if(movie.getRating() < 0.1) {
                continue;
            }

            double[] outputs = new double[] {movie.getRating()};
            double[] inputs = new double[1000];

            for (String actorId : movie.getActors()) {
                inputs[actors.indexOf(new Person(actorId))] = 1;
            }

            samples.add(new Sample(inputs, outputs));
        }

        //                 Num Inputs,  Num Hidden,  Num Outputs,  Sigmoid steepness,
        MLP mlp = new MLP( 1000,           11,          1,            2.0,
                //     Learning rate,  Weight init min,  Weight init max,  Print status frequency
                0.5,              -0.2,             0.2,                10   );


        mlp.training(samples);

        for (int i = 500; i < movies.size(); i++) {
            //error
            if(movies.get(i).getRating() < 0.1) {
                continue;
            }

            double[] inputs = new double[1000];

            for (String actorId : movies.get(i).getActors()) {
                inputs[actors.indexOf(new Person(actorId))] = 1;
            }

            System.out.println(String.format("Movie : %s, rating: %.1f", movies.get(i).getName(), movies.get(i).getRating()));
            System.out.println(String.format("Predicted rating: %.1f", mlp.feedForward(inputs, true)[0]));

        }
    }
}
