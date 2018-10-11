package io.sixhours.crawler.supervisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorRefFactory;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.EventFilter;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.ConfigFactory;
import io.sixhours.crawler.downloader.FileDownloadActor.DownloadFile;
import io.sixhours.crawler.downloader.FileDownloadActor.FileDownloadError;
import io.sixhours.crawler.downloader.FileDownloadActor.FileDownloadResult;
import io.sixhours.crawler.extractor.UrlExtractActor;
import io.sixhours.crawler.extractor.UrlExtractActor.ExtractUrls;
import io.sixhours.crawler.extractor.UrlExtractActor.UrlsExtracted;
import io.sixhours.crawler.extractor.UrlExtractException;
import io.sixhours.crawler.extractor.UrlExtractor;
import io.sixhours.crawler.supervisor.CrawlSupervisor.CrawlFinished;
import io.sixhours.crawler.supervisor.CrawlSupervisor.StartCrawling;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test class for {@code CrawlSupervisor} actor.
 *
 * @author Mladen Bolic
 */
@SuppressWarnings({"PMD.NonStaticInitializer", "PMD.JUnitTestsShouldIncludeAssert"})
@RunWith(MockitoJUnitRunner.class)
public class CrawlSupervisorTest {

  private static final String TEST_URL = "http://test.url/";
  private static final String TEST_PATH = "/test/path";

  @Spy
  private CrawlStatus crawlStatus;

  @Mock
  private UrlExtractor urlExtractor;

  static ActorSystem system;

  @BeforeClass
  public static void setUpClass() {
    system = ActorSystem.create("test-system",
        ConfigFactory.parseString("akka.loggers = [\"akka.testkit.TestEventListener\"]"));
  }

  @AfterClass
  public static void tearDownClass() {
    TestKit.shutdownActorSystem(system);
    system = null;
  }

  @Test
  public void givenUrl_whenStartCrawling_thenSendDownloadFile() {
    TestKit probe = new TestKit(system);

    when(crawlStatus.next()).thenReturn(Optional.of(TEST_URL));

    Function<ActorRefFactory, ActorRef> fileDownloadCreator = param -> probe.getRef();
    Function<ActorRefFactory, ActorRef> urlExtractorCreator = param -> probe.getRef();

    ActorRef crawlSupervisor = system.actorOf(CrawlSupervisor.props(
        crawlStatus,
        fileDownloadCreator, urlExtractorCreator));

    probe.send(crawlSupervisor, new StartCrawling(TEST_URL));

    probe.expectMsgEquals(new DownloadFile(TEST_URL));

    verify(crawlStatus, times(1)).add(TEST_URL);
    verify(crawlStatus, times(1)).next();
  }

  @Test
  public void givenUrl_whenFileDownloadResult_thenStartUrlExtract() {
    TestKit probe = new TestKit(system);

    Function<ActorRefFactory, ActorRef> fileDownloadCreator = param -> probe.getRef();
    Function<ActorRefFactory, ActorRef> urlExtractorCreator = param -> probe.getRef();

    ActorRef crawlSupervisor = system
        .actorOf(CrawlSupervisor.props(crawlStatus,
            fileDownloadCreator, urlExtractorCreator));

    probe.send(crawlSupervisor, new FileDownloadResult(TEST_URL, TEST_PATH));

    probe.expectMsgEquals(new ExtractUrls(TEST_URL, TEST_PATH));
  }

  @Test
  public void givenUrl_whenFileDownloadException_thenAddUrlToFailed() {
    TestKit probe = new TestKit(system);

    Function<ActorRefFactory, ActorRef> fileDownloadCreator = param -> probe.getRef();
    Function<ActorRefFactory, ActorRef> urlExtractorCreator = param -> probe.getRef();

    ActorRef crawlSupervisor = system
        .actorOf(CrawlSupervisor.props(crawlStatus,
            fileDownloadCreator, urlExtractorCreator));

    probe.send(crawlSupervisor, new FileDownloadError(TEST_URL));

    probe.expectNoMessage();

    verify(crawlStatus, times(1)).addFailed(TEST_URL);
  }

  @Test
  public void givenUrl_whenUrlsExtractedAndStatusIsFinished_thenSendCrawlFinished() {
    TestKit probe = new TestKit(system);

    Set<String> extractedUrls = Collections.emptySet();
    when(crawlStatus.isFinished()).thenReturn(Boolean.TRUE);

    Function<ActorRefFactory, ActorRef> fileDownloadCreator = param -> probe.getRef();
    Function<ActorRefFactory, ActorRef> urlExtractorCreator = param -> probe.getRef();

    ActorRef crawlSupervisor = system
        .actorOf(CrawlSupervisor.props(crawlStatus,
            fileDownloadCreator, urlExtractorCreator));

    probe.send(crawlSupervisor, new UrlsExtracted(TEST_URL, TEST_PATH,
        extractedUrls));

    probe.expectNoMessage();

    verify(crawlStatus, times(1)).addAll(extractedUrls);
    verify(crawlStatus, times(1)).addProcessed(TEST_URL);
  }

  @Test
  public void givenUrl_whenUrlsExtractedAndStatusIsNotFinished_thenSendStartCrawling() {
    TestKit probe = new TestKit(system);

    Set<String> extractedUrls = new HashSet<>(Arrays.asList("http://a.com/", "http://b.com/"));
    when(crawlStatus.isFinished()).thenReturn(Boolean.FALSE);
    when(crawlStatus.getRemaining())
        .thenReturn(extractedUrls);
    when(crawlStatus.next()).thenReturn(Optional.of("http://a.com/"))
        .thenReturn(Optional.of("http://b.com/"));

    Function<ActorRefFactory, ActorRef> fileDownloadCreator = param -> probe.getRef();
    Function<ActorRefFactory, ActorRef> urlExtractorCreator = param -> probe.getRef();

    ActorRef crawlSupervisor = system
        .actorOf(CrawlSupervisor.props(crawlStatus,
            fileDownloadCreator, urlExtractorCreator));

    probe.send(crawlSupervisor, new UrlsExtracted(TEST_URL, TEST_PATH,
        extractedUrls));

    probe.expectMsgAllOf(new DownloadFile("http://a.com/"), new DownloadFile("http://b.com/"));

    verify(crawlStatus, times(1)).addAll(extractedUrls);
    verify(crawlStatus, times(1)).addProcessed(TEST_URL);
    verify(crawlStatus, times(1)).getRemaining();
  }

  @Test
  public void givenUrl_whenCrawlFinished_thenPrintResultAndTerminate() {
    TestKit probe = new TestKit(system);

    Function<ActorRefFactory, ActorRef> fileDownloadCreator = param -> probe.getRef();
    Function<ActorRefFactory, ActorRef> urlExtractorCreator = param -> probe.getRef();

    ActorRef crawlSupervisor = system
        .actorOf(CrawlSupervisor.props(crawlStatus,
            fileDownloadCreator, urlExtractorCreator));

    probe.send(crawlSupervisor, new CrawlFinished());

    probe.expectNoMessage();

    verify(crawlStatus, times(1)).getFailed();
  }

  @Test
  public void test() {
    new TestKit(system) {{
      TestKit probe = new TestKit(system);

      when(urlExtractor.extractUrls(any(String.class), any(String.class)))
          .thenThrow(new UrlExtractException("error"));

      Function<ActorRefFactory, ActorRef> fileDownloadCreator = param -> probe.getRef();
      //      Function<ActorRefFactory, ActorRef> urlExtractorCreator = param -> probe.getRef();
      Function<ActorRefFactory, ActorRef> urlExtractorCreator = actorRefFactory -> actorRefFactory
          .actorOf(UrlExtractActor.props(TEST_URL, urlExtractor),
              "url-extract");

      ActorRef crawlSupervisor = system
          .actorOf(CrawlSupervisor.props(crawlStatus,
              fileDownloadCreator, urlExtractorCreator), "crawl-supervisor");

      //    ActorRef urlExtractorActor = (ActorRef) Await.result(ask(crawlSupervisor,
      //        UrlExtractActor.props("http://some.base.uri", urlExtractor), 5000), Duration.ofSeconds(5));
      //#create
      // probe.send(crawlSupervisor, new FileDownloadResult(TEST_URL, "/test/path"));

      // probe.expectMsgEquals(new ExtractUrls(TEST_URL, "/test/path"));
      //    ExtractUrls extractUrlsMessage = new ExtractUrls("http://some.url", "/some/path");
      //    urlExtractorActor.tell(extractUrlsMessage, probe.getRef());
      //    crawlSupervisor.tell(new Exception("test"), ActorRef.noSender());

      // probe.watch(child);
      // TODO: check that probe is restarted
      // expectMsgClass(UrlExtractException.class);
      // probe.awaitAssert(() ->)
      //#resume

      // probe.send(crawlSupervisor, new FileDownloadResult(TEST_URL, "/test/path"));

      //      final boolean result = new EventFilter(Logging.Error.class, system)
      //          .from("akka://test-system/user/crawl-supervisor")
      //          .message("Url extraction error: error")
      //          .occurrences(1)
      //          .intercept(() -> {
      //            probe.send(crawlSupervisor, new FileDownloadResult(TEST_URL, "/test/path"));
      //            return true;
      //          });

      //      final boolean result = new EventFilter(UrlExtractException.class, system)
      //          .from("akka://test-system/user/crawl-supervisor")
      //          .message("error")
      //          .occurrences(1)
      //          .intercept(() -> {
      //            probe.send(crawlSupervisor, new FileDownloadResult(TEST_URL, "/test/path"));
      //            return true;
      //          });

      //      probe.send(crawlSupervisor, new FileDownloadResult(TEST_URL, "/test/path"));

      probe.send(crawlSupervisor, new FileDownloadResult(TEST_URL, TEST_PATH));

      final boolean result = new EventFilter(UrlExtractException.class, system)
          //          .occurrences(1)
          .intercept(() -> {

            return true;
          });

      expectNoMessage();

      assertThat(result).isTrue();

      //    child.tell(42, ActorRef.noSender());
      //    assert Await.result(ask(child, "get", 5000), timeout).equals(42);
      //    child.tell(new ArithmeticException(), ActorRef.noSender());
      //    assert Await.result(ask(child, "get", 5000), timeout).equals(42);
      //    //#resume
      //
      //    //#restart
      //    child.tell(new NullPointerException(), ActorRef.noSender());
      //    assert Await.result(ask(child, "get", 5000), timeout).equals(0);
    }};
  }
}