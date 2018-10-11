package io.sixhours.crawler.downloader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.sixhours.crawler.downloader.FileDownloadActor.FileDownloadResult;
import java.io.IOException;
import java.net.URL;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test class for {@code FileDownloaderImpl}.
 *
 * @author Mladen Bolic
 */
public class FileDownloaderImplTest {

  private static final String EXISTING_URL = "page1";
  private static final String EXISTING_FILE_URL = "index.html";
  private static final String NON_EXISTING_FILE_URL = "non-existing.html";
  private static final String HTML_EXTENSION = ".html";

  @Rule
  public TemporaryFolder temporaryFolder;

  private FileDownloader fileDownloader;

  @Before
  public void setUp() throws Exception {
    temporaryFolder = new TemporaryFolder();
    temporaryFolder.create();

    fileDownloader = new FileDownloaderImpl(temporaryFolder.getRoot().getPath());
  }

  @After
  public void tearDown() {
    temporaryFolder.delete();
  }

  @Test
  public void givenExistingUrl_whenDownloadFile_thenReturnDownloadResult() throws Exception {
    URL url = Thread.currentThread().getContextClassLoader().getResource(EXISTING_FILE_URL);
    String path = url.toString();

    FileDownloadResult result = fileDownloader
        .downloadFile(path);

    assertThat(result.getUrl()).isEqualTo(url.toString());
    assertThat(result.getPath()).contains(EXISTING_FILE_URL);
  }

  @Test
  public void givenNotExistingUrl_whenDownloadFile_thenFailWithException() throws Exception {
    URL url = Thread.currentThread().getContextClassLoader().getResource(EXISTING_FILE_URL);
    String path = url.toString().replace(EXISTING_FILE_URL, NON_EXISTING_FILE_URL);

    assertThatThrownBy(() -> fileDownloader.downloadFile(path))
        .hasCauseInstanceOf(IOException.class);
  }

  @Test
  public void givenExistingRootUrl_whenDownloadFile_thenAppendHtmlExtensionAndReturnResult()
      throws Exception {
    URL url = Thread.currentThread().getContextClassLoader().getResource(EXISTING_URL);
    String path = url.toString();

    FileDownloadResult result = fileDownloader
        .downloadFile(path);

    assertThat(result.getUrl()).isEqualTo(url.toString());
    assertThat(result.getPath()).contains(EXISTING_URL + HTML_EXTENSION);
  }
}