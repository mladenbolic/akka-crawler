package io.sixhours.crawler.downloader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author
 */
public class FileDownloaderImpl implements FileDownloader {

  private static final String DOWNLOAD_DIR = "/Users/mladen/Downloads/xxx/";
  private static final String INDEX_HTML = "index.html";
  private static final String SLASH = "/";

  @Override
  public FileDownloadResult downloadFile(String baseUrl, String currentUrl) throws FileDownloadException {
    URL fileUrl;
    String filePath;

    try {
      fileUrl = new URL(currentUrl);
      filePath = getFilePath(fileUrl);

      createFileIfNotExists(filePath);
    } catch (IOException e) {
      throw new FileDownloadException(e.getMessage(), e);
    }

    try (
        ReadableByteChannel readableByteChannel = Channels.newChannel(fileUrl.openStream());
        FileOutputStream fileOutputStream = new FileOutputStream(
            DOWNLOAD_DIR + filePath);
    ) {

      fileOutputStream.getChannel()
          .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

      return new FileDownloadResult(filePath);
    } catch (IOException e) {
      throw new FileDownloadException(e.getMessage(), e);
    }
  }

  private void createFileIfNotExists(String filePath) throws IOException {
    Path f = Paths.get(DOWNLOAD_DIR + filePath);
    if (!Files.exists(f)) {
      Files.createDirectories(f.getParent());
      Files.createFile(f);
    }
  }

  private String getFilePath(URL url) {
    String filePath = url.getPath();
    filePath = (SLASH.equals(filePath)) ? INDEX_HTML : filePath;

    return filePath;
  }
}
