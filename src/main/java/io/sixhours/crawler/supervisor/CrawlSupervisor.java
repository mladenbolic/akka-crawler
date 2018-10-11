package io.sixhours.crawler.supervisor;

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
import io.sixhours.crawler.extractor.UrlExtractActor.ExtractUrls;
import io.sixhours.crawler.extractor.UrlExtractActor.UrlsExtracted;
import io.sixhours.crawler.extractor.UrlExtractException;
import java.time.Duration;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * Actor used to supervise and handle actors for downloading file content and actors for extracting
 * urls from files.
 *
 * @author Mladen Bolic
 */
@SuppressWarnings("PMD.UnusedFormalParameter")
@RequiredArgsConstructor
public class CrawlSupervisor extends AbstractActor {

  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private final CrawlStatusTmp crawlStatus;

  private final Function<ActorRefFactory, ActorRef> fileDownloadCreator;

  private final Function<ActorRefFactory, ActorRef> urlExtractorCreator;

  public static final String NAME = "crawl-supervisor";

  public static Props props(CrawlStatusTmp crawlStatus,
      Function<ActorRefFactory, ActorRef> fileDownloadCreator,
      Function<ActorRefFactory, ActorRef> urlExtractorCreator) {
    return Props.create(CrawlSupervisor.class, crawlStatus, fileDownloadCreator,
        urlExtractorCreator);
  }

  @Value
  public static class StartCrawling {

    private String url;
  }

  @Value
  public static class CrawlFinished {

  }

  private SupervisorStrategy strategy = new OneForOneStrategy(3, Duration.ofMinutes(1),
      DeciderBuilder
          .match(UrlExtractException.class, e -> {
            log.error("Url extraction error: {}", e.getMessage());
            return SupervisorStrategy.resume();
          })
          .match(Throwable.class, e -> {
            log.error("Unexpected error: {}", e.getMessage());
            return SupervisorStrategy.stop();
          })
          .build()
  );

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
    log.info(crawlStatus.print());
    log.info("============================================================\n");
    crawlStatus.getFailed()
        .forEach(log::info);

    // TODO: see when to terminate
    // getContext().system().terminate();
  }

  private void onStartCrawling(StartCrawling message) {
    String url = message.url;
    crawlStatus.add(url);

    ActorRef fileDownloaderActor = fileDownloadCreator.apply(getContext());
    crawlStatus.next()
        .ifPresent(path -> fileDownloaderActor.tell(new DownloadFile(path), getSelf()));
  }

  private void onFileDownloadResult(FileDownloadResult message) {
    ActorRef urlExtractor = urlExtractorCreator.apply(getContext());
    urlExtractor.tell(
        new ExtractUrls(message.getUrl(), message.getPath()),
        getSelf());
  }

  private void onFileDownloadError(FileDownloadError message) {
    crawlStatus.addFailed(message.getUrl());
  }

  private void onUrlsExtracted(UrlsExtracted message) {
    crawlStatus.addProcessed(message.getUrl());
    crawlStatus.addAll(message.getNewUrls());

    log.info(crawlStatus.print());

    if (crawlStatus.isFinished()) {
      getSelf().tell(new CrawlFinished(), ActorRef.noSender());
    } else {
      crawlStatus.getRemaining()
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