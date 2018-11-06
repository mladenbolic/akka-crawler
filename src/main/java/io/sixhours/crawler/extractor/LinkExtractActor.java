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
public class LinkExtractActor extends AbstractActor {

  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private final String baseUri;

  private final LinkExtractor linkExtractor;

  public static final String NAME = "url-extract";

  public static Props props(String baseUri, LinkExtractor linkExtractor) {
    return Props.create(LinkExtractActor.class, baseUri, linkExtractor);
  }

  @Value
  public static final class ExtractLinks {

    private final String url;
    private final String path;
  }

  @Value
  public static final class LinksExtracted {

    private final String url;
    private final String path;
    private final Set<String> newUrls;
  }

  private void onExtractUrls(ExtractLinks message) {
    String url = message.url;
    String path = message.path;

    ActorRef sender = getSender();
    ExecutionContext executionContext = getContext().system().dispatcher();

    Futures.future(() -> this.linkExtractor.extractUrls(url, path), executionContext)
        .onComplete(new OnComplete<LinkExtractResult>() {
          @Override
          public void onComplete(Throwable failure, LinkExtractResult result) {
            sender.tell(new LinksExtracted(url, path, result.getUrls()), getSelf());
          }
        }, executionContext);
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(ExtractLinks.class, this::onExtractUrls)
        .build();
  }
}
