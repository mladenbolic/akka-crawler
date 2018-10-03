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
import io.sixhours.crawler.downloader.FileDownloaderImpl;
import io.sixhours.crawler.extractor.UrlExtractActor.UrlsExtracted;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Value;

public class CrawlSupervisor extends AbstractActor {

  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private final CrawlStatus visitedPageStore = new CrawlStatus();

  private Map<String, ActorRef> downloadActors = new HashMap<>();
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
  public static class StartCrawling1 {

    private final String url;
  }

  @Value
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
    String fileUrl = message.getPath();

    failedUrls.add(fileUrl);
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
          fileDownloaderActor.tell(new DownloadFile(url), getSelf());

//          if (!processedUrls.contains(url)) {
//            processedUrls.add(url);
//
//            // for each new different url request we create new DownloadFile actor
//            ActorRef fileDownloaderActor = getContext().actorOf(FileDownloadActor.props(url),
//                FileDownloadActor.name(String.valueOf(processedUrls.size())));
//            getContext().watch(fileDownloaderActor);
//
//            fileDownloaderActor.tell(new DownloadFile(url), getSelf());
//  // TODO: think there is no point to keep the reference of the DownloadFile actor
//          }
        })
        .match(UrlsExtracted.class, message -> {
          // getIndexer().tell(content, getSelf());
          visitedPageStore.addAll(message.getUrls());

          log.info(visitedPageStore.toString());

          if (visitedPageStore.isFinished()) {
            // getIndexer().tell(IndexingActor.COMMIT_MESSAGE, getSelf());
            log.info("Shutting down, finished");
            getContext().system().terminate();
          } else {
            for (String url : visitedPageStore.getNextBatch()) {
              ActorRef fileDownloaderActor = getContext()
                  .actorOf(FileDownloadActor.props(url, new FileDownloaderImpl()),
                      FileDownloadActor.name(String.valueOf(UUID.randomUUID())));
              getContext().watch(fileDownloaderActor);
              fileDownloaderActor.tell(new DownloadFile(url), getSelf());
            }
          }
        })
        .match(CrawlFinished.class, this::onCrawlFinished)
        .match(FileDownloadError.class, this::onFileDownloadError)
        .match(Terminated.class, this::onTerminated)
        .build();
  }
}