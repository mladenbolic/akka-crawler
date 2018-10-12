package io.sixhours.crawler.extractor;

/**
 * Interfaces for extracting urls from a file.
 *
 * @author Mladen Bolic
 */
public interface UrlExtractor {

  /**
   * Extracts links from given {@code filePath}.
   *
   * @param url page url from which we are extracting urls (e.g. http://google.com/users)
   * @param filePath the path to the downloaded HTML page
   * @return url extract result containing list of found urls/links
   */
  UrlExtractResult extractUrls(String url, String filePath) throws UrlExtractException;
}
