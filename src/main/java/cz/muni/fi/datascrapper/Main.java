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
import java.util.*;

/**
 * Created by Rex on 8.11.2016.
 */
public class Main{

    private static volatile int buffered = 5000;
    private static volatile int processed = 0;

    private static Object writeLock = new Object();

    private static String baseActorUrl = "http://www.imdb.com/name/[id]/";
    private static String baseMovieUrl = "http://www.imdb.com/title/[id]/";
    private static String detailMovieUrl = "http://www.imdb.com/title/[id]/fullcredits";

    private static List<Movie> processedMovies = Collections.synchronizedList(new ArrayList<>());

    private static String[] actorLists = new String[]{
            "http://www.imdb.com/list/ls058011111/?start=1&view=compact&sort=listorian:asc",
            "http://www.imdb.com/list/ls058011111/?start=251&view=compact&sort=listorian:asc",
            "http://www.imdb.com/list/ls058011111/?start=501&view=compact&sort=listorian:asc",
            "http://www.imdb.com/list/ls058011111/?start=751&view=compact&sort=listorian:asc"
    };

    private static List<Person> baseActors;
    private static Set<Person> directors;
    private static List<Movie> movies = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        //*******************
        //loading actors
        //*******************

        baseActors = loadOrParseActors();
        System.out.println(baseActors.size());

        //loadMoviesForActors();

        //*******************
        //loading movie rating and year
        //*******************

        movies = loadPreProcessedMovies();
        //System.out.println(movies.size());

        //Collections.sort(movies, (o1, o2) -> o1.getActors().size() - o2.getActors().size());

        List<Movie> toBeDetailed = new ArrayList<>();
        for (Movie movie : movies) {
            if(movie.getActors().size() >= 2 && movie.getActors().size() < 10)
                toBeDetailed.add(movie);
            //System.out.println(movie.getName() + " " + movie.getActors().size());
        }

        System.out.println("Going to process " + toBeDetailed.size() + " movies");

        loadMovieDetails1(toBeDetailed);

        //*******************
        //loading movie director
        //*******************
        //movies = DataTools.getMoviesFromJson();
        //loadMovieDetails2(movies);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static List<Movie> loadPreProcessedMovies() throws IOException {
        return new Gson().fromJson(new JsonReader(new FileReader("data/movies_preprocessed.json")), new TypeToken<ArrayList<Movie>>(){}.getType());
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
                        Files.write(Paths.get("data/movies_preprocessed.json"), moviesJson.getBytes());
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
        if(Files.exists(Paths.get("data/actors.json"))) {
            return new Gson().fromJson(new JsonReader(new FileReader("data/actors.json")), new TypeToken<ArrayList<Person>>(){}.getType());
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
        Files.write(Paths.get("data/actors.json"), actorsJson.getBytes());

        return parsedActors;
    }


    private static void loadMovieDetails1(List<Movie> toBeDetailed) {
        buffered = 20;
        processed = 0;

        directors = new HashSet<>();

        toBeDetailed.stream().parallel().forEach(movie -> {
            try {
                Person director = parseMovieDetails(movie);

                if(movie.getRating() < 0.1 || movie.getDirector() == null) {
                    return;
                }

                synchronized (writeLock) {
                    processedMovies.add(movie);

                    if(director != null) {
                        directors.add(director);
                    }

                    ++processed;

                    if (buffered-- == 0 || processed == toBeDetailed.size()) {
                        String moviesJson = new Gson().toJson(processedMovies);
                        Files.write(Paths.get("data/movies_2.json"), moviesJson.getBytes());

                        String directorsJson = new Gson().toJson(directors);
                        Files.write(Paths.get("data/directors_2.json"), directorsJson.getBytes());

                        buffered = 20;
                    }
                }

                System.out.println("Processed " + processed + " movies");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void loadMovieDetails2(List<Movie> moviesWODirectors) throws IOException {
        buffered = 20;
        processed = 0;

//        List<Movie> moviesWDirectors = DataTools.getMoviesWDirectorsFromJson();
//        for (Movie movie : moviesWODirectors) {
//            if(!moviesWDirectors.contains(movie)); {
//                moviesWDirectors.add(movie);
//            }
//        }
//
//        final List<Movie> movies = moviesWODirectors;
//
//        processed = movies.size();

        Set<Person> parsedDirectors = new HashSet<>(); //DataTools.getBaseDirectors()

        moviesWODirectors.stream().parallel().forEach(movie -> {
            if(movie.getDirector() != null && movie.getDirector().length() > 0) {
                return;
            }

            try {
                Person director = parseMovieDirector(movie);

                if(director == null || movie.getRating() < 0.1) {
                    return;
                }

                synchronized (writeLock) {
                    parsedDirectors.add(director);
                    processedMovies.add(movie);
                    ++processed;

                    if (buffered-- == 0 || processed == movies.size()) {
                        String moviesJson = new Gson().toJson(processedMovies);
                        Files.write(Paths.get("data/movies_w_directors.json"), moviesJson.getBytes());

                        String directorsJson = new Gson().toJson(parsedDirectors);
                        Files.write(Paths.get("data/directors.json"), directorsJson.getBytes());
                        buffered = 20;
                    }
                }

                System.out.println("Processed " + processed + " movies");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static Person parseMovieDetails(Movie movie) throws IOException {
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

        try {
            Element directorElem = doc.select(".credit_summary_item").first().select("a").first();
            String directorName = directorElem.text();
            String directorId = directorElem.attr("href").replaceAll(".*/name/(.*?)\\?.*", "$1");

            movie.setDirector(directorId);
            return new Person(directorId, directorName);
        } catch (Exception e) {
            System.err.println("Failed to extract director");
        }

        return null;
    }

    private static Person parseMovieDirector(Movie movie) throws IOException{
        Document doc = Jsoup.connect(detailMovieUrl.replace("[id]", movie.getId())).userAgent("Mozilla").timeout(10_000).get();

        try {
            Element directorElem = doc.select(".name").first().select("a").first();
            String directorName = directorElem.text();
            String directorId = directorElem.attr("href").replaceAll(".*/name/(.*?)/.*", "$1");

            Person director = new Person(directorId, directorName);

            movie.setDirector(directorId);


            return director;
        } catch (Exception e) {
            System.out.println("Failed to extract director for movie " + movie.getName());
        }

        return null;
    }
}
