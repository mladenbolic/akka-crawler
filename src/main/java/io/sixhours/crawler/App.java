package io.sixhours.crawler;

import io.sixhours.crawler.crawler.AkkaCrawlerImpl;
import io.sixhours.crawler.crawler.Crawler;
import io.sixhours.crawler.utils.ArgsHandler;

/**
 * Application for crawling web sites and saving to disk online file structure.
 *
 * @author Mladen Bolic
 */
@SuppressWarnings("PMD.UseUtilityClass")
public class App {

  /**
   * Takes url for crawling as argument and starts crawling.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    String url = ArgsHandler.getUrl(args);
    Crawler crawler = new AkkaCrawlerImpl();
    crawler.crawl(url);
  }
}