package com.koverse.addon.cui;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import com.koverse.sdk.data.SimpleRecord;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class PortionHandlerTest {

  private static final String TEXT_FIELD = "text";
  private static final String FILENAME_FIELD = "filename";

  @Test
  public void testParagraphSplit() {

    String document = "CUI//PHYS\nThis is a paragraph.\n\nIt contains several lines.\nCUI//FSEC\nThis is the second paragraph.\nIt also contains two lines.";

    SimpleRecord record = new SimpleRecord();
    record.put(TEXT_FIELD, document);
    record.put(FILENAME_FIELD, "examplefile.doc");

    PortionHandler handler = new PortionHandler();

    Map<String, Object> configs = new HashMap<>();
    configs.put(PortionHandler.TEXT_FIELD_PARAMETER, TEXT_FIELD);

    handler.setup(configs);

    List<SimpleRecord> output = StreamSupport
        .stream(handler.transform(record).spliterator(), false)
        .collect(Collectors.toList());

    output.stream().forEach(r -> System.out.println(r));
    assertEquals(2, output.size());

    assertTrue(output.get(0).containsKey("filename"));
    assertTrue(output.get(0).containsKey("paragraph"));
  }
}
