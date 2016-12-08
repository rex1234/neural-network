package cz.muni.fi.datascrapper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import cz.muni.fi.datascrapper.model.Movie;
import cz.muni.fi.datascrapper.model.Person;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rex on 21.11.2016.
 */
public class DataTools {

    public static List<Person> getBaseActors() throws IOException {
        return new Gson().fromJson(new JsonReader(new FileReader("data/actors.json")), new TypeToken<ArrayList<Person>>(){}.getType());
    }

    public static List<Person> getBaseDirectors() throws IOException{
        return new Gson().fromJson(new JsonReader(new FileReader("data/directors_2.json")), new TypeToken<ArrayList<Person>>(){}.getType());
    }

    public static List<Movie> getMoviesFromJson() throws IOException{
        return new Gson().fromJson(new JsonReader(new FileReader("data/movies_preprocessed.json")), new TypeToken<ArrayList<Movie>>(){}.getType());
    }

    public static List<Movie> getMoviesWDirectorsFromJson() throws IOException{
        return new Gson().fromJson(new JsonReader(new FileReader("data/movies_2.json")), new TypeToken<ArrayList<Movie>>(){}.getType());
    }

}
