package io.sixhours.crawler;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorRefFactory;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import io.sixhours.crawler.CrawlSupervisor.CrawlFinished;
import io.sixhours.crawler.CrawlSupervisor.StartCrawling;
import io.sixhours.crawler.downloader.FileDownloadActor.DownloadFile;
import io.sixhours.crawler.downloader.FileDownloadActor.FileDownloadError;
import io.sixhours.crawler.downloader.FileDownloadActor.FileDownloadResult;
import io.sixhours.crawler.extractor.UrlExtractActor.ExtractUrls;
import io.sixhours.crawler.extractor.UrlExtractActor.UrlsExtracted;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CrawlSupervisorTest {

  private static final String TEST_URL = "http://test.url";
  private static final String TEST_PATH = "/test/path";

  @Mock
  private CrawlStatus crawlStatus;

  static ActorSystem system;

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
  public void givenUrl_whenStartCrawling_thenSendDownloadFile() {
    TestKit probe = new TestKit(system);

    when(crawlStatus.getNext()).thenReturn("http://test.url/");

    Function<ActorRefFactory, ActorRef> fileDownloadCreator = param -> probe.getRef();
    Function<ActorRefFactory, ActorRef> urlExtractorCreator = param -> probe.getRef();

    ActorRef crawlSupervisor = system.actorOf(CrawlSupervisor.props("http://test.url/",
        crawlStatus,
        fileDownloadCreator, urlExtractorCreator));

    probe.send(crawlSupervisor, new StartCrawling("http://test.url/"));

    probe.expectMsgEquals(new DownloadFile("http://test.url/"));

    verify(crawlStatus, times(1)).add("http://test.url/");
    verify(crawlStatus, times(1)).getNext();
  }

  @Test
  public void givenUrl_whenFileDownloadResult_thenStartUrlExtract() {
    TestKit probe = new TestKit(system);

    Function<ActorRefFactory, ActorRef> fileDownloadCreator = param -> probe.getRef();
    Function<ActorRefFactory, ActorRef> urlExtractorCreator = param -> probe.getRef();

    ActorRef crawlSupervisor = system
        .actorOf(CrawlSupervisor.props("http://base.url/", crawlStatus,
            fileDownloadCreator, urlExtractorCreator));

    probe.send(crawlSupervisor, new FileDownloadResult("http://test.url/", "/test/path"));

    probe.expectMsgEquals(new ExtractUrls("http://test.url/", "/test/path", "http://base.url/"));
  }

  @Test
  public void givenUrl_whenFileDownloadException_thenAddUrlToFailed() {
    TestKit probe = new TestKit(system);

    Function<ActorRefFactory, ActorRef> fileDownloadCreator = param -> probe.getRef();
    Function<ActorRefFactory, ActorRef> urlExtractorCreator = param -> probe.getRef();

    ActorRef crawlSupervisor = system
        .actorOf(CrawlSupervisor.props("http://test.url/", crawlStatus,
            fileDownloadCreator, urlExtractorCreator));

    probe.send(crawlSupervisor, new FileDownloadError("http://test.url/"));

    probe.expectNoMessage();

    verify(crawlStatus, times(1)).addFailed("http://test.url/");
  }

  @Test
  public void givenUrl_whenUrlsExtractedAndStatusIsFinished_thenSendCrawlFinished() {
    TestKit probe = new TestKit(system);

    Set<String> extractedUrls = Collections.emptySet();
    when(crawlStatus.isFinished()).thenReturn(Boolean.TRUE);

    Function<ActorRefFactory, ActorRef> fileDownloadCreator = param -> probe.getRef();
    Function<ActorRefFactory, ActorRef> urlExtractorCreator = param -> probe.getRef();

    ActorRef crawlSupervisor = system
        .actorOf(CrawlSupervisor.props("http://test.url/", crawlStatus,
            fileDownloadCreator, urlExtractorCreator));

    probe.send(crawlSupervisor, new UrlsExtracted("http://test.url/", "/test/path",
        extractedUrls));

    probe.expectNoMessage();

    verify(crawlStatus, times(1)).addAll(extractedUrls);
    verify(crawlStatus, times(1)).addProcessed("http://test.url/");
  }

  @Test
  public void givenUrl_whenUrlsExtractedAndStatusIsNotFinished_thenSendStartCrawling() {
    TestKit probe = new TestKit(system);

    Set<String> extractedUrls = new HashSet<>(Arrays.asList("http://a.com/", "http://b.com/"));
    when(crawlStatus.isFinished()).thenReturn(Boolean.FALSE);
    when(crawlStatus.getPagesToVisit())
        .thenReturn(extractedUrls);
    when(crawlStatus.getNext()).thenReturn("http://a.com/").thenReturn("http://b.com/");

    Function<ActorRefFactory, ActorRef> fileDownloadCreator = param -> probe.getRef();
    Function<ActorRefFactory, ActorRef> urlExtractorCreator = param -> probe.getRef();

    ActorRef crawlSupervisor = system
        .actorOf(CrawlSupervisor.props("http://test.url/", crawlStatus,
            fileDownloadCreator, urlExtractorCreator));

    probe.send(crawlSupervisor, new UrlsExtracted("http://test.url/", "/test/path",
        extractedUrls));

    probe.expectMsgAllOf(new DownloadFile("http://a.com/"), new DownloadFile("http://b.com/"));

    verify(crawlStatus, times(1)).addAll(extractedUrls);
    verify(crawlStatus, times(1)).addProcessed("http://test.url/");
    verify(crawlStatus, times(1)).getPagesToVisit();
  }

  @Test
  public void givenUrl_whenCrawlFinished_thenPrintResultAndTerminate() {
    TestKit probe = new TestKit(system);

    Function<ActorRefFactory, ActorRef> fileDownloadCreator = param -> probe.getRef();
    Function<ActorRefFactory, ActorRef> urlExtractorCreator = param -> probe.getRef();

    ActorRef crawlSupervisor = system
        .actorOf(CrawlSupervisor.props("http://test.url/", crawlStatus,
            fileDownloadCreator, urlExtractorCreator));

    probe.send(crawlSupervisor, new CrawlFinished());

    probe.expectNoMessage();

    verify(crawlStatus, times(1)).getFailed();
  }
}