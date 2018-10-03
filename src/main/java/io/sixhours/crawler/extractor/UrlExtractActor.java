package io.sixhours.crawler.extractor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.sixhours.crawler.CrawlSupervisor.StartCrawling;
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
    private final String baseUri;
  }

  @Value
  public static final class UrlsExtracted {

    private final Set<String> urls;
  }

  private void onExtractUrls(ExtractUrls message) {
    String url = message.url;
    String baseUri = message.baseUri;

    UrlExtractResult result = this.urlExtractor.extractUrls(baseUri, url);
    // result.getUrls().forEach(u -> getSender().tell(new StartCrawling(u), getSelf()));
    // or we can return as a result the list of extracted urls, they will be put inside of temp store
    // and then we can call this url, and start processing remaining urls
    getSender().tell(new UrlsExtracted(result.getUrls()), getSelf());
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(ExtractUrls.class, this::onExtractUrls)
        .build();
  }
}
