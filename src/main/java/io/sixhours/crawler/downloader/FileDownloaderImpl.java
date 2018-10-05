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
  public static final String EMPTY_STRING = "";

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

  private void createFileIfNotExists(String fileUri) throws IOException {
    Path filePath = Paths.get(DOWNLOAD_DIR + fileUri);
    Path parentPath = filePath.getParent();
    if (!Files.exists(filePath) && !Files.isDirectory(filePath)) {
      if (parentPath != null) {
        Files.createDirectories(parentPath);
      }
      Files.createFile(filePath);
    }
  }

  private String getFilePath(URL url) {
    String filePath = url.getPath();
    filePath = (SLASH.equals(filePath)) ? INDEX_NAME : filePath;

    String fileExtension = FilenameUtils.getExtension(filePath);
    if (Objects.isNull(fileExtension) || EMPTY_STRING.equals(fileExtension)) {
      filePath = filePath + HTML_EXTENSION;
    }

    return filePath;
  }
}
