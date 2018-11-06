package io.sixhours.crawler.extractor;

import java.util.Set;
import lombok.Value;

/**
 * Result when extracting the urls using {@code LinkExtractor}.
 *
 * @author Mladen Bolic
 */
@Value
public class LinkExtractResult {

  private final Set<String> urls;
}
