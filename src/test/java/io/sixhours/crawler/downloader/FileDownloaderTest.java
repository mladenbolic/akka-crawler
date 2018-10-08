package io.sixhours.crawler.downloader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.net.URL;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileDownloaderTest {

  private static final String EXISTING_FILE_URL = "index.html";
  private static final String NON_EXISTING_FILE_URL = "non-existing.html";

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
  public void givenExistingUrl_whenFileDownload_thenReturnDownloadResult() throws Exception {
    URL url = Thread.currentThread().getContextClassLoader().getResource(EXISTING_FILE_URL);
    String path = url.toString();

    FileDownloadResult result = fileDownloader
        .downloadFile(path);

    assertThat(result.getUrl()).isEqualTo(url.toString());
    assertThat(result.getPath()).isNotBlank();
  }

  @Test(expected = FileDownloadException.class)
  public void givenNotExistingUrl_whenFileDownload_thenFailWithException() throws Exception {
    URL url = Thread.currentThread().getContextClassLoader().getResource(EXISTING_FILE_URL);
    String path = url.toString().replace(EXISTING_FILE_URL, NON_EXISTING_FILE_URL);

    System.out.println(path);
    fileDownloader.downloadFile(path);

    fail("Should throw FileDownloadException");
  }
}