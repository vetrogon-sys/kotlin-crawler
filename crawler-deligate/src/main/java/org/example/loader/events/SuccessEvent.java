package org.example.loader.events;

import org.example.loader.Link;
import org.example.loader.Loader;

import java.io.InputStream;

public class SuccessEvent extends LoaderEvent {

    public SuccessEvent(Loader loader, Link link, String content) {
        super(loader, link, content);
    }


}
