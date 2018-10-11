package io.sixhours.crawler.crawler;

/**
 * Crawler used to crawl/download the website content.
 *
 * @author Mladen Bolic
 */
public interface Crawler {

  /**
   * Crawls specified url.
   *
   * <p>Given url will be base url (domain).
   *
   * @param url domain url to be parsed
   */
  void crawl(String url);
}
