package io.sixhours.crawler;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.sixhours.crawler.downloader.FileDownloadActor;
import io.sixhours.crawler.downloader.FileDownloadActor.DownloadFile;
import io.sixhours.crawler.downloader.FileDownloadActor.FileDownloadError;
import io.sixhours.crawler.downloader.FileDownloadResult;
import io.sixhours.crawler.downloader.FileDownloaderImpl;
import io.sixhours.crawler.extractor.UrlExtractActor;
import io.sixhours.crawler.extractor.UrlExtractActor.ExtractUrls;
import io.sixhours.crawler.extractor.UrlExtractActor.UrlsExtracted;
import io.sixhours.crawler.extractor.UrlExtractorImpl;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@SuppressWarnings("PMD.UnusedFormalParameter")
@RequiredArgsConstructor
public class CrawlSupervisor extends AbstractActor {

  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private final CrawlStatus crawlStatus = new CrawlStatus();

  private final String baseUri;

  public static final String NAME = "crawler-supervisor";

  public static Props props(String baseUri) {
    return Props.create(CrawlSupervisor.class, baseUri);
  }

  @Value
  public static class StartCrawling {

    private String url;
  }

  @Value
  public static class CrawlFinished {

  }

  @Override
  public void preStart() {
    log.info("Crawler started");
  }

  @Override
  public void postStop() {
    log.info("Crawler stopped");
  }

  private void onCrawlFinished(CrawlFinished message) {
    log.info("============================================================");
    log.info(crawlStatus.toString());
    log.info("============================================================\n");
    crawlStatus.getFailed()
        .forEach(log::info);

    getContext().system().terminate();
  }

  private void onStartCrawling(StartCrawling message) {
    String url = message.url;
    crawlStatus.add(url);

    ActorRef fileDownloaderActor = getContext()
        .actorOf(FileDownloadActor.props(new FileDownloaderImpl(System.getProperty("user.home")+"/Downloads/")),
            FileDownloadActor.name(String.valueOf(UUID.randomUUID())));

    fileDownloaderActor.tell(new DownloadFile(crawlStatus.getNext()), getSelf());
  }

  private void onFileDownloadResult(FileDownloadResult message) {
    ActorRef urlExtractor = getContext()
        .actorOf(UrlExtractActor.props(new UrlExtractorImpl()),
            UrlExtractActor.NAME + UUID.randomUUID());

    urlExtractor.tell(
        new ExtractUrls(message.getUrl(), message.getPath(), this.baseUri),
        getSelf());
  }

  private void onFileDownloadError(FileDownloadError message) {
    crawlStatus.addFailed(message.getUrl());
  }

  private void onUrlsExtracted(UrlsExtracted message) {
    crawlStatus.addAll(message.getUrls());
    crawlStatus.addProcessed(message.getUrl());

    log.info(crawlStatus.toString());
    if (crawlStatus.isFinished()) {
      getSelf().tell(new CrawlFinished(), ActorRef.noSender());
    } else {
      crawlStatus.getPagesToVisit()
          .forEach(url -> getSelf().tell(new StartCrawling(url), ActorRef.noSender()));
    }
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(StartCrawling.class, this::onStartCrawling)
        .match(CrawlFinished.class, this::onCrawlFinished)
        .match(FileDownloadResult.class, this::onFileDownloadResult)
        .match(FileDownloadError.class, this::onFileDownloadError)
        .match(UrlsExtracted.class, this::onUrlsExtracted)
        .build();
  }
}