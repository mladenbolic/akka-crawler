package io.sixhours.crawler.downloader;

import akka.actor.AbstractActor;
import akka.actor.Actor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * Actor for handling file downloads.
 *
 * <p>Actor should return {@code FileDownloadResult} upon successful file download.
 *
 * <p>Actor should return {@code FileDownloadError} if error occurs during file download.
 *
 * @author Mladen Bolic
 */
@RequiredArgsConstructor
public class FileDownloadActor extends AbstractActor {

  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private static final String NAME_PREFIX = "file-download-%s";

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
  public static class FileDownloadResult {

    private final String url;
    private final String path;
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

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(DownloadFile.class, this::onDownloadFile)
        .build();
  }
}
