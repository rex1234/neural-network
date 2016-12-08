package cz.muni.fi.network;

import cz.muni.fi.datascrapper.DataTools;
import cz.muni.fi.datascrapper.model.Movie;
import cz.muni.fi.datascrapper.model.Person;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by MiHu on 11.11.2016.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        //PrintStream out = new PrintStream(new FileOutputStream("output.txt"));
        //System.setOut(out);

        trainOnMovies(500, 100, 2, true);
    }

    private static void sinTraining() {
        ArrayList<Sample> samples = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            double x = new Random(i).nextInt((int) (2 * Math.PI * 1000)) / 1000d;
            samples.add(new Sample(new double[]{x / 7}, new double[]{(Math.sin(x) + 1) / 2}));
        }

        //    Num Inputs,  Num Hidden,  Num Outputs, Num Learning steps, Show Graph, Output image name
        MLP mlp = new MLP(1, 2, 1, 1000, true, "2",
                //Learning rate, Use Glorot & Bengio weight init? ,  Print status frequency, Momentum influence, Frequency of decreasing learning rate
                0.15, false, 10, 0.65, 30);

        mlp.training(samples);

        for (double x = 0.1; x < Math.PI * 2; x = x + 0.1) {
            System.out.printf("%.1f = %.4f%n", x, (Math.sin(x) + 1) / 2);
            System.out.printf("Výstup: %.4f%n", mlp.feedForward(new double[]{x / 7}, false)[0]);
            System.out.println();
        }

    }

    private static void xorTraining() {
        ArrayList<Sample> samples = new ArrayList<>();
        samples.add(new Sample(new double[]{1, 1}, new double[]{0}));
        samples.add(new Sample(new double[]{1, 0}, new double[]{1}));
        samples.add(new Sample(new double[]{0, 1}, new double[]{1}));
        samples.add(new Sample(new double[]{0, 0}, new double[]{0}));

        //    Num Inputs,  Num Hidden,  Num Outputs, Num Learning steps, Show Graph, Output image name
        MLP mlp = new MLP(2, 2, 1, 1000, true, "1",
                //Learning rate, Use Glorot & Bengio weight init? ,  Print status frequency, Momentum influence, Frequency of decreasing learning rate
                0.15, false, 10, 0.5, 15);

        mlp.training(samples);

        System.out.println("Vstup: [1,0] Výstup: " + mlp.feedForward(new double[]{1, 0}, true)[0]);
        System.out.println("Vstup: [1,1] Výstup: " + mlp.feedForward(new double[]{1, 1}, true)[0]);
        System.out.println("Vstup: [0,1] Výstup: " + mlp.feedForward(new double[]{0, 1}, true)[0]);
        System.out.println("Vstup: [0,0] Výstup: " + mlp.feedForward(new double[]{0, 0}, true)[0]);
    }

    private static void trainOnMovies(final int desiredActorCount, final int desiredDirectorsCount, final int actorsInMovie, boolean useDummyNeurons) throws IOException {
        //
        // load training data from json
        //
        List<Person> actors = DataTools.getBaseActors();
        List<Person> directors = DataTools.getBaseDirectors();
        List<Movie> movies = DataTools.getMoviesWDirectorsFromJson();

        //
        // keep only top frequent 500 actors / 100 directors
        //
        for (Movie movie : movies) {
            for (String actor : movie.getActors()) {
                for (Person person : actors) {
                    if (person.getId().equals(actor)) {
                        person.count++;
                    }
                }
            }

            for (Person director : directors) {
                if (director.getId().equals(movie.getDirector())) {
                    director.count++;
                }
            }
        }

        Collections.sort(actors, (b, a) -> b.count - a.count);
        Collections.sort(directors, (b, a) -> b.count - a.count);

        int outputSize = desiredActorCount + desiredDirectorsCount + (useDummyNeurons ? (1 + actorsInMovie) : 0);

        Set<String> removedActors = actors.subList(desiredActorCount, actors.size()).stream().map(a -> a.getId()).collect(Collectors.toSet());
        actors = actors.subList(0, desiredActorCount);
        Set<String> removedDirectors = actors.subList(desiredDirectorsCount, actors.size()).stream().map(a -> a.getId()).collect(Collectors.toSet());
        directors = directors.subList(0, desiredDirectorsCount);

        //
        // remove removed actors / directors from movies
        //

        for (Movie movie : movies) {
            movie.getActors().removeAll(removedActors);
            if(removedDirectors.contains(movie.getDirector())) {
                movie.setDirector(null);
            }
        }

        //
        // remove movies unfit for training (w/o director or 4 actors)
        //

        movies = movies.stream().filter(m -> !m.filterMovie(actorsInMovie, useDummyNeurons)).collect(Collectors.toList());

        List<Sample> samples = new ArrayList<>();
        Collections.shuffle(movies, new Random(55));

        int trainingSize = (int) (movies.size() * 0.9);

        System.out.printf("Training on %d movies%n", trainingSize);

        int done = 0;
        for (Movie movie : movies) {
            if(done++ == trainingSize) {
                break;
            }

            double[] outputs = new double[]{movie.getRating() / 5 - 1};
            double[] inputs = new double[outputSize];

            int missingActors = 0;
            for (String actorId : movie.getActors()) {
                int i = actors.indexOf(new Person(actorId));

                if(i == -1) {
                    ++missingActors;
                } else {
                    inputs[i] = 1;
                }
            }

            if(useDummyNeurons) {
                int peopleNeurons = desiredActorCount + desiredDirectorsCount;
                for (int i = peopleNeurons; i < peopleNeurons + missingActors; i++) {
                    inputs[i] = 1;
                }
            }

            int directorIndex = directors.indexOf(new Person(movie.getDirector()));
            if(directorIndex == -1) {
                inputs[inputs.length - 1] = 1;
            } else {
                inputs[desiredActorCount + directorIndex] = 1;
            }

            if(!useDummyNeurons && (directorIndex == -1 || missingActors > 0)) {
                continue;
            }

            samples.add(new Sample(inputs, outputs));
        }

        System.out.println(samples.size());

        //    Num Inputs,  Num Hidden,  Num Outputs, Num Learning steps, Show Graph, Output image name
        MLP mlp = new MLP(outputSize, 40, 1, 1000, true, "7",
                //Learning rate, Use Glorot & Bengio weight init? ,  Print status frequency, Momentum influence, Frequency of decreasing learning rate
                0.2, false, 30, 0.7, 80);
        mlp.training(samples);

        int[] diffs = new int[] {0,0,0,0};

        for (int i = trainingSize; i < movies.size(); i++) {

            if(i == trainingSize) {
                System.out.println("***");
                System.out.println((movies.size() - trainingSize) + "movies not from the training set:");
                System.out.println("***");
            }

            double[] inputs = new double[outputSize];

            for (String actorId : movies.get(i).getActors()) {
                int j = actors.indexOf(new Person(actorId));
                inputs[j] = 1;
            }

            int directorIndex = directors.indexOf(new Person(movies.get(i).getDirector()));
            inputs[desiredActorCount + directorIndex] = 1;

            float rating = movies.get(i).getRating();
            double predictedRating = (mlp.feedForward(inputs, false)[0] + 1) * 5;

            System.out.printf("Movie : %s, rating: %.1f%n", movies.get(i).getName(), rating);
            System.out.printf("Predicted rating: %.1f%n%n", predictedRating);

            if(Math.abs(rating - predictedRating) < 0.5) {
                ++diffs[0];
            } else if(Math.abs(rating - predictedRating) < 1) {
                ++diffs[1];
            } else if(Math.abs(rating - predictedRating) < 2) {
                ++diffs[2];
            } else {
                ++diffs[3];
            }
        }

        int total = Arrays.stream(diffs).sum();

        System.out.printf("******************************%n");
        System.out.printf("*    < 0.5       *    %.2f %%  *%n", percent(diffs[0], total));
        System.out.printf("******************************%n");
        System.out.printf("*    < 1         *    %.2f %%  *%n", percent(diffs[1], total));
        System.out.printf("******************************%n");
        System.out.printf("*    < 2         *    %.2f %%  *%n", percent(diffs[2], total));
        System.out.printf("******************************%n");
        System.out.printf("*    >= 2        *    %.2f %%  *%n", percent(diffs[3], total));
        System.out.printf("******************************%n");
    }

    private static float percent(int x, int total) {
        return x/(float)total * 100f;
    }
}
