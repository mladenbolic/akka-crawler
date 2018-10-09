package io.sixhours.crawler.extractor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.jsoup.Jsoup;

/**
 * Class for extracting links from downloaded file.
 *
 * @author Mladen Bolic
 */
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public class UrlExtractorImpl implements UrlExtractor {

  @Override
  public UrlExtractResult extractUrls(String baseUri, String filePath) throws UrlExtractException {
    File file = new File(filePath);
    LineIterator it = null;
    Set<String> urls = new HashSet<>();

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
    } catch (IOException e) {
      e.printStackTrace();
      // TODO: send event that urls could not be extracted
      throw new UrlExtractException(e.getMessage(), e);
    } finally {
      // getSender().tell(new UrlsExtracted(requestId, urls), getSender());
      LineIterator.closeQuietly(it);
    }
    return new UrlExtractResult(urls);
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
}
