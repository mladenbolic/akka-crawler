package io.sixhours.crawler.extractor;

import java.util.Set;
import lombok.Value;

/**
 * @author
 */
@Value
public class UrlExtractResult {
  private final Set<String> urls;
}
