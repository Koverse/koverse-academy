package com.koverse.addon.cui;


import com.koverse.sdk.security.SecurityLabelParser;
import com.koverse.sdk.security.SecurityLabelParserException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Example of how labels might be parsed into security label expressions.
 */
public class TitanicLabelParser implements SecurityLabelParser {

  private static final Map<String, String> CUI_MAP = new HashMap<>();

  static {
    CUI_MAP.put("PHYS", "PHYS");
    CUI_MAP.put("FSEC", "FSEC");
    CUI_MAP.put("REL TO USA", "US");
    CUI_MAP.put("REL TO NORAMER", "(US|CAN|MEX)");
    // TODO: fill this out with all labels
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
    return "0.1.0";
  }

  @Override
  public String toExpression(String s) throws SecurityLabelParserException {

    // this parses simple CUI expressions in a limited way
    // we expect to get paragraphs from the PortionHandler here

    // get the first line
    String line = s.split("\n")[0];

    List<String> parts = Arrays.asList(line.split("/"));

    // check for CUI here or some other top-level system to choose how to parse the rest
    if (parts.get(0).equals("CUI")) {

      Stream<String> tokens = parts.subList(2, parts.size()).stream().map(p -> CUI_MAP.get(p));

      // we'll simply AND these for now. Future implementations can implement ORs when split according to CUI
      // TODO: implement parsing for ORs
      return tokens.collect(Collectors.joining("&"));
    }

    return "";
  }
}
