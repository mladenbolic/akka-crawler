package io.sixhours.crawler.extractor;

import akka.actor.AbstractActor;
import akka.actor.Actor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * Actor for handling link extractions from specified url.
 *
 * @author Mladen Bolic
 */
@RequiredArgsConstructor
public class UrlExtractActor extends AbstractActor {

  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private final UrlExtractor urlExtractor;

  public static final String NAME = "url-extract";

  public static Props props(UrlExtractor urlExtractor) {
    return Props.create(UrlExtractActor.class, urlExtractor);
  }

  @Value
  public static final class ExtractUrls {

    private final String url;
    private final String path;
    private final String baseUri;
  }

  @Value
  public static final class UrlsExtracted {

    private final String url;
    private final String path;
    private final Set<String> urls;
  }

  @Value
  public static final class UrlExtractError {

    private final String url;
  }

  private void onExtractUrls(ExtractUrls message) {
    String url = message.url;
    String path = message.path;
    String baseUri = message.baseUri;

    UrlExtractResult result;
    try {
      result = this.urlExtractor.extractUrls(baseUri, path);

      getSender().tell(new UrlsExtracted(url, path, result.getUrls()), getSelf());
      getContext().stop(getSelf());
    } catch (UrlExtractException e) {
      getSender().tell(new UrlExtractError(url), Actor.noSender());
    }
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(ExtractUrls.class, this::onExtractUrls)
        .build();
  }
}
