package io.sixhours.crawler.supervisor;

import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

/**
 * Class holding the crawling status.
 *
 * @author Mladen Bolic
 */
public class CrawlStatus {

  private final Set<String> total = new HashSet<>();
  private final Set<String> remaining = new HashSet<>();
  private final Set<String> processing = new HashSet<>();
  private final Set<String> processed = new HashSet<>();
  private final Set<String> failed = new HashSet<>();

  public void add(String url) {
    if (!total.contains(url)) {
      remaining.add(url);
      total.add(url);
    }
  }

  public void addAll(Collection<String> urls) {
    for (String url : urls) {
      add(url);
    }
  }

  public void addProcessed(String url) {
    processing.remove(url);
    processed.add(url);
  }

  public void addFailed(String url) {
    processing.remove(url);
    failed.add(url);
  }

  @CheckReturnValue
  public Set<String> getRemaining() {
    return remaining;
  }

  @CheckReturnValue
  public Set<String> getFailed() {
    return failed;
  }

  public Optional<String> next() {
    Iterator<String> iterator = remaining.iterator();

    if (!remaining.isEmpty() && iterator.hasNext()) {
      String url = iterator.next();
      remaining.remove(url);
      processing.add(url);

      return Optional.of(url);
    }

    return Optional.empty();
  }

  public boolean isFinished() {
    return remaining.isEmpty() && processing.isEmpty();
  }

  public String print() {
    return String
        .format("Total: %s, Processed: %s, In Progress: %s, Failed: %s",
            total.size(), processed.size(), processing.size(), failed.size());
  }
}
