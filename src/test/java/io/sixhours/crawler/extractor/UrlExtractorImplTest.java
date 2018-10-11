package io.sixhours.crawler.extractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test class for {@code UrlExtractorImpl}.
 *
 * @author Mladen Bolic
 */
public class UrlExtractorImplTest {

  private static final String EXISTING_FILE_URL = "index.html";
  private static final String NON_EXISTING_FILE_URL = "non-existing.html";
  private static final String BASE_URI = "http://sixhours.io";

  @Rule
  public TemporaryFolder temporaryFolder;

  private UrlExtractor urlExtractor;

  @Before
  public void setUp() throws Exception {
    temporaryFolder = new TemporaryFolder();
    temporaryFolder.create();

    urlExtractor = new UrlExtractorImpl();
  }

  @After
  public void tearDown() {
    temporaryFolder.delete();
  }

  @Test
  public void givenExistingUrl_whenUrlExtract_thenReturnUrlExtractResult() throws Exception {
    URL url = Thread.currentThread().getContextClassLoader().getResource(EXISTING_FILE_URL);
    String file = url.getFile();

    Set expectedResult = new HashSet(
        Arrays.asList("http://sixhours.io/scripts.js", "http://sixhours.io/page1"));

    UrlExtractResult result = urlExtractor
        .extractUrls(BASE_URI, file);

    assertThat(result.getUrls()).isEqualTo(expectedResult);
  }

  @Test(expected = UrlExtractException.class)
  public void givenNotExistingUrl_whenFileDownload_thenFailWithException() throws Exception {
    URL url = Thread.currentThread().getContextClassLoader().getResource(EXISTING_FILE_URL);
    String file = url.getFile().replace(EXISTING_FILE_URL, NON_EXISTING_FILE_URL);

    urlExtractor
        .extractUrls(BASE_URI, file);

    fail("Should throw UrlExtractException");
  }
}