package io.sixhours.crawler.downloader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import io.sixhours.crawler.downloader.FileDownloadActor.DownloadFile;
import io.sixhours.crawler.downloader.FileDownloadActor.FileDownloadError;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FileDownloadActorTest {

  static ActorSystem system;

  @Mock
  private FileDownloader fileDownloader;

  @BeforeClass
  public static void setup() {
    system = ActorSystem.create();
  }

  @AfterClass
  public static void teardown() {
    TestKit.shutdownActorSystem(system);
    system = null;
  }

  @Test
  public void givenUrl_whenFileDownload_thenDownloadResult() throws Exception {
    TestKit probe = new TestKit(system);
    when(fileDownloader.downloadFile(any(String.class), any(String.class)))
        .thenReturn(new FileDownloadResult("http://some.url", "/some/path"));

    ActorRef fileDownloaderActor = system.actorOf(FileDownloadActor.props("group", fileDownloader));

    fileDownloaderActor.tell(new DownloadFile("http://some.url"), probe.getRef());

    FileDownloadResult response = probe.expectMsgClass(FileDownloadResult.class);

    verify(fileDownloader, times(1))
        .downloadFile(any(String.class), any(String.class));
    assertThat(response.getUrl()).isEqualTo("http://some.url");
    assertThat(response.getPath()).isEqualTo("/some/path");
  }

  @Test
  public void givenUrl_whenFileDownloadException_thenErrorResult() throws Exception {
    TestKit probe = new TestKit(system);
    when(fileDownloader.downloadFile(any(String.class), any(String.class)))
        .thenThrow(new FileDownloadException("error"));

    ActorRef fileDownloaderActor = system.actorOf(FileDownloadActor.props("group", fileDownloader));

    fileDownloaderActor.tell(new DownloadFile("http://some.url"), probe.getRef());

    FileDownloadError response = probe.expectMsgClass(FileDownloadError.class);

    verify(fileDownloader, times(1))
        .downloadFile(any(String.class), any(String.class));
    assertThat(response.getUrl()).isEqualTo("http://some.url");
  }
}