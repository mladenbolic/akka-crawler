package io.sixhours.crawler;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.sixhours.crawler.CrawlSupervisor.StartCrawling;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.jsoup.Jsoup;

/**
 * @author
 */
public class UrlExtractor extends AbstractActor {

  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  public static final String NAME = "url-extractor";

  public static Props props() {
    return Props.create(UrlExtractor.class);
  }

  @RequiredArgsConstructor
  public static final class ExtractUrls {

    private final long requestId;
    private final String url;
    private final String baseUri;
  }

  @RequiredArgsConstructor
  public static final class UrlsExtracted {

    final List<String> urls;
  }

  private void onExtractUrls(ExtractUrls message) {
    String url = message.url;
    String baseUri = message.baseUri;
    File file = new File(url);

    this.readFileContent(file, baseUri);
  }

  private void onUrlsExtracted(UrlsExtracted message) {
  }

  private void readFileContent(File file, String baseUri) {
    LineIterator it = null;
    List<String> urls = new ArrayList<>();
//    String currentHost = "http://www.burgerking.no/";

    try {
      it = FileUtils.lineIterator(file, StandardCharsets.UTF_8.name());
      while (it.hasNext()) {
        String line = it.nextLine();

        urls.addAll(
            getLinks(line, baseUri, "a[href]", "href")
                .filter(link -> !link.contains("mailto"))
                .collect(Collectors.toList())
        );

        urls.addAll(
            getLinks(line, baseUri, "script[src]", "src")
                .collect(Collectors.toList())
        );

        urls.addAll(
            getLinks(line, baseUri, "img[src]", "src")
                .collect(Collectors.toList())
        );

        urls.addAll(
            getLinks(line, baseUri, "link[href]", "href")
                .collect(Collectors.toList())
        );
      }

      for (String u : urls) {
        log.info("Url: {}", u);
        getSender().tell(new StartCrawling(u), getSelf());
      }

      // TODO 1): tell

    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      // getSender().tell(new UrlsExtracted(requestId, urls), getSender());
      LineIterator.closeQuietly(it);
    }
  }

  private Stream<String> getLinks(String content, String baseUri, String cssQuery,
      String attributeKey) {

    return Jsoup.parseBodyFragment(content, baseUri)
        .select(cssQuery).stream()
        .map(element -> element.absUrl(attributeKey))
        .filter(link -> Objects.nonNull(link) && link.length() > 0)
        .filter(link -> {
          try {
            return new URL(baseUri).getHost().equals(new URL(link).getHost());
          } catch (MalformedURLException e) {
            e.printStackTrace();
          }
          return false;
        });
  }


  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(ExtractUrls.class, this::onExtractUrls)
        .match(UrlsExtracted.class, this::onUrlsExtracted)
        .build();
  }
}
