package io.sixhours.crawler;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.sixhours.crawler.FileDownloader.DownloadFile;
import io.sixhours.crawler.FileDownloader.FileDownloadError;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;

public class CrawlSupervisor extends AbstractActor {

  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private Map<String, ActorRef> downloadActors = new HashMap<>();
  private Set<String> processedUrls = new HashSet<>();
  private Set<String> failedUrls = new HashSet<>();

  public static final String NAME = "crawler-supervisor";

  public static Props props() {
    return Props.create(CrawlSupervisor.class);
  }

  public CrawlSupervisor() {
  }


  @RequiredArgsConstructor
  public static class StartCrawling {

    private final String url;
  }

  @RequiredArgsConstructor
  public static class CrawlFinished {

    private final String url;
    private final Set<String> extractedUrls;
    private final Set<String> failedUrls;
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
//    ActorRef actor = t.getActor();
//    String groupId = downloadActors.get(actor);
//    log.info("Device group actor for {} has been terminated", groupId);
//    downloadActors.remove(groupActor);
//    downloadActors.remove(groupId);
    log.info("Actor terminated: {}", t.getActor());
  }

  private void onCrawlFinished(CrawlFinished message) {
    // maybe to aggregate total files downloaded, skipped
    // TOTAL: downloads: 1, errors: 0
    // like the aggregation of unit test results
  }

  private void onFileDownloadError(FileDownloadError message) {
    String fileUrl = message.fileName;

    failedUrls.add(fileUrl);
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(StartCrawling.class, crawlRequest -> {
          String url = crawlRequest.url;

          if (!processedUrls.contains(url)) {
            processedUrls.add(url);

            // for each new different url request we create new DownloadFile actor
            ActorRef fileDownloaderActor = getContext().actorOf(FileDownloader.props(url),
                FileDownloader.name(String.valueOf(processedUrls.size())));
            getContext().watch(fileDownloaderActor);

            fileDownloaderActor.tell(new DownloadFile(url), getSelf());
  // TODO: see if DownloadFile actor should start himself, or we should do it from CrawlServer
  // TODO: think there is no point to keep the reference of the DownloadFile actor
          }
        })
        .match(CrawlFinished.class, this::onCrawlFinished)
        .match(FileDownloadError.class, this::onFileDownloadError)
        .match(Terminated.class, this::onTerminated)
        .build();
  }
}