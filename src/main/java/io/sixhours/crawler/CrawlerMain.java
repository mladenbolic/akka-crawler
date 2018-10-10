package io.sixhours.crawler;

import akka.actor.ActorRef;
import akka.actor.ActorRefFactory;
import akka.actor.ActorSystem;
import io.sixhours.crawler.CrawlSupervisor.StartCrawling;
import io.sixhours.crawler.downloader.FileDownloadActor;
import io.sixhours.crawler.downloader.FileDownloaderImpl;
import io.sixhours.crawler.extractor.UrlExtractActor;
import io.sixhours.crawler.extractor.UrlExtractorImpl;
import java.util.UUID;
import java.util.function.Function;

@SuppressWarnings("PMD.UseUtilityClass")
public class CrawlerMain {

  public static void main(String[] args) {
    ActorSystem system = ActorSystem.create("crawler-system");

    Function<ActorRefFactory, ActorRef> fileDownloadCreator = actorRefFactory -> actorRefFactory
        .actorOf(FileDownloadActor.props(
            new FileDownloaderImpl(System.getProperty("user.home") + "/Downloads/Akka-Crawler")),
            FileDownloadActor.name(String.valueOf(UUID.randomUUID())));

    Function<ActorRefFactory, ActorRef> urlExtractorCreator = actorRefFactory -> actorRefFactory
        .actorOf(UrlExtractActor.props(new UrlExtractorImpl()),
            UrlExtractActor.NAME + UUID.randomUUID());

    ActorRef crawlSupervisor = system
        .actorOf(CrawlSupervisor
                .props("http://www.burgerking.no/", new CrawlStatusImpl(), fileDownloadCreator,
                    urlExtractorCreator),
            CrawlSupervisor.NAME);

    crawlSupervisor.tell(new StartCrawling("http://www.burgerking.no/"), ActorRef.noSender());
  }
}