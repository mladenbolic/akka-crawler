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
   * @param baseUri websites base URL (e.g. http://google.com)
   * @param filePath the path to the downloaded HTML page
   * @return url extract result containing list of found urls/links
   */
  UrlExtractResult extractUrls(String baseUri, String filePath) throws UrlExtractException;
}
