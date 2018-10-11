package io.sixhours.crawler.extractor;

import java.util.Set;
import lombok.Value;

/**
 * Result when extracting the urls using {@code UrlExtractor}.
 *
 * @author Mladen Bolic
 */
@Value
public class UrlExtractResult {

  private final Set<String> urls;
}
