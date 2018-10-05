package io.sixhours.crawler;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.Value;

/**
 * @author
 */
@Value
public class CrawlStatus {

  private Set<String> pagesToVisit = new HashSet<>();
  private Set<String> total = new HashSet<>();
  private Set<String> processing = new HashSet<>();
  private Set<String> processed = new HashSet<>();
  private Set<String> failed = new HashSet<>();

  public void add(String page) {
    if (!total.contains(page)) {
      pagesToVisit.add(page);
      total.add(page);
    }
  }

  public void addAll(Collection<String> pages) {
    for (String page : pages) {
      add(page);
    }
  }

  public void addFailed(String page) {
    processing.remove(page);
    failed.add(page);
  }

  public void addProcessed(String page) {
    processing.remove(page);
    processed.add(page);
  }

  public String getNext() {
    if (pagesToVisit.isEmpty()) {
      return null;
    } else {
      String next = pagesToVisit.iterator().next();
      pagesToVisit.remove(next);
      processing.add(next);
      return next;
    }
  }

  public Collection<String> getNextBatch() {
    Set<String> pages = new HashSet<>();
    pages.addAll(pagesToVisit);
    pagesToVisit.clear();
    processing.addAll(pages);
    return pages;
  }

  public boolean isFinished() {
    return pagesToVisit.isEmpty() && processing.isEmpty();
  }

  @Override
  public String toString() {
    return String
        .format("Total: %1$3s, Processed: %2$3s, In Progress: %3$3s",
            total.size(), processed.size(), processing.size());
  }
}
