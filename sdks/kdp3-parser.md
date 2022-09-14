KDP3 provides the functionality to parse the data of arbitrary incoming data types. 
To start a project containing a custom parser, we'll need to include the Koverse SDK.
In this example, we'll use the previously used Titanic dataset, importing the CSV we previously exported.

First, we'll start off by subclassing AbstractFileBasedRecordsProvider

```java
import com.koverse.com.google.common.collect.ImmutableSet;
import com.koverse.sdk.data.Parameter;
import com.koverse.sdk.data.SimpleRecord;
import com.koverse.sdk.data.TermTypeDetector;
import com.koverse.sdk.ingest.records.AbstractFileBasedRecordsProvider;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExcelSimpleCsvRecords extends AbstractFileBasedRecordsProvider {
```

Next, we define the variables that we'll use with user-defined parameters that will define some of the schema of the CSV file we'll be ingesting, as well as starting the logger.

```java
  private static final Logger logger = LoggerFactory.getLogger(ExcelSimpleCsvRecords.class);

  private static final String NAME = "Microsoft Excel (Simple)";
  private static final String TYPE_ID = "parser_structured_ms_excel_simple";
  private static final String DESC = "Parses text from an Excel file as if it was a CSV formatted file";
  private static final String VERSION = "0.3";
  private static final Set<String> SUPPORTED_EXTENSIONS = ImmutableSet.of("xls", "xlsx");
  private static final int PRIORITY = 20;
  static final String PARAM_SHEET_NUMBER = "sheetNumber";
  private static final String DETERMINE_TYPES = "koverse_determine_types";

  private InputStream inputStream;
  private int sheetNumber;
  private boolean determineTypes;
```

We define our addon metadata.

```java
  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getTypeId() {
    return TYPE_ID;
  }

  @Override
  public String getDescription() {
    return DESC;
  }

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public int getPriority() {
    return PRIORITY;
  }

  @Override
  public Set<String> getSupportedExtensions() {
    return SUPPORTED_EXTENSIONS;
  }
```

We define user-defined parameters here.

```java
  @Override
  public List<Parameter> getParameters() {
    return Arrays.asList(
            Parameter.newBuilder()
            .parameterName(PARAM_SHEET_NUMBER)
            .displayName("Sheet number")
            .hint("The first sheet is 1, the second is 2, and so on")
            .required(true)
            .type(Parameter.TYPE_INTEGER)
            .defaultValue("1")
            .build(),
            Parameter.newBuilder()
            .displayName("Determine Types")
            .parameterName(DETERMINE_TYPES)
            .type(Parameter.TYPE_BOOLEAN)
            .required(true)
            .defaultValue("true")
            .hint("Tries to determine and automatically apply types such as number, date, boolean, etc")
            .build()
    );
  }
```

We define the input stream here by overriding setInputStream.

```java
  @Override
  public void setInputStream(final InputStream input) throws IOException {
    this.inputStream = input;
  }
```

We configure some parameters about our parser here, which reads some options from the user defined parameters.

```java
  @Override
  public void configure(final Map<String, String> options) {
    sheetNumber = Integer.parseInt(options.get(PARAM_SHEET_NUMBER));
    this.determineTypes = Boolean.parseBoolean(options.get(DETERMINE_TYPES));
  }
```

We define whether the incoming data is nested or not.

```java
  @Override
  public boolean flatSchema() {
    return true;
  }
```

We meet the interface requirements for close and throw an ioException.

```java
@Override
  public void close() throws IOException {
  }
```

Finally, we override the iterator method, which reads from the input stream, parsing the content into an iterator of simple records that KDP3 can read.

```java
  @Override
  public Iterator<SimpleRecord> iterator() {
    try {
      final Workbook workbook = WorkbookFactory.create(inputStream);
      final Sheet sheet = workbook.getSheetAt(sheetNumber - 1); //make the sheet number 0-based
      final int firstRowNumber = sheet.getFirstRowNum();
      final int lastRowNumber = sheet.getLastRowNum();
      final int numberOfRows;
      final List<SimpleRecord> records;

      if (lastRowNumber == 0) {
        numberOfRows = 0;
      } else if (firstRowNumber < lastRowNumber) {
        numberOfRows = lastRowNumber - firstRowNumber;
      } else {
        throw new RuntimeException(
                String.format(
                        "There is a problem with sheet %s ('%s'): first row number is %s and last row number is %s",
                        sheetNumber,
                        sheet.getSheetName(),
                        firstRowNumber,
                        lastRowNumber));
      }

      if (numberOfRows > 1) {
        final int firstCellNumber = sheet.getRow(firstRowNumber).getFirstCellNum();
        final List<String> header = processHeaderRow(sheet.getRow(firstRowNumber), firstCellNumber);

        logger.info("Header: is {} using first cell number {}", header, firstCellNumber);
        records = new ArrayList<>(numberOfRows);

        for (int rowNumber = firstRowNumber + 1; rowNumber <= lastRowNumber; rowNumber++) {
          final List<String> fields = processRow(sheet.getRow(rowNumber), firstCellNumber, firstCellNumber + header.size());

          logger.debug("Fields: {}", fields);

          if (hasData(fields)) {
            final SimpleRecord record = new SimpleRecord(new HashMap<String, Object>(header.size()));

            for (int fieldNumber = 0; fieldNumber < fields.size(); fieldNumber++) {
              final String fieldName = header.get(fieldNumber);
              final String fieldValue = fields.get(fieldNumber);
              
              if (determineTypes) {
                record.put(fieldName, TermTypeDetector.typify(fieldValue));
              } else {
                record.put(fieldName, fieldValue);
              }
            }

            records.add(record);
          }
        }
      } else {
        records = Collections.emptyList();
      }

      return records.iterator();
    } catch (IOException | InvalidFormatException e) {
      throw new RuntimeException(
              String.format(
                      "Could not read excel file: %s", getCurrentFilename()),
              e);
    }
  }

  private List<String> processHeaderRow(final Row row, final int firstCellNumber) {
    final int lastCellNumber = row.getLastCellNum();

    return processRow(row, firstCellNumber, lastCellNumber);

  }

  private List<String> processRow(final Row row, final int firstCellNumber, final int lastCellNumber) {
    final List<String> strings = new ArrayList<>(lastCellNumber - firstCellNumber);

    for (int cellNumber = firstCellNumber; cellNumber < lastCellNumber; cellNumber++) {
      strings.add(processCell(row, cellNumber));
    }

    return strings;
  }

  private String processCell(final Row row, final int cellNumber) {
    final Cell cell = row.getCell(cellNumber);

    return cell == null ? "" : cell.toString().trim();
  }

  private boolean hasData(final List<String> strings) {

    for (final String string : strings) {
      if (!string.isEmpty()) {
        return true;
      }
    }

    return false;
  }
}
```


