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

  /**
   * Adds given url to the list of total and remaing urls.
   *
   * <p>If url already exists, nothing will be added.
   *
   * @param url the url we are going to save to crawl status
   */
  public void add(String url) {
    if (!total.contains(url)) {
      total.add(url);
      remaining.add(url);
    }
  }

  public void addAll(Collection<String> urls) {
    urls.forEach(this::add);
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
  public Set<String> getTotal() {
    return total;
  }

  @CheckReturnValue
  public Set<String> getRemaining() {
    return remaining;
  }

  @CheckReturnValue
  public Set<String> getProcessing() {
    return processing;
  }

  @CheckReturnValue
  public Set<String> getProcessed() {
    return processed;
  }

  @CheckReturnValue
  public Set<String> getFailed() {
    return failed;
  }

  /**
   * Returns {@code Opttional} of next url that should be processed.
   *
   * <p>If all urls are processed, {@code Optional.empty()} is returned.
   *
   * @return {@code Optional} containing next url
   */
  public Optional<String> next() {
    Iterator<String> iterator = remaining.iterator();

    if (!remaining.isEmpty() && iterator.hasNext()) {
      String url = iterator.next();
      iterator.remove();
      processing.add(url);

      return Optional.of(url);
    }

    return Optional.empty();
  }

  /**
   * Returns batch of urls that should be processed.
   *
   * @return batch of urls
   */
  public Set<String> nextBatch() {
    Set<String> result = new HashSet<>(remaining);
    processing.addAll(remaining);
    remaining.clear();

    return result;
  }

  public boolean isFinished() {
    return processing.isEmpty() && remaining.isEmpty();
  }

  /**
   * Prints current crawl status.
   *
   * @return string containing current crawl status
   */
  public String print() {
    return String
        .format(
            "Total: %s, Processed: %s, In Progress: %s, Remaining: %s, Failed: %s",
            total.size(), processed.size(), processing.size(), remaining.size(), failed.size());
  }
}
