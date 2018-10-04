package io.sixhours.crawler.downloader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

/**
 * @author
 */
@Slf4j
public class FileDownloaderImpl implements FileDownloader {

  private static final String DOWNLOAD_DIR = "/Users/mladen/Downloads/xxx/";
  private static final String INDEX_NAME = "index";
  private static final String SLASH = "/";
  private static final String HTML_EXTENSION = ".html";

  @Override
  public FileDownloadResult downloadFile(String baseUrl, String url)
      throws FileDownloadException {
    URL fileUrl;
    String filePath;
    InputStream inputStream = null;

    try {
      fileUrl = new URL(url);
      filePath = getFilePath(fileUrl);

      inputStream = fileUrl.openStream();

      createFileIfNotExists(filePath);
    } catch (IOException e) {
      throw new FileDownloadException(e.getMessage(), e);
    }

    try (
        ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream);
        FileOutputStream fileOutputStream = new FileOutputStream(
            DOWNLOAD_DIR + filePath);
        FileChannel fileChannel = fileOutputStream.getChannel()
    ) {

      fileChannel
          .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

      return new FileDownloadResult(url, DOWNLOAD_DIR + filePath);
    } catch (IOException e) {
      throw new FileDownloadException(e.getMessage(), e);
    } finally {
      try {
        inputStream.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void createFileIfNotExists(String filePath) throws IOException {
    Path f = Paths.get(DOWNLOAD_DIR + filePath);
    if (!Files.exists(f) && !Files.isDirectory(f)) {
      Files.createDirectories(f.getParent());
      Files.createFile(f);
    }
  }

  private String getFilePath(URL url) {
    String filePath = url.getPath();
    filePath = (SLASH.equals(filePath)) ? INDEX_NAME : filePath;

    String fileExtension = FilenameUtils.getExtension(filePath);
    if (Objects.isNull(fileExtension) || "".equals(fileExtension)){
      filePath = filePath + HTML_EXTENSION;
    }

    return filePath;
  }
}
