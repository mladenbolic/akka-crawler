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
 * Test class for {@code LinkExtractorImpl}.
 *
 * @author Mladen Bolic
 */
public class LinkExtractorImplTest {

  private static final String EXISTING_FILE_URL = "index.html";
  private static final String NON_EXISTING_FILE_URL = "non-existing.html";
  private static final String BASE_URI = "http://sixhours.io";

  @Rule
  public TemporaryFolder temporaryFolder;

  private LinkExtractor linkExtractor;

  @Before
  public void setUp() throws Exception {
    temporaryFolder = new TemporaryFolder();
    temporaryFolder.create();

    linkExtractor = new LinkExtractorImpl();
  }

  @After
  public void tearDown() {
    temporaryFolder.delete();
  }

  @Test
  public void givenExistingUrl_whenUrlExtract_thenReturnUrlExtractResult() {
    URL url = Thread.currentThread().getContextClassLoader().getResource(EXISTING_FILE_URL);
    String file = url.getFile();

    Set<String> expectedResult = new HashSet<>(
        Arrays.asList("http://sixhours.io/scripts.js", "http://sixhours.io/page1"));

    LinkExtractResult result = linkExtractor
        .extractUrls(BASE_URI, file);

    assertThat(result.getUrls()).isEqualTo(expectedResult);
  }

  @Test(expected = LinkExtractException.class)
  public void givenNotExistingUrl_whenFileDownload_thenFailWithException() {
    URL url = Thread.currentThread().getContextClassLoader().getResource(EXISTING_FILE_URL);
    String file = url.getFile().replace(EXISTING_FILE_URL, NON_EXISTING_FILE_URL);

    linkExtractor
        .extractUrls(BASE_URI, file);

    fail("Should throw LinkExtractException");
  }
}