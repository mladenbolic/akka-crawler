package io.sixhours.crawler.crawler;

import akka.actor.AbstractActor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorRefFactory;
import akka.actor.ActorSystem;
import akka.routing.RoundRobinPool;
import io.sixhours.crawler.downloader.FileDownloadActor;
import io.sixhours.crawler.downloader.FileDownloaderImpl;
import io.sixhours.crawler.extractor.LinkExtractActor;
import io.sixhours.crawler.extractor.LinkExtractorImpl;
import io.sixhours.crawler.supervisor.CrawlStatus;
import io.sixhours.crawler.supervisor.CrawlSupervisor;
import io.sixhours.crawler.supervisor.CrawlSupervisor.StartCrawling;
import java.io.File;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.commons.io.FileUtils;

/**
 * Class for crawling the websites.
 *
 * <p>It uses Akka actors in order to asynchronously crawl the website content.
 *
 * @author Mladen Bolic
 */
public class AkkaCrawlerImpl implements Crawler {

  private static final String DOWNLOAD_DIR =
      FileUtils.getUserDirectory() + File.separator + "Downloads" + File.separator + "Akka-Crawler"
          + File.separator;

  @Override
  public void crawl(String url) {
    ActorSystem system = ActorSystem.create("crawler-system");

    ActorRef crawlSupervisor = system
        .actorOf(CrawlSupervisor.props(
            new CrawlStatus(),
            getFileDownloadActorCreator(),
            getUrlExtractActorCreator(url),
            getTerminateCommand()
            ),
            CrawlSupervisor.NAME);

    crawlSupervisor.tell(new StartCrawling(url), ActorRef.noSender());
  }

  private Consumer<ActorContext> getTerminateCommand() {
    return context -> context.system().terminate();
  }

  private Function<ActorRefFactory, ActorRef> getFileDownloadActorCreator() {
    return actorRefFactory -> actorRefFactory.actorOf(new RoundRobinPool(50).props(
        FileDownloadActor.props(new FileDownloaderImpl(DOWNLOAD_DIR))));
  }

  private Function<ActorRefFactory, ActorRef> getUrlExtractActorCreator(String domain) {
    return actorRefFactory -> actorRefFactory.actorOf(new RoundRobinPool(50).props(
        LinkExtractActor.props(domain, new LinkExtractorImpl())));
  }
}
