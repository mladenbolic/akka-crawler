package io.sixhours.crawler.extractor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import scala.concurrent.ExecutionContext;

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

  private void onExtractUrls(ExtractUrls message) {
    String url = message.url;
    String path = message.path;

    ActorRef sender = getSender();
    ExecutionContext executionContext = getContext().system().dispatcher();

    Futures.future(() -> this.urlExtractor.extractUrls(url, path), executionContext)
        .onComplete(new OnComplete<UrlExtractResult>() {
          @Override
          public void onComplete(Throwable failure, UrlExtractResult result) {
            sender.tell(new UrlsExtracted(url, path, result.getUrls()), getSelf());
          }
        }, executionContext);
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(ExtractUrls.class, this::onExtractUrls)
        .build();
  }
}
