package io.sixhours.crawler;

import io.sixhours.crawler.crawler.AkkaCrawlerImpl;
import io.sixhours.crawler.crawler.Crawler;

@SuppressWarnings("PMD.UseUtilityClass")
public class App {

  public static void main(String[] args) {
    String url;
    if(args == null){
      url = "http://www.burgerking.no/";
    } else {
      url = args[0];
    }

    Crawler crawler = new AkkaCrawlerImpl();
    crawler.crawl("http://www.burgerking.no/");
  }
}