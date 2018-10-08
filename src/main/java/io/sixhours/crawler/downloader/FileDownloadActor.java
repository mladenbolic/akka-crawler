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

  private final FileDownloader fileDownloader;

  public static Props props(FileDownloader fileDownloader) {
    return Props.create(FileDownloadActor.class, fileDownloader);
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
      FileDownloadResult result = fileDownloader.downloadFile(url);
      getSender().tell(result, Actor.noSender());
      getContext().stop(getSelf());
    } catch (FileDownloadException e) {
      getSender().tell(new FileDownloadError(url), Actor.noSender());
    }
  }

  private void onFileDownloaded(FileDownloaded message) {
    log.debug("File downloaded {}", message.path);
  }

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
