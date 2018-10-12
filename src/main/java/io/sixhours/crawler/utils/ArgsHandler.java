package io.sixhours.crawler.utils;

import lombok.experimental.UtilityClass;

/**
 * Utility class for reading program input arguments.
 *
 * @author Mladen Bolic
 */
@SuppressWarnings("PMD.UseUtilityClass")
@UtilityClass
public class ArgsHandler {

  static final String DEFAULT_URL = "http://www.burgerking.no/";

  /**
   * Reads string from first input argument and returns its value as result.
   *
   * <p>If input argument is not specified the default input argument will be used.
   *
   * @param args input arguments
   * @return url
   */
  public static String getUrl(String[] args) {
    String url;
    if (args.length == 0) {
      url = DEFAULT_URL;
    } else {
      url = args[0];
    }
    return url;
  }
}