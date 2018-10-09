package io.sixhours.crawler.downloader;

import io.sixhours.crawler.downloader.FileDownloadActor.FileDownloadResult;

/**
 * @author
 */
public interface FileDownloader {

  FileDownloadResult downloadFile(String currentUrl) throws FileDownloadException;
}
