package com.koverse.titanic;

import static org.junit.Assert.assertEquals;

import com.koverse.sdk.security.SecurityLabelParserException;

import org.junit.Test;

public class TitanicLabelParserTest {

  @Test
  public void testLabelParsing() throws SecurityLabelParserException {
    String paragraph1 = "1";

    String paragraph2 = "2";

    String paragraph3 = "3";

    TitanicLabelParser parser = new TitanicLabelParser();

    String expression1 = parser.toExpression(paragraph1);
    String expression2 = parser.toExpression(paragraph2);
    String expression3 = parser.toExpression(paragraph3);

    assertEquals("SECRET", expression1);
    assertEquals("FOUO", expression2);
    assertEquals("U", expression3);
  }
}
