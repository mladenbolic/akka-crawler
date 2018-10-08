package io.sixhours.crawler.downloader;

/**
 * @author
 */
public interface FileDownloader {

  FileDownloadResult downloadFile(String currentUrl) throws FileDownloadException;
}
