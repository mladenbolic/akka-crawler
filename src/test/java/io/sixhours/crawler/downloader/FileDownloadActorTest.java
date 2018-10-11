package io.sixhours.crawler.downloader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import io.sixhours.crawler.downloader.FileDownloadActor.DownloadFile;
import io.sixhours.crawler.downloader.FileDownloadActor.FileDownloadError;
import io.sixhours.crawler.downloader.FileDownloadActor.FileDownloadResult;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test class for {@code FileDownloadActor}.
 *
 * @author Mladen Bolic
 */
@RunWith(MockitoJUnitRunner.class)
public class FileDownloadActorTest {

  private static final String TEST_URL = "http://test.url/contact";
  private static final String TEST_PATH = "/test/path";

  static ActorSystem system;

  @Mock
  private FileDownloader fileDownloader;

  @BeforeClass
  public static void setUpClass() {
    system = ActorSystem.create();
  }

  @AfterClass
  public static void tearDownClass() {
    TestKit.shutdownActorSystem(system);
    system = null;
  }

  @Test
  public void givenUrl_whenFileDownload_thenReturnDownloadResultForFile() throws Exception {
    TestKit probe = new TestKit(system);
    when(fileDownloader.downloadFile(any(String.class)))
        .thenReturn(new FileDownloadResult(TEST_URL, TEST_PATH));

    ActorRef fileDownloaderActor = system
        .actorOf(FileDownloadActor.props(fileDownloader), FileDownloadActor.name(String.valueOf(
            UUID.randomUUID())));

    fileDownloaderActor.tell(new DownloadFile(TEST_URL), probe.getRef());

    FileDownloadResult response = probe.expectMsgClass(FileDownloadResult.class);

    verify(fileDownloader, times(1))
        .downloadFile(any(String.class));
    assertThat(response.getUrl()).isEqualTo(TEST_URL);
    assertThat(response.getPath()).isEqualTo(TEST_PATH);
  }

  @Test
  public void givenUrl_whenFileDownloadException_thenReturnErrorResult() throws Exception {
    TestKit probe = new TestKit(system);
    when(fileDownloader.downloadFile(any(String.class)))
        .thenThrow(new FileDownloadException("error"));

    ActorRef fileDownloaderActor = system.actorOf(FileDownloadActor.props(fileDownloader));

    fileDownloaderActor.tell(new DownloadFile(TEST_URL), probe.getRef());

    FileDownloadError response = probe.expectMsgClass(FileDownloadError.class);

    verify(fileDownloader, times(1))
        .downloadFile(any(String.class));
    assertThat(response.getUrl()).isEqualTo(TEST_URL);
  }
}