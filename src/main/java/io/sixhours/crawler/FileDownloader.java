package io.sixhours.crawler;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.sixhours.crawler.CrawlSupervisor.StartCrawling;
import io.sixhours.crawler.UrlExtractor.ExtractUrls;
import io.sixhours.crawler.UrlExtractor.UrlsExtracted;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * @author
 */
public class FileDownloader extends AbstractActor {

  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  final String url;

  public static final String NAME = "download-file";
  private static final String NAME_PREFIX = "download-file-%s";
  private static final String DOWNLOAD_DIR = "/Users/mladen/Downloads/xxx/";

  public static Props props(String url) {
    return Props.create(FileDownloader.class, url);
  }

  public static String name(String suffix) {
    return String.format(NAME_PREFIX, suffix);
  }

  public FileDownloader(String url) {
    this.url = url;
  }

  @RequiredArgsConstructor
  public static final class DownloadFile {

    final String fileName;
  }

  @RequiredArgsConstructor
  public static final class FileDownloaded {

    final String fileName;
  }

  @RequiredArgsConstructor
  public static final class FileDownloadError {

    final String fileName;
  }

  private void onDownloadFile(DownloadFile message) {
    String fileName = message.fileName;

//    FileUtils.copyURLToFile(
//        new URL(FILE_URL),
//        new File(FILE_NAME),
//        CONNECT_TIMEOUT,
//        READ_TIMEOUT);

    URL fileUrl = null;
    String filePath = null;
    try {
      fileUrl = new URL(fileName);
      filePath = fileUrl.getPath();

      filePath = ("/".equals(filePath)) ? "index.html" : filePath;
      log.info("Path: {}", filePath);

      Path f = Paths.get(DOWNLOAD_DIR + filePath);
      if (!Files.exists(f)) {
        Files.createDirectories(f.getParent());
        Files.createFile(f);
      }
    } catch (IOException e) {
      e.printStackTrace();
      log.error("Error downloading file!", e);
      getSender().tell(new FileDownloadError(filePath), getSelf());
    }

    try (
        ReadableByteChannel readableByteChannel = Channels.newChannel(fileUrl.openStream());
        FileOutputStream fileOutputStream = new FileOutputStream(
            DOWNLOAD_DIR + filePath);
    ) {

      fileOutputStream.getChannel()
          .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

      // TODO: should we stop ourself, well yes, we can free-up memory if we are not going to use the actor anymore
      getContext().stop(getSelf());
      getSender().tell(new FileDownloaded(filePath), getSelf());

      // urlExtractor will have reference to CrawlerServer, so it can send direct CrawlRequest, not using DownloadFile actor
      // 'forward' so URL extractor could have reference, not to the current actor, but to his parent actor
      ActorRef urlExtractor = getContext().actorOf(UrlExtractor.props(), UrlExtractor.NAME);
      urlExtractor.forward(new ExtractUrls(0L, DOWNLOAD_DIR + filePath, this.url), getContext());

    } catch (IOException e) {
      e.printStackTrace();
      log.error("Error downloading file!", e);
      getSender().tell(new FileDownloadError(filePath), getSelf());
    }
  }

  private void onFileDownloaded(FileDownloaded message) {
    log.debug("File downloaded {}", message.fileName);
  }

  private void onUrlsExtracted(UrlsExtracted message) {
    List<String> urls = message.urls;

    // start crawling new urls
    urls.forEach(s -> getContext().getParent().tell(new StartCrawling(s), getSender()));
  }

  @Override
  public void preStart() {
    log.info("DownloadFile started");
  }

  @Override
  public void postStop() {
    log.info("DownloadFile stopped");
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(DownloadFile.class, this::onDownloadFile)
        .match(FileDownloaded.class, this::onFileDownloaded)
        .match(UrlsExtracted.class, this::onUrlsExtracted)
        .build();
  }
}
