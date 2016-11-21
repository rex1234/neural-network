package cz.muni.fi.datascrapper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import cz.muni.fi.datascrapper.model.Movie;
import cz.muni.fi.datascrapper.model.Person;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Rex on 8.11.2016.
 */
public class Main{

    private static volatile int buffered = 5000;
    private static volatile int processed = 0;

    private static Object writeLock = new Object();

    private static String baseActorUrl = "http://www.imdb.com/name/[id]/";
    private static String baseMovieUrl = "http://www.imdb.com/title/[id]/";


    private static String[] actorLists = new String[]{
            "http://www.imdb.com/list/ls058011111/?start=1&view=compact&sort=listorian:asc",
            "http://www.imdb.com/list/ls058011111/?start=251&view=compact&sort=listorian:asc",
            "http://www.imdb.com/list/ls058011111/?start=501&view=compact&sort=listorian:asc",
            "http://www.imdb.com/list/ls058011111/?start=751&view=compact&sort=listorian:asc"
    };

    private static List<Person> baseActors;
    private static List<Movie> movies = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        baseActors = loadOrParseActors();
        System.out.println(baseActors.size());

        //loadMoviesForActors();
        movies = loadPreProcessedMovies();
        System.out.println(movies.size());

        //Collections.sort(movies, (o1, o2) -> o1.getActors().size() - o2.getActors().size());

        List<Movie> toBeDetailed = new ArrayList<>();
        for (Movie movie : movies) {
            if(movie.getActors().size() >= 4 && movie.getActors().size() < 10)
                toBeDetailed.add(movie);
            //System.out.println(movie.getName() + " " + movie.getActors().size());
        }

        System.out.println("Going to process " + toBeDetailed.size() + " movies");

        loadMovieDetails(toBeDetailed);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static List<Movie> loadPreProcessedMovies() throws IOException {
        return new Gson().fromJson(new JsonReader(new FileReader("movies_preprocessed.json")), new TypeToken<ArrayList<Movie>>(){}.getType());
    }

    private static void loadMoviesForActors() {
        baseActors.stream().parallel().forEach(actor -> {
            try {
                Document doc = Jsoup.connect(baseActorUrl.replace("[id]", actor.getId())).userAgent("Mozilla").timeout(10_000).get();
                Elements movieElements = doc.select("div.filmo-row");
                for (Element movieElement : movieElements) {
                    Element movieRow = movieElement.select("a").first();
                    String id = movieRow.attr("href").replaceAll("/title/(.*?)/.*", "$1");
                    String name = movieRow.text();

                    Movie movie = new Movie(id, name);

                    int i = movies.indexOf(movie);

                    if (i == -1) {
                        movies.add(movie);
                        movie.getActors().add(actor.getId());
                    } else {
                        movie = movies.get(i);
                        if (!movie.getActors().contains(actor.getId())) {
                            movie.getActors().add(actor.getId());
                        }
                    }
                }

                System.out.println("Processed " + ++processed + " actors");

                if (buffered-- == 0 || processed == baseActors.size()) {
                    synchronized (writeLock) {
                        String moviesJson = new Gson().toJson(movies);
                        Files.write(Paths.get("movies_preprocessed.json"), moviesJson.getBytes());
                        buffered = 5000;
                    }
                }

                System.out.println("Added movies from actor " + actor.getName());
            } catch (IOException e) {
                System.err.println("Failed to load movies for actor " + actor.getName());
                e.printStackTrace();
            }
        });
    }

    private static List<Person> loadOrParseActors() throws IOException {
        if(Files.exists(Paths.get("actors.json"))) {
            return new Gson().fromJson(new JsonReader(new FileReader("actors.json")), new TypeToken<ArrayList<Person>>(){}.getType());
        } else {
            return parseActorsFromImdb();
        }
    }

    private static List<Person> parseActorsFromImdb() throws IOException {
        List<Person> parsedActors = new ArrayList<>();

        for (String actorList : actorLists) {
            Document doc = Jsoup.connect(actorList).userAgent("Mozilla").timeout(10_000).get();
            Elements actorElements = doc.select("tr.list_item.compact");
            for (Element element : actorElements) {
                Element row = element.select("td.name").first().select("a").first();
                String name = row.text();
                String id = row.attr("href").replace("/name/", "");

                Person actor = new Person(id, name);
                parsedActors.add(actor);
            }

            System.err.println("Processed " + parsedActors.size() + " actors");
        }

        String actorsJson = new Gson().toJson(parsedActors);
        Files.write(Paths.get("actors.json"), actorsJson.getBytes());

        return parsedActors;
    }


    private static List<Movie> processedMovies = Collections.synchronizedList(new ArrayList<Movie>());
    private static void loadMovieDetails(List<Movie> toBeDetailed) {
        buffered = 20;
        processed = 0;

        toBeDetailed.stream().parallel().forEach(movie -> {
            try {
                parseMovieDetails(movie);
                System.out.printf("%s - %.1f %d%n", movie.getName(), movie.getRating(), movie.getYear());

                synchronized (writeLock) {
                    processedMovies.add(movie);
                    ++processed;

                    if (buffered-- == 0 || processed == toBeDetailed.size()) {
                        String moviesJson = new Gson().toJson(processedMovies);
                        Files.write(Paths.get("movies.json"), moviesJson.getBytes());
                        buffered = 20;
                    }
                }

                System.out.println("Processed " + processed + " movies");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void parseMovieDetails(Movie movie) throws IOException {
        Document doc = Jsoup.connect(baseMovieUrl.replace("[id]", movie.getId())).userAgent("Mozilla").timeout(10_000).get();

        try {
            float rating = Float.parseFloat(doc.select("div.ratingvalue").first().select("span").first().text());
            movie.setRating(rating);
        } catch (Exception e) {
            System.err.println("Failed to extract rating");
        }

        try {
            int year = Integer.parseInt(doc.title().replaceAll(".*\\(.*?(\\d+)\\) - IMDb", "$1"));
            movie.setYear(year);
        } catch (Exception e) {
            System.err.println("Failed to extract year");
        }
    }
}
