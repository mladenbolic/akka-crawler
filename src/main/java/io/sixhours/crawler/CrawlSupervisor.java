package io.sixhours.crawler;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Value;

public class CrawlSupervisor extends AbstractActor {

  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private final CrawlStatus visitedPageStore = new CrawlStatus();

  private Map<String, ActorRef> urlToDownloadActor = new HashMap<>();
  private Map<ActorRef, String> downloadActorToUrl = new HashMap<>();
  private Set<String> processedUrls = new HashSet<>();
  private Set<String> failedUrls = new HashSet<>();

  public static final String NAME = "crawler-supervisor";

  public static Props props() {
    return Props.create(CrawlSupervisor.class);
  }

  @Value
  public static class StartCrawling {

    private final String url;
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

  // TODO: do we really need this? We need it if we are going to "supervise" write which actor didn't finish it's job
  private void onTerminated(Terminated t) {
    ActorRef actor = t.getActor();
    String url = downloadActorToUrl.get(actor);
//    log.info("Url could not be processed: {}", url);
//    urlToDownloadActor.remove(groupActor);
//    urlToDownloadActor.remove(groupId);
//    log.error("Actor terminated: {}", t.getActor());
  }

  private void onCrawlFinished(CrawlFinished message) {
    log.info("============================================================");
    log.info(visitedPageStore.toString());
    log.info("============================================================\n");
    visitedPageStore.getFailedPages()
        .forEach(log::info);

    getContext().system().terminate();
  }

  private void onFileDownloadError(FileDownloadError message) {
    visitedPageStore.addFailed(message.getUrl());
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(StartCrawling.class, crawlRequest -> {
          String url = crawlRequest.url;

          visitedPageStore.add(url);

          ActorRef fileDownloaderActor = getContext()
              .actorOf(FileDownloadActor.props(url, new FileDownloaderImpl()),
                  FileDownloadActor.name(String.valueOf(visitedPageStore.size())));

          getContext().watch(fileDownloaderActor);
          fileDownloaderActor.tell(new DownloadFile(visitedPageStore.getNext()), getSelf());

          urlToDownloadActor.putIfAbsent(url, fileDownloaderActor);
          downloadActorToUrl.putIfAbsent(fileDownloaderActor, url);
        })
        .match(FileDownloadResult.class, message -> {
          ActorRef urlExtractor = getContext()
              .actorOf(UrlExtractActor.props(new UrlExtractorImpl()),
                  UrlExtractActor.NAME + String.valueOf(UUID.randomUUID()));
          urlExtractor.tell(
              new ExtractUrls(message.getUrl(), message.getPath(), "http://www.burgerking.no/"),
              getSelf());
        })
        .match(UrlsExtracted.class, message -> {
          visitedPageStore.addAll(message.getUrls());
          visitedPageStore.processed(message.getUrl());

          log.info(visitedPageStore.toString());

          if (visitedPageStore.isFinished()) {
            log.info("Finished crawling");
            getSelf().tell(new CrawlFinished(), ActorRef.noSender());
          } else {
            for (String url : visitedPageStore.getNextBatch()) {
              ActorRef fileDownloaderActor = getContext()
                  .actorOf(FileDownloadActor.props(url, new FileDownloaderImpl()),
                      FileDownloadActor.name(String.valueOf(UUID.randomUUID())));

              getContext().watch(fileDownloaderActor);
              fileDownloaderActor.tell(new DownloadFile(url), getSelf());

              urlToDownloadActor.putIfAbsent(url, fileDownloaderActor);
              downloadActorToUrl.putIfAbsent(fileDownloaderActor, url);
            }
          }
        })
        .match(CrawlFinished.class, this::onCrawlFinished)
        .match(FileDownloadError.class, this::onFileDownloadError)
        .match(Terminated.class, this::onTerminated)
        .build();
  }
}