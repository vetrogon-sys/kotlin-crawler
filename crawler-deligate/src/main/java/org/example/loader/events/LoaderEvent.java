package org.example.loader.events;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.loader.Link;
import org.example.loader.Loader;
import org.example.util.JsonUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public abstract class LoaderEvent {

    protected final Loader loader;
    protected final Link link;
    protected final String content;

    protected LoaderEvent(Loader loader, Link link, String content) {
        this.loader = loader;
        this.content = content;
        this.link = link;
    }

    public LoaderTask addTask(Link link) {
        return loader.addTask(link);
    }

    public Loader loader() {
        return loader;
    }

    public String content() {
        return content;
    }

    public JsonNode json() {
        return JsonUtils.parseJson(content);
    }

    public Document html() {
        return Jsoup.parse(content);
    }

}
