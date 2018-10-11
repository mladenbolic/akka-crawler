package io.sixhours.crawler.supervisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test class for {@code CrawlStatus}.
 *
 * @author Mladen Bolic
 */
@RunWith(MockitoJUnitRunner.class)
public class CrawlStatusTest {

  private static final String URL_A = "http://a.com";
  private static final String URL_B = "http://b.com";

  @Spy
  private CrawlStatus crawlStatus;

  @Test
  public void givenCrawlStatus_whenAddDuplicateUrl_thenResultDoesNotContainDuplicates() {
    crawlStatus.add(URL_A);

    crawlStatus.add(URL_B);
    crawlStatus.add(URL_B);

    assertThat(crawlStatus.getRemaining())
        .containsExactlyInAnyOrder(URL_A, URL_B);
    assertThat(crawlStatus.getTotal())
        .containsExactlyInAnyOrder(URL_A, URL_B);
  }

  @Test
  public void givenCrawlStatus_whenAddAllUrls_thenAddUrlsOneByOne() {
    crawlStatus.addAll(Arrays.asList(URL_A, URL_B));

    verify(crawlStatus, times(2)).add(any(String.class));
    assertThat(crawlStatus.getRemaining())
        .containsExactlyInAnyOrder(URL_A, URL_B);
    assertThat(crawlStatus.getTotal())
        .containsExactlyInAnyOrder(URL_A, URL_B);
  }

  @Test
  public void giveCrawlStatus_whenAddProcess_thenUpdateProcessedAndProcessing() {
    crawlStatus.add(URL_A);
    crawlStatus.add(URL_B);

    crawlStatus.next();
    crawlStatus.addProcessed(URL_A);

    assertThat(crawlStatus.getProcessed())
        .containsExactly(URL_A);
    assertThat(crawlStatus.getRemaining())
        .containsExactly(URL_B);
  }

  @Test
  public void giveCrawlStatus_whenAddFailed_thenUpdateFailedAndProcessing() {
    crawlStatus.add(URL_A);
    crawlStatus.add(URL_B);

    crawlStatus.next();
    crawlStatus.addFailed(URL_A);

    assertThat(crawlStatus.getFailed())
        .containsExactly(URL_A);
    assertThat(crawlStatus.getRemaining())
        .containsExactly(URL_B);
  }

  @Test
  public void givenCrawlStatusWithUrl_whenNext_thenReturnResult() {
    crawlStatus.add(URL_A);

    Optional<String> result = crawlStatus.next();

    assertThat(crawlStatus.getRemaining()).isEmpty();
    assertThat(result).isEqualTo(Optional.of(URL_A));
  }

  @Test
  public void givenCrawlStatusWithoutUrl_whenNext_thenReturnEmptyResult() {
    Optional<String> result = crawlStatus.next();

    assertThat(crawlStatus.getRemaining()).isEmpty();
    assertThat(result).isEqualTo(Optional.empty());
  }

  @Test
  public void givenCrawlStatusWithEmptyProcessingAndRemaining_whenIsFinished_thenReturnTrue() {
    boolean result = crawlStatus.isFinished();

    assertThat(result).isTrue();
  }


  @Test
  public void givenCrawlStatusWithNotEmptyProcessingOrRemaining_whenIsFinished_thenReturnFalse() {
    crawlStatus.add(URL_A);

    boolean result = crawlStatus.isFinished();

    assertThat(result).isFalse();
  }
}