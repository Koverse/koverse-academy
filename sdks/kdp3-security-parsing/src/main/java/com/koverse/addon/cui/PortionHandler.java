package com.koverse.addon.cui;

import static java.lang.Math.abs;

import com.koverse.sdk.Version;
import com.koverse.sdk.data.Parameter;
import com.koverse.sdk.data.SimpleRecord;
import com.koverse.sdk.ingest.transform.Normalization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PortionHandler extends Normalization {

  protected static final String TEXT_FIELD_PARAMETER = "textFieldParam";

  private String textField;

  @Override
  public String getName() {
    return "Document Portion Handler";
  }

  @Override
  public String getDescription() {
    return "Handle portion-marked documents";
  }

  @Override
  public String getTypeId() {
    return "portion-handler-normalization";
  }

  @Override
  public List<Parameter> getParameters() {

    return Arrays.asList(Parameter.newBuilder()
        .type(Parameter.TYPE_STRING)
        .displayName("Text field")
        .parameterName(TEXT_FIELD_PARAMETER)
        .build());
  }

  @Override
  public Version getVersion() {
    return new Version(0, 1,0);
  }

  @Override
  public void setup(Map<String, Object> configs) {
    textField = configs.get(TEXT_FIELD_PARAMETER).toString();
  }

  @Override
  public Iterable<SimpleRecord> transform(SimpleRecord simpleRecord) {

    ArrayList<SimpleRecord> portions = new ArrayList<>();

    int portionIndex = 0;

    // find the text field that contains portions
    if (simpleRecord.containsKey(textField)) {
      String documentText = simpleRecord.get(textField).toString();

      // alternately could use something like this if filenames conflict: Long.toString(abs(documentText.hashCode()));
      final String documentID = simpleRecord.get("filename").toString();

      // parse into paragraphs
      List<String> paragraphs = Arrays.asList(documentText.split("CUI//"));

      for (String p : paragraphs) {
        if (p.length() > 0) {
          SimpleRecord paragraph = new SimpleRecord();
          paragraph.put("filename", documentID);
          paragraph.put("paragraph", "CUI//" + p);
          paragraph.put("portionIndex", portionIndex);
          portionIndex++;

          portions.add(paragraph);
        }
      }
    }

    return portions;
  }
}
