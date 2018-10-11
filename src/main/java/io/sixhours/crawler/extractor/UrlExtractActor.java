package io.sixhours.crawler.extractor;

import akka.actor.AbstractActor;
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

  private final String baseUri;

  private final UrlExtractor urlExtractor;

  public static final String NAME_PREFIX = "url-extract-%s";

  public static Props props(String baseUri, UrlExtractor urlExtractor) {
    return Props.create(UrlExtractActor.class, baseUri, urlExtractor);
  }

  public static String name(String suffix) {
    return String.format(NAME_PREFIX, suffix);
  }

  @Value
  public static final class ExtractUrls {

    private final String url;
    private final String path;
  }

  @Value
  public static final class UrlsExtracted {

    private final String url;
    private final String path;
    private final Set<String> newUrls;
  }

  @Value
  public static final class UrlExtractError {

    private final String url;
  }

  private void onExtractUrls(ExtractUrls message) {
    String url = message.url;
    String path = message.path;

    UrlExtractResult result = this.urlExtractor.extractUrls(this.baseUri, path);

    getSender().tell(new UrlsExtracted(url, path, result.getUrls()), getSelf());
    getContext().stop(getSelf());
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(ExtractUrls.class, this::onExtractUrls)
        .build();
  }
}
