package io.sixhours.crawler.supervisor;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import akka.actor.AbstractActor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorRefFactory;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.ConfigFactory;
import io.sixhours.crawler.downloader.FileDownloadActor.DownloadFile;
import io.sixhours.crawler.downloader.FileDownloadActor.FileDownloadError;
import io.sixhours.crawler.downloader.FileDownloadActor.FileDownloadResult;
import io.sixhours.crawler.extractor.LinkExtractActor.ExtractLinks;
import io.sixhours.crawler.extractor.LinkExtractActor.LinksExtracted;
import io.sixhours.crawler.supervisor.CrawlSupervisor.CrawlFinished;
import io.sixhours.crawler.supervisor.CrawlSupervisor.StartCrawling;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
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

  private static ActorSystem system;

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
    Function<ActorRefFactory, ActorRef> linkExtractorCreator = param -> probe.getRef();
    Consumer<ActorContext> terminate = context -> {
    };

    ActorRef crawlSupervisor = system.actorOf(CrawlSupervisor.props(
        crawlStatus,
        fileDownloadCreator, linkExtractorCreator, terminate));

    probe.send(crawlSupervisor, new StartCrawling(TEST_URL));

    probe.expectMsgEquals(new DownloadFile(TEST_URL));

    verify(crawlStatus, times(1)).add(TEST_URL);
    verify(crawlStatus, times(1)).next();
  }

  @Test
  public void givenUrl_whenFileDownloadResult_thenStartLinkExtract() {
    TestKit probe = new TestKit(system);

    Function<ActorRefFactory, ActorRef> fileDownloadCreator = param -> probe.getRef();
    Function<ActorRefFactory, ActorRef> linkExtractorCreator = param -> probe.getRef();
    Consumer<ActorContext> terminate = context -> {
    };

    ActorRef crawlSupervisor = system
        .actorOf(CrawlSupervisor.props(crawlStatus,
            fileDownloadCreator, linkExtractorCreator, terminate));

    probe.send(crawlSupervisor, new FileDownloadResult(TEST_URL, TEST_PATH));

    probe.expectMsgEquals(new ExtractLinks(TEST_URL, TEST_PATH));
  }

  @Test
  public void givenUrl_whenFileDownloadException_thenAddUrlToFailed() {
    TestKit probe = new TestKit(system);

    Function<ActorRefFactory, ActorRef> fileDownloadCreator = param -> probe.getRef();
    Function<ActorRefFactory, ActorRef> linkExtractorCreator = param -> probe.getRef();
    Consumer<ActorContext> terminate = context -> {
    };

    ActorRef crawlSupervisor = system
        .actorOf(CrawlSupervisor.props(crawlStatus,
            fileDownloadCreator, linkExtractorCreator, terminate));

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
    Function<ActorRefFactory, ActorRef> linkExtractorCreator = param -> probe.getRef();
    Consumer<ActorContext> terminate = context -> {
    };

    ActorRef crawlSupervisor = system
        .actorOf(CrawlSupervisor.props(crawlStatus,
            fileDownloadCreator, linkExtractorCreator, terminate));

    probe.send(crawlSupervisor, new LinksExtracted(TEST_URL, TEST_PATH,
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
    when(crawlStatus.nextBatch())
        .thenReturn(extractedUrls);

    Function<ActorRefFactory, ActorRef> fileDownloadCreator = param -> probe.getRef();
    Function<ActorRefFactory, ActorRef> linkExtractorCreator = param -> probe.getRef();
    Consumer<ActorContext> terminate = context -> {
    };

    ActorRef crawlSupervisor = system
        .actorOf(CrawlSupervisor.props(crawlStatus,
            fileDownloadCreator, linkExtractorCreator, terminate));

    probe.send(crawlSupervisor, new LinksExtracted(TEST_URL, TEST_PATH,
        extractedUrls));

    probe.expectMsgAllOf(new DownloadFile("http://a.com/"), new DownloadFile("http://b.com/"));

    verify(crawlStatus, times(1)).addAll(extractedUrls);
    verify(crawlStatus, times(1)).addProcessed(TEST_URL);
    verify(crawlStatus, times(1)).nextBatch();
  }

  @Test
  public void givenUrl_whenCrawlFinished_thenPrintResultAndTerminate() {
    TestKit probe = new TestKit(system);

    Function<ActorRefFactory, ActorRef> fileDownloadCreator = param -> probe.getRef();
    Function<ActorRefFactory, ActorRef> linkExtractorCreator = param -> probe.getRef();
    Consumer<ActorContext> terminate = context -> {
    };

    ActorRef crawlSupervisor = system
        .actorOf(CrawlSupervisor.props(crawlStatus,
            fileDownloadCreator, linkExtractorCreator, terminate));

    probe.send(crawlSupervisor, new CrawlFinished());

    probe.expectNoMessage();

    verify(crawlStatus, times(1)).getFailed();
  }
}