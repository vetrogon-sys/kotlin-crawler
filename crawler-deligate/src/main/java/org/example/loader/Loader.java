package org.example.loader;

import org.apache.commons.lang3.StringUtils;
import org.example.loader.events.AbstractDataLoader;
import org.example.loader.events.FailEvent;
import org.example.loader.events.LoaderTask;
import org.example.loader.events.SuccessEvent;
import org.example.model.ModelConstants;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.slf4j.LoggerFactory.getLogger;

public class Loader extends AbstractDataLoader {
    private static final Logger log = getLogger(Loader.class);


    public Loader(long pauseRequest, int limitRequest) {
        super(pauseRequest, limitRequest);
    }

    @Override
    public boolean runTask(LoaderTask loaderTask) {
        Link link = loaderTask.getLink();
        initLoader(link);

        CompletableFuture.runAsync(() -> {
            try {
                URL taskUrl = new URI(link.buildUrl()).toURL();
                HttpURLConnection urlConnection = (HttpURLConnection) taskUrl.openConnection();

                initConnection(urlConnection, link);
                urlConnection.setRequestMethod(link.getMethod());

                if (link.getBody() != null) {
                    urlConnection.setDoOutput(true);
                    try (OutputStream os = urlConnection.getOutputStream();
                         OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                        osw.write(link.getBody());
                        osw.flush();
                    }
                }

                logLink(link);
                int responseCode = urlConnection.getResponseCode();
                if (is2xxCode(responseCode)) {
                    handleConnection(urlConnection);
                    InputStream response = urlConnection.getInputStream();

                    SuccessEvent successEvent = new SuccessEvent(this, link, new String(response.readAllBytes(), StandardCharsets.UTF_8));
                    loaderTask.complete(successEvent);
                } else if (is4xxCode(responseCode)) {
                    InputStream response = urlConnection.getErrorStream();

                    FailEvent successEvent = new FailEvent(this, link, new String(response.readAllBytes()), getExceptionForStatus(responseCode));
                    loaderTask.complete(successEvent);
                } else {
                    String responseMessage = urlConnection.getResponseMessage();

                    FailEvent successEvent = new FailEvent(this, link, responseMessage, getExceptionForStatus(responseCode));
                    loaderTask.complete(successEvent);
                }

            } catch (IOException | URISyntaxException e) {
                loaderTask.complete(new FailEvent(this, link, e.getMessage(), e));
            }
        }, loaderExecutor);

        return true;
    }

    protected void handleConnection(HttpURLConnection connection) {
        connection.getHeaderFields().forEach((s, values) -> {
            if (StringUtils.equalsIgnoreCase(s, ModelConstants.SET_COOKIE_HEADER)) {
                for (String cookieString : values) {
                    String[] split = cookieString.split("=");
                    String key = split[0];
                    String value = split[1];
                    if (value.endsWith(";")) {
                        value = value.substring(0, value.length() - 1);
                    }
                    this.cookies.put(key, new HttpCookie(key, value));
                }
            }
        });
    }

}
