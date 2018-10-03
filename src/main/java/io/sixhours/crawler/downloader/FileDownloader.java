package io.sixhours.crawler.downloader;

/**
 * @author
 */
public interface FileDownloader {
  FileDownloadResult downloadFile(String baseUrl, String currentUrl) throws FileDownloadException;
}
