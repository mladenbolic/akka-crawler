package io.sixhours.crawler.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * Test class for {@code ArgsHandler}.
 *
 * @author Mladen Bolic
 */
public class ArgsHandlerTest {

  @Test
  public void givenEmptyArgs_whenGetUrl_thenReturnDefaultUrl() {
    String[] args = new String[0];

    String result = ArgsHandler.getUrl(args);

    assertThat(result).isEqualTo(ArgsHandler.DEFAULT_URL);
  }

  @Test
  public void givenNotEmptyArgs_whenGetUrl_thenReturnUrl() {
    String[] args = new String[]{"http:/some.url"};

    String result = ArgsHandler.getUrl(args);

    assertThat(result).isEqualTo("http:/some.url");
  }
}