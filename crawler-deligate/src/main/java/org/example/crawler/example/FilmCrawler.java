package org.example.crawler.example;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.crawler.CrawlerSettings;
import org.example.crawler.CrawlerUtils;
import org.example.crawler.DataCrawler;
import org.example.loader.Link;
import org.example.loader.events.SuccessEvent;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;

@CrawlerSettings
public class FilmCrawler extends DataCrawler {
    private static final Logger log = getLogger(FilmCrawler.class);

    final Map<String, Film> films = new ConcurrentHashMap<>();

    @Override
    public Link createStartLink() {
        return new Link.Builder("http://127.0.0.1:8080/films").build();
    }

    @Override
    public void handleSuccessStart(SuccessEvent filmSe) {
        for (JsonNode filmNode : filmSe.json().at("/_embedded/films")) {
            String filmId = filmNode.at("/id").asText();
            String film = filmNode.at("/name").asText();

            Link filmsLink = new Link.Builder("http://127.0.0.1:8080/films/%s/genre".formatted(filmId)).build();
            filmSe.addTask(filmsLink)
                  .onSuccess(filmsSe -> handleGenres(filmsSe, film, filmId));
        }
    }

    private void handleGenres(SuccessEvent filmsSe, String film, String filmId) {
        JsonNode genreNode = filmsSe.json();
        String genreId = genreNode.at("/id").asText();
        String genre = genreNode.at("/name").asText();

        films.put(genreId, new Film(filmId, film, genre, genreId));
    }

    @Override
    public void onComplete() {
        super.onComplete();

        StringBuilder response = new StringBuilder("[");
        films.values().forEach(response::append);
        response.append("]");

        log.info(response.toString());
    }

    record Film(String id, String name, String genre, String genreId) {
        @Override
        public String toString() {
            return """
                  {
                    "id": "%s",
                    "name": "%s",
                    "genre": "%s",
                    "genreId": "%s"
                  }
                  """.formatted(id, name, genre, genreId);
        }
    }

    public static void main(String[] args) {
        CrawlerUtils.runCrawler(FilmCrawler.class);
    }
}
