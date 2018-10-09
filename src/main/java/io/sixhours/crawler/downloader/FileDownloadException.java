package io.sixhours.crawler.downloader;

/**
 * Class representing file download exception.
 *
 * @author Mladen Bolic
 */
public class FileDownloadException extends Exception {

  public static final long serialVersionUID = -3387514328743229948L;

  public FileDownloadException(String message, Throwable cause) {
    super(message, cause);
  }

  public FileDownloadException(String message) {
    super(message);
  }
}
