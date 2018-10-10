package io.sixhours.crawler;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorRefFactory;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import io.sixhours.crawler.downloader.FileDownloadActor.DownloadFile;
import io.sixhours.crawler.downloader.FileDownloadActor.FileDownloadError;
import io.sixhours.crawler.downloader.FileDownloadActor.FileDownloadResult;
import io.sixhours.crawler.downloader.FileDownloadException;
import io.sixhours.crawler.extractor.UrlExtractActor.ExtractUrls;
import io.sixhours.crawler.extractor.UrlExtractActor.UrlsExtracted;
import io.sixhours.crawler.extractor.UrlExtractException;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@SuppressWarnings("PMD.UnusedFormalParameter")
@RequiredArgsConstructor
public class CrawlSupervisor extends AbstractActor {

  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private final String baseUri;

  private final CrawlStatus crawlStatus;

  private final Function<ActorRefFactory, ActorRef> fileDownloadCreator;

  private final Function<ActorRefFactory, ActorRef> urlExtractorCreator;

  public static final String NAME = "crawl-supervisor";

  public static Props props(String baseUri, CrawlStatus crawlStatus,
      Function<ActorRefFactory, ActorRef> fileDownloadCreator,
      Function<ActorRefFactory, ActorRef> urlExtractorCreator) {
    return Props.create(CrawlSupervisor.class, baseUri, crawlStatus, fileDownloadCreator,
        urlExtractorCreator);
  }

  @Value
  public static class StartCrawling {

    private String url;
  }

  @Value
  public static class CrawlFinished {

  }

  private SupervisorStrategy strategy = new OneForOneStrategy(false,
      DeciderBuilder.
          match(FileDownloadException.class, e -> {
            log.warning("Evaluation of a top level expression failed, restarting.");
            return SupervisorStrategy.stop();
          }).
          match(UrlExtractException.class, e -> {
            log.error("Evaluation failed because of: {}", e.getMessage());
            return SupervisorStrategy.stop();
          }).
          match(Throwable.class, e -> {
            log.error("Unexpected failure: {}", e.getMessage());
            return SupervisorStrategy.stop();
          }).build());

  @Override
  public SupervisorStrategy supervisorStrategy() {
    return strategy;
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

//     getContext().system().terminate();
  }

  private void onStartCrawling(StartCrawling message) {
    String url = message.url;
    crawlStatus.add(url);

    ActorRef fileDownloaderActor = fileDownloadCreator.apply(getContext());
    fileDownloaderActor.tell(new DownloadFile(crawlStatus.getNext()), getSelf());
  }

  private void onFileDownloadResult(FileDownloadResult message) {
    ActorRef urlExtractor = urlExtractorCreator.apply(getContext());
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