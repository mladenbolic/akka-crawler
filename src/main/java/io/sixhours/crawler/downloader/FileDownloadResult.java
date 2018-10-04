package io.sixhours.crawler.downloader;

import lombok.Value;

/**
 * @author
 */
@Value
public class FileDownloadResult {
  private final String url;
  private final String path;
}
