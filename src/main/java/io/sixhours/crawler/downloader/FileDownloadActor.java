package io.sixhours.crawler.downloader;

import akka.actor.AbstractActor;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import scala.concurrent.ExecutionContext;

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

  public static final String NAME = "file-download";

  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private final FileDownloader fileDownloader;


  public static Props props(FileDownloader fileDownloader) {
    return Props.create(FileDownloadActor.class, fileDownloader);
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

    ActorRef sender = getSender();
    ExecutionContext executionContext = getContext().system().dispatcher();

    Futures.future(() -> fileDownloader.downloadFile(url), executionContext)
        .onComplete(new OnComplete<FileDownloadResult>() {
          @Override
          public void onComplete(Throwable failure, FileDownloadResult result) {
            if (failure != null) {
              sender.tell(new FileDownloadError(url), Actor.noSender());
            } else {
              sender.tell(result, Actor.noSender());
            }
          }
        }, executionContext);
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(DownloadFile.class, this::onDownloadFile)
        .build();
  }
}
