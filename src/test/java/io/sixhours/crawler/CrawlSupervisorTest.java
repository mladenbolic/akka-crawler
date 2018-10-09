package io.sixhours.crawler;

import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class CrawlSupervisorTest {

  private static final String TEST_URL = "http://test.url";
  private static final String TEST_PATH = "/test/path";

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

//  @Ignore
//  public void givenUrl_whenFileDownload_thenReturnDownloadResult() throws Exception {
//    TestKit probe = new TestKit(system);
//
//    TestActorRef<CrawlSupervisor> crawlSupervisorActor = TestActorRef.create(system
//        ,CrawlSupervisor.props("http://www.burgerking.no/"), CrawlSupervisor.NAME);
//
//
//
//    // ActorRef crawlSupervisorActor = system.actorOf(CrawlSupervisor.props("http://www.burgerking.no/"));
//    // crawlSupervisorActor.tell(new StartCrawling("http://www.burgerking.no/"), Actor.noSender());
//
//    // check that when we call startcrawling that
//    // fileDownloaderActor.tell(new DownloadFile(crawlStatus.getNext()), getSelf());
//    // is called
//
//    new TestKit(system) {{
//      getRef().tell(42, ActorRef.noSender());
//      awaitCond(Duration.ofSeconds(1), Duration.ofMillis(100), this::msgAvailable);
//    }};
//
//    final CompletableFuture<Object> future = PatternsCS
//        .ask(crawlSupervisorActor, new StartCrawling("http://www.burgerking.no/"), 3000)
//        .toCompletableFuture();
//
//    assertThat(future.isDone()).isTrue();
//    assertThat(future.get()).isEqualTo(42);
//  }

//  @Test
//  public void givenUrl_whenFileDownloadException_thenReturnErrorResult() throws Exception {
//    TestKit probe = new TestKit(system);
//    when(fileDownloader.downloadFile(any(String.class)))
//        .thenThrow(new FileDownloadException("error"));
//
//    ActorRef fileDownloaderActor = system.actorOf(FileDownloadActor.props(fileDownloader));
//
//    fileDownloaderActor.tell(new DownloadFile(TEST_URL), probe.getRef());
//
//    FileDownloadError response = probe.expectMsgClass(FileDownloadError.class);
//
//    verify(fileDownloader, times(1))
//        .downloadFile(any(String.class));
//    assertThat(response.getUrl()).isEqualTo(TEST_URL);
//  }
}