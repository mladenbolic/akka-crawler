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
  private Set<String> allPages = new HashSet<>();
  private Set<String> inProgress = new HashSet<>();
  private Set<String> failedPages = new HashSet<>();
  private Set<String> processedPages = new HashSet<>();

  public void add(String page) {
    if (!allPages.contains(page)) {
      pagesToVisit.add(page);
      allPages.add(page);
    }
  }

  public void addAll(Collection<String> pages) {
    for (String page : pages) {
      add(page);
    }
  }

  public void addFailed(String page) {
    inProgress.remove(page);
    failedPages.add(page);
  }

  public void processed(String page) {
    inProgress.remove(page);
    processedPages.add(page);
  }

  public String getNext() {
    if (pagesToVisit.isEmpty()) {
      return null;
    } else {
      String next = pagesToVisit.iterator().next();
      pagesToVisit.remove(next);
      inProgress.add(next);
      return next;
    }
  }

  public Collection<String> getNextBatch() {
    Set<String> pages = new HashSet<>();
    pages.addAll(pagesToVisit);
    pagesToVisit.clear();
    inProgress.addAll(pages);
    return pages;
  }

  public int size() {
    return allPages.size();
  }

  public int processedSize() {
    return processedPages.size();
  }

  public Set<String> getProgressed() {
    return this.inProgress;
  }

  public boolean isFinished() {
    return pagesToVisit.isEmpty() && inProgress.isEmpty();
  }

  @Override
  public String toString() {
    return String
        .format("Total: %1$3s, Processed: %2$3s, In Progress: %3$3s",
            allPages.size(), processedPages.size(), inProgress.size());
  }
}
