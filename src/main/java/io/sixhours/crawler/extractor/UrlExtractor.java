package io.sixhours.crawler.extractor;

/**
 * @author
 */
public interface UrlExtractor {
  UrlExtractResult extractUrls(String baseUrl, String filePath);
}
