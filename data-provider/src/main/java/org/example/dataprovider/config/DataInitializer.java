package org.example.dataprovider.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.dataprovider.entity.Film;
import org.example.dataprovider.entity.Genre;
import org.example.dataprovider.repository.FilmRepository;
import org.example.dataprovider.repository.GenreRepository;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final FilmRepository filmRepository;
    private final GenreRepository genreRepository;


    static final List<String> GENRES_LIST = List.of("Comedy", "Drama", "Action", "Thriller",
          "Horror", "Romance", "Adventure", "Science Fiction", "Fantasy",
          "Mystery", "Crime", "Animation", "Family", "Documentary", "War");

    static final List<String> FILMS_LIST = List.of("Anchorman: The Legend of Ron Burgundy",
          "The Shawshank Redemption", "Die Hard", "Gone Girl", "The Conjuring",
          "The Notebook", "Indiana Jones and the Raiders of the Lost Ark", "Blade Runner",
          "The Lord of the Rings: The Fellowship of the Ring", "Memento", "Pulp Fiction",
          "Toy Story", "Finding Nemo", "March of the Penguins", "Saving Private Ryan");

    @PostConstruct
    public void init() {
        List<Genre> genres = new ArrayList<>();
        for (String genre : GENRES_LIST) {
            Genre genreEntity = new Genre();
            genreEntity.setName(genre);

            genres.add(genreRepository.save(genreEntity));
        }


        Random random = new Random();
        for (String filmName : FILMS_LIST) {
            Film film = new Film();
            film.setName(filmName);
            Genre genre = genres.get(random.nextInt(0, genres.size() - 1));
            film.setGenre(genre);
            genre.addFilm(film);

            filmRepository.save(film);
        }

        genreRepository.saveAll(genres);


    }


}
