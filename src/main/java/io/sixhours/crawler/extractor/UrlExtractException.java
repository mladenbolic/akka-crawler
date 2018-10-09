package io.sixhours.crawler.extractor;

/**
 * Class representing url extract exception.
 *
 * @author Mladen Bolic
 */
public class UrlExtractException extends Exception {

  public static final long serialVersionUID = -3387514328743229948L;

  public UrlExtractException(String message, Throwable cause) {
    super(message, cause);
  }

  public UrlExtractException(String message) {
    super(message);
  }
}
