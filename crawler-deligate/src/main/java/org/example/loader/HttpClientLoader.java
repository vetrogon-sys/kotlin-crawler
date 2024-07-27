package org.example.loader;

import org.apache.commons.lang3.StringUtils;
import org.example.loader.events.AbstractDataLoader;
import org.example.loader.events.FailEvent;
import org.example.loader.events.LoaderTask;
import org.example.loader.events.SuccessEvent;
import org.slf4j.Logger;

import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.slf4j.LoggerFactory.getLogger;

public class HttpClientLoader extends AbstractDataLoader {
    private static final Logger log = getLogger(HttpClientLoader.class);

    public HttpClientLoader(long pauseRequest, int limitRequest) {
        super(pauseRequest, limitRequest);
    }

    @Override
    public boolean runTask(LoaderTask loaderTask) {
        Link link = loaderTask.getLink();
        initLoader(link);

        try (HttpClient client = HttpClient.newBuilder().executor(loaderExecutor).build()) {
            HttpRequest request = getRequest(link);

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                  .whenComplete((response, throwable) -> {
                      if (throwable != null) {
                          loaderTask.complete(new FailEvent(this, link, throwable.getLocalizedMessage(), throwable));
                          return;
                      }
                      logLink(link);

                      int code = response.statusCode();

                      if (is2xxCode(code)) {
                          handleResponse(response);
                          loaderTask.complete(new SuccessEvent(this, link, response.body()));
                      } else {
                          loaderTask.complete(new FailEvent(this, link, response.body(), getExceptionForStatus(code)));
                      }

                  });
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
        }


        return false;
    }

    private void handleResponse(HttpResponse<String> response) {
        response.headers().map().entrySet().stream()
              .filter(e -> StringUtils.equalsIgnoreCase(e.getKey(), "set-cookie"))
              .forEach(e -> e.getValue().forEach(v -> cookies.put(e.getKey(), new HttpCookie(e.getKey(), v))));
    }

    private HttpRequest getRequest(Link link) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(link.buildUrl()));

        link.getHeaders()
              .forEach(request::header);

        if (StringUtils.isNoneBlank(link.getBody())) {
            request.method(link.getMethod(), HttpRequest.BodyPublishers.ofString(link.getBody()));
        } else {
            request.method(link.getMethod(), HttpRequest.BodyPublishers.noBody());
        }

        return request
              .build();
    }

}
