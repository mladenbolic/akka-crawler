package io.sixhours.crawler.crawler;

import akka.actor.ActorRef;
import akka.actor.ActorRefFactory;
import akka.actor.ActorSystem;
import io.sixhours.crawler.downloader.FileDownloadActor;
import io.sixhours.crawler.downloader.FileDownloaderImpl;
import io.sixhours.crawler.extractor.UrlExtractActor;
import io.sixhours.crawler.extractor.UrlExtractorImpl;
import io.sixhours.crawler.supervisor.CrawlStatus;
import io.sixhours.crawler.supervisor.CrawlSupervisor;
import io.sixhours.crawler.supervisor.CrawlSupervisor.StartCrawling;
import java.util.UUID;
import java.util.function.Function;

/**
 * Class for crawling the websites.
 *
 * <p>It uses Akka actors in order to asynchronously crawl the website content.
 *
 * @author Mladen Bolic
 */
public class AkkaCrawlerImpl implements Crawler {

  private static final String DOWNLOAD_DIR =
      System.getProperty("user.home") + "/Downloads/Akka-Crawler";

  @Override
  public void crawl(String url) {
    ActorSystem system = ActorSystem.create("crawler-system");

    ActorRef crawlSupervisor = system
        .actorOf(CrawlSupervisor.props(
            new CrawlStatus(),
            getFileDownloadActorCreator(),
            getUrlExtractActorCreator(url)
            ),
            CrawlSupervisor.NAME);

    crawlSupervisor.tell(new StartCrawling(url), ActorRef.noSender());
  }

  private Function<ActorRefFactory, ActorRef> getFileDownloadActorCreator() {
    return actorRefFactory -> actorRefFactory
        .actorOf(FileDownloadActor.props(
            new FileDownloaderImpl(DOWNLOAD_DIR)),
            FileDownloadActor.name(String.valueOf(UUID.randomUUID())));
  }

  private Function<ActorRefFactory, ActorRef> getUrlExtractActorCreator(String domain) {
    return actorRefFactory -> actorRefFactory
        .actorOf(UrlExtractActor.props(domain, new UrlExtractorImpl()),
            UrlExtractActor.name(String.valueOf(UUID.randomUUID())));
  }
}
