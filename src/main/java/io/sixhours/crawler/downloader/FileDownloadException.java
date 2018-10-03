package io.sixhours.crawler.downloader;

/**
 * @author
 */
public class FileDownloadException extends Exception {

  public FileDownloadException(String message, Throwable cause) {
    super(message, cause);
  }

  public FileDownloadException(String message) {
    super(message);
  }
}
