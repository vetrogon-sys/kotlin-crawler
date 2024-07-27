package org.example.crawler.example;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.crawler.CrawlerSettings;
import org.example.crawler.CrawlerUtils;
import org.example.crawler.DataCrawler;
import org.example.loader.Link;
import org.example.loader.Loader;
import org.example.loader.events.DataLoader;
import org.example.loader.events.SuccessEvent;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

@CrawlerSettings
public class FilmCrawler extends DataCrawler {
    private static final Logger log = getLogger(FilmCrawler.class);

    public FilmCrawler(DataLoader loader) {
        super(loader);
    }

    @Override
    public Link createStartLink() {
        return new Link.Builder("https://jsonplaceholder.typicode.com/posts").build();
    }

    @Override
    public void handleSuccessStart(SuccessEvent filmSe) {
        for (JsonNode node : filmSe.json()) {
            String nodeId = node.at("/id").asText();
            if (!isSuitable(nodeId)) {
                continue;
            }
            Link link = new Link.Builder("https://jsonplaceholder.typicode.com/todos/" + nodeId)
                  .build();

            filmSe.addTask(link)
                  .onSuccess(se -> se.addTask(new Link.Builder("https://jsonplaceholder.typicode.com/posts/" + se.json().at("/id").asText()).build())
                        .onSuccess(se1 -> log.info(se1.json().at("/title").asText()))
                  );
        }
    }

    public static void main(String[] args) {
        CrawlerUtils.runCrawler(FilmCrawler.class);
    }
}
