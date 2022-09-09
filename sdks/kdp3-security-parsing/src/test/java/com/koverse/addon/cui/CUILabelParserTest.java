package com.koverse.addon.cui;

import static org.junit.Assert.assertEquals;

import com.koverse.sdk.security.SecurityLabelParserException;

import org.junit.Test;

public class CUILabelParserTest {

  @Test
  public void testLabelParsing() throws SecurityLabelParserException {
    String paragraph1 = "CUI//PHYS/REL TO USA\n"
        + "This information is specific to physical infrastructure that only US citizens are allowed to see.";

    String paragraph2 = "CUI//PHYS\n"
        + "This information is specific to physical infrastructure.";

    String paragraph3 = "CUI//FSEC\n"
        + "This information is also about physical infrastructure.";

    String paragraph4 = "CUI//PHYS/REL TO NORAMER\n"
            + "This information is also about physical infrastructure.";

    CUILabelParser parser = new CUILabelParser();

    String expression1 = parser.toExpression(paragraph1);
    String expression2 = parser.toExpression(paragraph2);
    String expression3 = parser.toExpression(paragraph3);
    String expression4 = parser.toExpression(paragraph4);

    assertEquals("PHYS&US", expression1);
    assertEquals("PHYS", expression2);
    assertEquals("FSEC", expression3);
    assertEquals("PHYS&(US|CAN|MEX)", expression4);

  }
}
