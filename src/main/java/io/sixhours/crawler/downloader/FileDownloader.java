package io.sixhours.crawler.downloader;

import io.sixhours.crawler.downloader.FileDownloadActor.FileDownloadResult;

/**
 * File downloader used for downloading files from specified url.
 *
 * @author Mladen Bolic
 */
public interface FileDownloader {

  /**
   * Downloads file from specified url.
   *
   * @param url the url location of the file
   * @return File download result
   * @throws FileDownloadException custom exception during file download
   */
  FileDownloadResult downloadFile(String url) throws FileDownloadException;
}
