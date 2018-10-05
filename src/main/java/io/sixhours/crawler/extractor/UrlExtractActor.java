package io.sixhours.crawler.extractor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * @author
 */
@RequiredArgsConstructor
public class UrlExtractActor extends AbstractActor {

  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private final UrlExtractor urlExtractor;

  public static final String NAME = "url-extractor";

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

  private void onExtractUrls(ExtractUrls message) {
    String url = message.url;
    String path = message.path;
    String baseUri = message.baseUri;

    UrlExtractResult result = this.urlExtractor.extractUrls(baseUri, path);
    getSender().tell(new UrlsExtracted(url, path, result.getUrls()), getSelf());
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(ExtractUrls.class, this::onExtractUrls)
        .build();
  }
}
