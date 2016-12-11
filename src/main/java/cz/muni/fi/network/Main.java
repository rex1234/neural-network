package cz.muni.fi.network;

import cz.muni.fi.datascrapper.DataTools;
import cz.muni.fi.datascrapper.model.Movie;
import cz.muni.fi.datascrapper.model.Person;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by MiHu on 11.11.2016.
 */
public class Main {

    private static List<Person> actors;
    private static List<Person> directors;
    private static List<Movie> movies;

    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(new FileOutputStream("output.txt"));
        System.setOut(out);
//        xorTraining();

        //ACTORS count, DIRECTORS count, ACTORS in movie, use DUMMY neurons, num HIDDEN neurons, Learning steps, show graph
        trainOnMovies(1000, 200, 2, true, 40, 500, false, "1",
                //learning rate, glorotBengioWeights, print status freq, momentum, Dec learnRatefreq
                0.2, true, 10, 0.5, 20,
                // dropout ON, Minibatch ON, Minibatch size
                true, false, 0);

        //ACTORS count, DIRECTORS count, ACTORS in movie, use DUMMY neurons, num HIDDEN neurons, Learning steps, show graph
        trainOnMovies(1000, 200, 2, true, 40, 500, false, "1",
                //learning rate, glorotBengioWeights, print status freq, momentum, Dec learnRatefreq
                0.2, true, 10, 0.5, 20,
                // dropout ON, Minibatch ON, Minibatch size
                true, false, 0);

        //ACTORS count, DIRECTORS count, ACTORS in movie, use DUMMY neurons, num HIDDEN neurons, Learning steps, show graph
        trainOnMovies(1000, 200, 2, true, 30, 500, false, "2",
                //learning rate, glorotBengioWeights, print status freq, momentum, Dec learnRatefreq
                0.2, true, 10, 0.2, 35,
                // dropout ON, Minibatch ON, Minibatch size
                true, false, 0);

        //ACTORS count, DIRECTORS count, ACTORS in movie, use DUMMY neurons, num HIDDEN neurons, Learning steps, show graph
        trainOnMovies(1000, 200, 2, true, 50, 500, false, "3",
                //learning rate, glorotBengioWeights, print status freq, momentum, Dec learnRatefreq
                0.2, true, 10, 0.0, 50,
                // dropout ON, Minibatch ON, Minibatch size
                true, false, 0);

        //ACTORS count, DIRECTORS count, ACTORS in movie, use DUMMY neurons, num HIDDEN neurons, Learning steps, show graph
        trainOnMovies(1000, 200, 2, true, 40, 500, false, "4",
                //learning rate, glorotBengioWeights, print status freq, momentum, Dec learnRatefreq
                0.2, true, 10, 0.5, 40,
                // dropout ON, Minibatch ON, Minibatch size
                false, false, 0);

        //ACTORS count, DIRECTORS count, ACTORS in movie, use DUMMY neurons, num HIDDEN neurons, Learning steps, show graph
        trainOnMovies(1000, 200, 2, true, 40, 500, false, "5",
                //learning rate, glorotBengioWeights, print status freq, momentum, Dec learnRatefreq
                0.3, true, 10, 0.5, 30,
                // dropout ON, Minibatch ON, Minibatch size
                false, false, 0);
    }

    private static void trainOnMovies(final int desiredActorCount, final int desiredDirectorsCount, final int actorsInMovie, boolean useDummyNeurons,
                                      int numHiddenNeurons, int numLearningSteps, boolean showGraph, String imgName,
                                      double learningRate, boolean glorotBengioWeights, int printStatusFreq, double momentumInfluence, int decLearningRateFreq,
                                      boolean dropoutOn, boolean minibatchOn, int minibatchSize) throws IOException {

        //
        // load training data from json
        //
        actors = DataTools.getBaseActors();
        directors = DataTools.getBaseDirectors();
        movies = DataTools.getMoviesWDirectorsFromJson();

        //
        // keep only top frequent 'desiredActorCount' actors / 'desiredDirectorsCount' directors
        //
        int inputSize = desiredActorCount + desiredDirectorsCount + (useDummyNeurons ? (1 + actorsInMovie) : 0);
        removePeopleAboveLimit(desiredActorCount, desiredDirectorsCount);

        //
        // remove movies unfit for training (w/o director or 'actorsInMovie' actors)
        //

        movies = movies.stream().filter(m -> !m.filterMovie(actorsInMovie, useDummyNeurons)).collect(Collectors.toList());

        List<Sample> samples = new ArrayList<>();
        Collections.shuffle(movies, new Random(55));

        int trainingSize = (int) (movies.size() * 0.9);

        System.out.printf("Training on %d movies%n", trainingSize);

        int iteratedMovies = 0;
        for (Movie movie : movies) {
            if (iteratedMovies++ == trainingSize) {
                break;
            }

            double[] outputs = new double[]{movie.getRating() / 5 - 1};
            double[] inputs = new double[inputSize];

            if (!createInputsForMovie(desiredActorCount, desiredDirectorsCount, actorsInMovie, useDummyNeurons, movie, inputs)) {
                continue;
            }

            double inputSum = Math.round(Arrays.stream(inputs).sum());
            if (inputSum != actorsInMovie + 1) {
                throw new RuntimeException("Invalid count of set neurons " + inputSum);
            }

            samples.add(new Sample(inputs, outputs));
        }

        System.out.println(samples.size());
        MLP mlp = new MLP(inputSize, numHiddenNeurons, 1, numLearningSteps, showGraph, imgName,
                learningRate, glorotBengioWeights, printStatusFreq, momentumInfluence, decLearningRateFreq,
                dropoutOn, minibatchOn, minibatchSize);

        mlp.training(samples);

        int[] diffs = new int[]{0, 0, 0, 0};
        double mse = 0.0;

        //
        // validation
        //

        for (int i = 0; i < movies.size(); i++) {

            if (i == iteratedMovies) {
                mse *= 1d / i;
                System.out.println("Mean square error: " + mse);
                printDiffsTable(diffs);
                mse = 0.0;

                diffs = new int[]{0, 0, 0, 0};

                System.out.println("***");
                System.out.println((movies.size() - trainingSize) + "movies not from the training set:");
                System.out.println("***");
            }

            double[] inputs = new double[inputSize];

            if (!createInputsForMovie(desiredActorCount, desiredDirectorsCount, actorsInMovie, useDummyNeurons, movies.get(i), inputs)) {
                continue;
            }

            float rating = movies.get(i).getRating();
            double predictedRating = (mlp.feedForward(inputs, false)[0] + 1) * 5;

            if (i >= trainingSize) {
                System.out.printf("Movie : %s, rating: %.1f - predicted %.1f%n", movies.get(i).getName(), rating, predictedRating);
            }

            mse += Math.pow(rating - predictedRating, 2);

            if (Math.abs(rating - predictedRating) < 0.5) {
                ++diffs[0];
            } else if (Math.abs(rating - predictedRating) < 1) {
                ++diffs[1];
            } else if (Math.abs(rating - predictedRating) < 2) {
                ++diffs[2];
            } else {
                ++diffs[3];
            }
        }
        mse *= 1d / (movies.size() - iteratedMovies);
        System.out.println("Mean square error: " + mse);
        printDiffsTable(diffs);
    }

    private static void removePeopleAboveLimit(int desiredActorCount, int desiredDirectorsCount) {
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

        Set<String> removedActors = actors.subList(desiredActorCount, actors.size()).stream().map(a -> a.getId()).collect(Collectors.toSet());
        actors = actors.subList(0, desiredActorCount);
        Set<String> removedDirectors = actors.subList(desiredDirectorsCount, actors.size()).stream().map(a -> a.getId()).collect(Collectors.toSet());
        directors = directors.subList(0, desiredDirectorsCount);

        //
        // remove removed actors / directors from movies
        //

        for (Movie movie : movies) {
            movie.getActors().removeAll(removedActors);
            if (removedDirectors.contains(movie.getDirector())) {
                movie.setDirector(null);
            }
        }
    }

    private static boolean createInputsForMovie(int desiredActorCount, int desiredDirectorsCount, int actorsInMovie, boolean useDummyNeurons, Movie movie, double[] inputs) {
        int missingActors = actorsInMovie - movie.getActors().size();
        for (String actorId : movie.getActors()) {
            int i = actors.indexOf(new Person(actorId));

            if (i == -1) {
                ++missingActors;
            } else {
                inputs[i] = 1;
            }
        }

        if (useDummyNeurons) {
            int peopleNeurons = desiredActorCount + desiredDirectorsCount;
            for (int i = peopleNeurons; i < peopleNeurons + missingActors; i++) {
                inputs[i] = 1;
            }
        }

        int directorIndex = directors.indexOf(new Person(movie.getDirector()));
        if (directorIndex == -1) {
            inputs[inputs.length - 1] = 1;
        } else {
            inputs[desiredActorCount + directorIndex] = 1;
        }

        if (!useDummyNeurons && (directorIndex == -1 || missingActors > 0)) {
            return false;
        }
        return true;
    }

    private static void printDiffsTable(int[] diffs) {
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
        return x / (float) total * 100f;
    }

    private static void sinTraining() {
        ArrayList<Sample> samples = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            double x = new Random(i).nextInt((int) (2 * Math.PI * 1000)) / 1000d;
            samples.add(new Sample(new double[]{x / 7}, new double[]{(Math.sin(x) + 1) / 2}));
        }

        //    Num Inputs,  Num Hidden,  Num Outputs, Num Learning steps, Show Graph, Output image name
        MLP mlp = new MLP(1, 2, 1, 1000, true, "2",
                //Learning rate, Use Glorot & Bengio weight init? ,  Print status frequency, Momentum influence, Frequency of decreasing learning rate, Is dropout on, Is minibatch on, Minibatch size
                0.15, false, 10, 0.65, 30, true, true, 10);

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

        samples.add(new Sample(new double[]{1, 1}, new double[]{0}));
        samples.add(new Sample(new double[]{1, 0}, new double[]{1}));
        samples.add(new Sample(new double[]{0, 1}, new double[]{1}));
        samples.add(new Sample(new double[]{0, 0}, new double[]{0}));

        samples.add(new Sample(new double[]{1, 1}, new double[]{0}));
        samples.add(new Sample(new double[]{1, 0}, new double[]{1}));
        samples.add(new Sample(new double[]{0, 1}, new double[]{1}));
        samples.add(new Sample(new double[]{0, 0}, new double[]{0}));

        //    Num Inputs,  Num Hidden,  Num Outputs, Num Learning steps, Show Graph, Output image name
        MLP mlp = new MLP(2, 2, 1, 3000, false, "1",
                //Learning rate, Use Glorot & Bengio weight init? ,  Print status frequency, Momentum influence, Frequency of decreasing learning rate,Is dropout on, Is minibatch on, Minibatch size
                0.3, false, 10, 0.5, 30, false, true, 10);

        mlp.training(samples);

        System.out.println("Vstup: [1,0] Výstup: " + mlp.feedForward(new double[]{1, 0}, true)[0]);
        System.out.println("Vstup: [1,1] Výstup: " + mlp.feedForward(new double[]{1, 1}, true)[0]);
        System.out.println("Vstup: [0,1] Výstup: " + mlp.feedForward(new double[]{0, 1}, true)[0]);
        System.out.println("Vstup: [0,0] Výstup: " + mlp.feedForward(new double[]{0, 0}, true)[0]);
    }
}
