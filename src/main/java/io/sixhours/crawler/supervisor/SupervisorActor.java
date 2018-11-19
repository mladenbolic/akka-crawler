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
import io.sixhours.crawler.extractor.LinkExtractActor.ExtractLinks;
import io.sixhours.crawler.extractor.LinkExtractActor.LinksExtracted;
import io.sixhours.crawler.extractor.LinkExtractException;
import java.time.Duration;
import java.util.function.Consumer;
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
public class SupervisorActor extends AbstractActor {

  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private final CrawlStatus crawlingStatus;

  private final Function<ActorRefFactory, ActorRef> fileDownloadCreator;

  private final Function<ActorRefFactory, ActorRef> linkExtractorCreator;

  private final Consumer<ActorContext> terminate;

  private ActorRef fileDownloaderActor;
  private ActorRef linkExtractor;

  private final SupervisorStrategy strategy = new OneForOneStrategy(3, Duration.ofMinutes(1),
      DeciderBuilder
          .match(LinkExtractException.class, e -> {
            log.error("Link extraction error: {}", e.getMessage());
            return SupervisorStrategy.restart();
          })
          .match(Throwable.class, e -> {
            log.error("Unexpected error: {}", e.getMessage());
            return SupervisorStrategy.stop();
          })
          .build()
  );

  public static final String NAME = "crawl-supervisor";

  public static Props props(CrawlStatus crawlStatus,
      Function<ActorRefFactory, ActorRef> fileDownloadCreator,
      Function<ActorRefFactory, ActorRef> linkExtractorCreator,
      Consumer<ActorContext> terminate) {
    return Props.create(SupervisorActor.class, crawlStatus, fileDownloadCreator,
            linkExtractorCreator, terminate);
  }

  @Value
  public static class StartCrawling {

    private String url;
  }

  @Value
  public static class CrawlFinished {

  }

  @Override
  public SupervisorStrategy supervisorStrategy() {
    return strategy;
  }

  @Override
  public void preStart() {
    log.info("Crawler started");
    fileDownloaderActor = fileDownloadCreator.apply(getContext());
    linkExtractor = linkExtractorCreator.apply(getContext());
  }

  @Override
  public void postStop() {
    log.info("Crawler stopped");
  }

  private void onStartCrawling(StartCrawling message) {
    String url = message.url;
    crawlingStatus.add(url);

    log.info(crawlingStatus.print());

    crawlingStatus.next()
        .ifPresent(path -> fileDownloaderActor.tell(new DownloadFile(path), getSelf()));
  }

  private void onCrawlFinished(@SuppressWarnings("unused") CrawlFinished message) {
    log.info("============================================================");
    log.info(crawlingStatus.print());
    log.info("============================================================\n");
    log.info("Failed urls:");
    crawlingStatus.getFailed()
        .forEach(log::info);

    terminate.accept(getContext());
  }

  private void onFileDownloadResult(FileDownloadResult message) {
    linkExtractor.tell(new ExtractLinks(message.getUrl(), message.getPath()), getSelf());
  }

  private void onFileDownloadError(FileDownloadError message) {
    crawlingStatus.addFailed(message.getUrl());

    log.info(crawlingStatus.print());
  }

  private void onLinksExtracted(LinksExtracted message) {
    crawlingStatus.addProcessed(message.getUrl());
    crawlingStatus.addAll(message.getNewUrls());

    log.info(crawlingStatus.print());

    if (crawlingStatus.isFinished()) {
      getSelf().tell(new CrawlFinished(), ActorRef.noSender());
    } else {
      crawlingStatus.getRemainingUrls()
          .forEach(url -> fileDownloaderActor.tell(new DownloadFile(url), getSelf()));
    }
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(StartCrawling.class, this::onStartCrawling)
        .match(CrawlFinished.class, this::onCrawlFinished)
        .match(FileDownloadResult.class, this::onFileDownloadResult)
        .match(FileDownloadError.class, this::onFileDownloadError)
        .match(LinksExtracted.class, this::onLinksExtracted)
        .build();
  }
}