package com.koverse.titanic;

import com.koverse.sdk.security.SecurityLabelParser;
import com.koverse.sdk.security.SecurityLabelParserException;

import java.util.HashMap;
import java.util.Map;

/**
 * Example of how labels might be parsed into security label expressions.
 */
public class TitanicLabelParser implements SecurityLabelParser {

  private static final Map<String, String> PASSENGER_MAP = new HashMap<>();

  static {
    PASSENGER_MAP.put("1", "SECRET");
    PASSENGER_MAP.put("2", "FOUO");
    PASSENGER_MAP.put("3", "U");
  }

  @Override
  public String getDisplayName() {
    return "Titanic Label Parser";
  }

  @Override
  public String getDescription() {
    return "Parse titanic passengers into security labels based on their class";
  }

  @Override
  public String getTypeId() {
    return "titanic-label-parser";
  }

  @Override
  public String getVersion() {
    return "0.1.2";
  }

  @Override
  public String toExpression(String s) throws SecurityLabelParserException {

    // this parses simple expressions in a limited way

    return PASSENGER_MAP.get(s);
  }
}
