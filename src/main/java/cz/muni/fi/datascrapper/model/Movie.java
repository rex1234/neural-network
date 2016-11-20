package cz.muni.fi.datascrapper.model;

import java.util.List;

/**
 * Created by Rex on 21.11.2016.
 */
public class Movie {
    private String id;
    private float rating;
    private String name;
    private List<Person> actors;
    private Person director;
    private int year;

    public Movie(String id, float rating, List<Person> actors, Person director, int year) {
        this.id = id;
        this.rating = rating;
        this.actors = actors;
        this.director = director;
        this.year = year;
    }

    public Movie(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public List<Person> getActors() {
        return actors;
    }

    public void setActors(List<Person> actors) {
        this.actors = actors;
    }

    public Person getDirector() {
        return director;
    }

    public void setDirector(Person director) {
        this.director = director;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Movie movie = (Movie) o;

        return !(id != null ? !id.equals(movie.id) : movie.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public String getName() {
        return name;
    }
}
