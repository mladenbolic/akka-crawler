package io.sixhours.crawler.supervisor;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Crawl status holder.
 *
 * <p>Using this interface we should have precises control on keeping track of current crawler
 * state.
 *
 * @author Mladen Bolic
 */
public interface CrawlStatusTmp {

  void add(String page);

  void addAll(Collection<String> pages);

  void addProcessed(String page);

  void addFailed(String page);

  Set<String> getRemaining();

  Set<String> getFailed();

  Optional<String> next();

  boolean isFinished();

  String print();
}
