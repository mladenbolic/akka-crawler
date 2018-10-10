package io.sixhours.crawler;

import java.util.Collection;
import java.util.Set;

/**
 * @author
 */
public interface CrawlStatus {

  void add(String page);

  void addAll(Collection<String> pages);

  void addFailed(String page);

  void addProcessed(String page);

  Set<String> getFailed();
  Set<String> getPagesToVisit();

  String getNext();

  Collection<String> getNextBatch();

  boolean isFinished();
}
