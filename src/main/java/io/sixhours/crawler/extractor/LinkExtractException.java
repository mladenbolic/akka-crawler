package io.sixhours.crawler.extractor;

/**
 * Class representing url extract exception.
 *
 * @author Mladen Bolic
 */
public class LinkExtractException extends RuntimeException {

  public static final long serialVersionUID = -3387514328743229948L;

  public LinkExtractException(String message, Throwable cause) {
    super(message, cause);
  }

  public LinkExtractException(String message) {
    super(message);
  }
}
