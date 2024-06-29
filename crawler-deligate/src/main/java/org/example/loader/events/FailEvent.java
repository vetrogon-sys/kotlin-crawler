package org.example.loader.events;

import org.example.loader.Link;
import org.example.loader.Loader;

public class FailEvent extends LoaderEvent {

    private final Throwable cause;

    public FailEvent(Loader loader, Link link, String content, Throwable cause) {
        super(loader, link, content);
        this.cause = cause;
    }

    public Throwable cause() {
        return cause;
    }
}
