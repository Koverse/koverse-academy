KDP3 provides the functionality to export data to arbitrary sinks.   
To start a project containing a custom sink, we’ll need to include the Koverse SDK.
In this example we’ll use the previously used Titanic dataset, exporting the data to CSV. In a real life example, this could be the transfer of data from one airgapped datastore to another.

First we’ll start off by subclassing ExportFileFormat:

```java
import com.koverse.sdk.data.Parameter;
import com.koverse.sdk.data.SimpleRecord;
import com.koverse.sdk.export.ExportFileFormat;

import au.com.bytecode.opencsv.CSVWriter;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvFormat extends ExportFileFormat {
```

Next, we define the variables that we'll use with user-defined parameters that will define our export schema.

```java
  public static final String CSV_FIELD_NAMES = "koverse_exportfileformat_csv_fieldnames";
  public static final String SEPARATOR_CHARACTER =
      "koverse_exportfileformat_csv_separator_character";
  public static final String QUOTE_CHARACTER = "koverse_exportfileformat_csv_quote_character";
  public static final String ESCAPE_CHARACTER = "koverse_exportfileformat_csv_escape_character";
  public static final String WRITE_HEADERS_TO_FILE = "koverse_exportfileformat_csv_writeheaders";


  private CSVWriter writer = null;
  private String[] fieldNames;
  private char separatorCharacter;
  private char quoteCharacter;
  private char escapeCharacter;
  ```

  We define user-defined parameters here:
  
  ```java
    @Override
  public List<Parameter> getParameters() {
    ArrayList<Parameter> params = new ArrayList<>();
    params.add(Parameter.newBuilder()
        .parameterName(CSV_FIELD_NAMES)
        .displayName("Fields to Export")
        .type(Parameter.TYPE_COLLECTION_MULTIPLE_FIELD)
        .hint("The fields to export.")
        .build());
    params.add(Parameter.newBuilder()
        .parameterName(SEPARATOR_CHARACTER)
        .displayName("Separator Character")
        .type(Parameter.TYPE_STRING)
        .defaultValue(",")
        .hint("The field separator character.")
        .build());
    params.add(Parameter.newBuilder()
        .parameterName(QUOTE_CHARACTER)
        .displayName("Quote Character")
        .type(Parameter.TYPE_STRING)
        .defaultValue("\"")
        .hint("The field quote character.")
        .build());
    params.add(Parameter.newBuilder()
        .parameterName(ESCAPE_CHARACTER)
        .displayName("Escape Character")
        .type(Parameter.TYPE_STRING)
        .defaultValue("\\")
        .hint("The escape character.")
        .build());
    params.add(Parameter.newBuilder()
        .parameterName(WRITE_HEADERS_TO_FILE)
        .displayName("Write Header Line")
        .type(Parameter.TYPE_BOOLEAN)
        .defaultValue("false")
        .hint("Indicates if headers are to be written.")
        .build());
    return params;
  }
  ```

We override the writeREcordToFormat class to read the records from the dataset and write them to a String array.

```java
  @Override
  protected void writeRecordToFormat(final SimpleRecord record) throws IOException {

    // assume record is flat yo
    // export transforms should be used to flatten any nested values
    String[] fields = new String[fieldNames.length];
    int i = 0;
    for (String fieldName : fieldNames) {
      if (record.containsKey(fieldName)) {
        // should make sure this writes a representation that will
        // end up being read as the right object type if ingested again
        fields[i] = record.get(fieldName).toString();
      } else {
        fields[i] = "";
      }
      i++;
    }

    writer.writeNext(fields);
  }
  ```

We define the file that we export to here.

```java
  @Override
  protected void startFile() throws IOException {

    Map<String, String> values = getContext().getParameterValues();
    separatorCharacter =
        StringEscapeUtils.unescapeJava(values.get(SEPARATOR_CHARACTER).toString()).charAt(0);
    quoteCharacter =
        StringEscapeUtils.unescapeJava(values.get(QUOTE_CHARACTER).toString()).charAt(0);
    escapeCharacter =
        StringEscapeUtils.unescapeJava(values.get(ESCAPE_CHARACTER).toString()).charAt(0);
    fieldNames = StringEscapeUtils.unescapeJava(values.get(CSV_FIELD_NAMES).toString()).split(",");

    writer =
        new CSVWriter(new OutputStreamWriter(getOutputStream()), separatorCharacter,
            quoteCharacter, escapeCharacter);
    if (values.get(WRITE_HEADERS_TO_FILE).toString().equals("true")) {
      writer.writeNext(fieldNames);
    }
  }
```

We define how to terminate the variables or running processes associated with the file writer here.

```java
  @Override
  protected void endFile() throws IOException {
    writer.flush();
  }
```

Here, we can define some metadata about the addon.

```java
  @Override
  public String getName() {
    return "CSV File Format (Comma separated values)";
  }

  @Override
  public String getTypeId() {
    return "koverse_exportfileformat_csv";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public String getDescription() {
    return "Export records to a comma-separated values file.";
  }

  @Override
  public String getExtension() {
    return "csv";
  }
}
```

