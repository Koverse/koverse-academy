KDP3 provides the functionality to  to parse external security labels into internal Accumulo security label expressions. 
To start a project containing a custom security label parser, we’ll need to include the Koverse SDK.
In this example we’ll use the previously used Titanic dataset, assigning security labels to passengers based on their class. In a real life dataset for example, some deployed assets may be undercover, but instead of having to be placed in different dataset or database, using KDP3, they can be placed in the same dataset as other assets and conveniently viewed securely.

First we’ll start off by subclassing SecurityLabelParser:

```java
import com.koverse.sdk.security.SecurityLabelParser;
import com.koverse.sdk.security.SecurityLabelParserException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TitanicLabelParser implements SecurityLabelParser {
```

We'll begin by defining a parameter to map labels from the incoming data to internal ones used by Accumulo. 

```java
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
  ```

Next, we'll parse the incoming column from the dataset, pass the resulting string to the map we defined earlier, and apply the map to the external labels, parsing the output to an Accumulo security label expression.

```java
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
```
