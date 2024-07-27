package org.example.loader.events;

import org.example.loader.Link;

public class SuccessEvent extends LoaderEvent {

    public SuccessEvent(DataLoader loader, Link link, String content) {
        super(loader, link, content);
    }


}
