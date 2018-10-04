package io.sixhours.crawler.downloader;

import akka.actor.AbstractActor;
import akka.actor.Actor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * @author
 */
@RequiredArgsConstructor
public class FileDownloadActor extends AbstractActor {

  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private static final String NAME_PREFIX = "download-file-%s";

  private final String baseUrl;
  private final FileDownloader fileDownloader;

  public static Props props(String url, FileDownloader fileDownloader) {
    return Props.create(FileDownloadActor.class, url, fileDownloader);
  }

  public static String name(String suffix) {
    return String.format(NAME_PREFIX, suffix);
  }

  @Value
  public static final class DownloadFile {

    final String url;
  }

  @Value
  public static final class FileDownloaded {

    final String path;
  }

  @Value
  public static final class FileDownloadError {

    private final String url;
  }

  private void onDownloadFile(DownloadFile message) {
    String url = message.url;

    try {
      // return download file result => we need to process downloaded file (extract urls)
      FileDownloadResult result = fileDownloader.downloadFile(baseUrl, url);
//      log.info("### Sending FileDownloadResult");
      getSender().tell(result, Actor.noSender());

//       tell to sender that file is downloaded (we can use this to update the current status and/or for letting parent to handle the creation of url extractor
//      getSender().tell(new FileDownloaded(url), Actor.noSender());
      getContext().stop(getSelf());

//      ActorRef urlExtractor = getContext()
//          .actorOf(UrlExtractActor.props(new UrlExtractorImpl()), UrlExtractActor.NAME);
//      urlExtractor.forward(new ExtractUrls(result.getPath(), this.baseUrl), getContext());

      // we should kill the actor only when it recieved data from url extractor
//      getContext().stop(getSelf());

    } catch (FileDownloadException e) {
      // e.printStackTrace();
      getSender().tell(new FileDownloadError(url), Actor.noSender());
    }
  }

  private void onFileDownloaded(FileDownloaded message) {
    log.debug("File downloaded {}", message.path);
  }
//
//  @Override
//  public void preStart() {
//    log.info("FileDownloadActor started");
//  }
//
//  @Override
//  public void postStop() {
//    log.info("FileDownloadActor stopped");
//  }

  @Override
  public void preRestart(Throwable reason, Optional<Object> message) throws Exception {
    log.info("Restarting FileDownloadActor because of {}", reason.getClass());
    super.preRestart(reason, message);
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(DownloadFile.class, this::onDownloadFile)
        .match(FileDownloaded.class, this::onFileDownloaded)
        .build();
  }
}
