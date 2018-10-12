package io.sixhours.crawler.extractor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
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
  public UrlExtractResult extractUrls(String url, String filePath) throws UrlExtractException {
    File file = new File(filePath);
    Set<String> urls = new HashSet<>();

    try (
        LineIterator it = FileUtils.lineIterator(file, StandardCharsets.UTF_8.name())
    ) {
      while (it.hasNext()) {
        String line = it.nextLine();

        urls.addAll(
            getLinks(line, url, "a[href]", "href")
                .filter(link -> !link.contains("mailto"))
                .collect(Collectors.toList())
        );

        urls.addAll(
            getLinks(line, url, "script[src]", "src")
                .collect(Collectors.toList())
        );

        urls.addAll(
            getLinks(line, url, "img[src]", "src")
                .collect(Collectors.toList())
        );

        urls.addAll(
            getLinks(line, url, "link[href]", "href")
                .collect(Collectors.toList())
        );
      }
    } catch (IOException e) {
      throw new UrlExtractException(e.getMessage(), e);
    }
    return new UrlExtractResult(urls);
  }

  private Stream<String> getLinks(String content, String url, String cssQuery,
      String attributeKey) {

    return Jsoup.parseBodyFragment(content, url)
        .select(cssQuery).stream()
        .map(element -> element.absUrl(attributeKey))
        .map(link -> link.contains("#") ? link.substring(0, link.indexOf("#")) : link)
        .filter(link -> link.length() > 0)
        .filter(link -> this.checkHost(url, link));
  }

  private boolean checkHost(String url, String link) {
    try {
      return new URL(url).getHost().equals(new URL(link).getHost());
    } catch (MalformedURLException e) {
      throw new UrlExtractException(e.getMessage(), e);
    }
  }
}
