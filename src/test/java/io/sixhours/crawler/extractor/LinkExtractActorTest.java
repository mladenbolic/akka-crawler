package io.sixhours.crawler.extractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import io.sixhours.crawler.extractor.LinkExtractActor.ExtractLinks;
import io.sixhours.crawler.extractor.LinkExtractActor.LinksExtracted;
import java.util.Collections;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test class for {@code LinkExtractActor}.
 *
 * @author Mladen Bolic
 */
@RunWith(MockitoJUnitRunner.class)
public class LinkExtractActorTest {

  private static ActorSystem system;

  @Mock
  private LinkExtractor linkExtractor;

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
  public void givenUrl_whenUrlExtract_thenReturnExtractedUrls() {
    when(linkExtractor.extractLinks(any(String.class), any(String.class)))
        .thenReturn(new LinkExtractResult(Collections.emptySet()));

    TestKit probe = new TestKit(system);
    ActorRef urlExtractorActor = system
        .actorOf(LinkExtractActor.props("http://some.base.uri", linkExtractor));

    ExtractLinks extractLinksMessage = new ExtractLinks("http://some.url", "/some/path");
    urlExtractorActor.tell(extractLinksMessage, probe.getRef());

    LinksExtracted response = probe.expectMsgClass(LinksExtracted.class);

    verify(linkExtractor, times(1))
        .extractLinks(any(String.class), any(String.class));
    assertThat(response.getUrl()).isEqualTo("http://some.url");
    assertThat(response.getPath()).isEqualTo("/some/path");
    assertThat(response.getNewUrls()).isEqualTo(Collections.emptySet());
  }
}