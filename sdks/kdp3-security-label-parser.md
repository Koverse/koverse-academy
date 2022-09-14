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
    return "0.1.0";
  ```

Next, we'll parse the incoming column from the dataset, pass the resulting string to the map we defined earlier, and apply the map to the external labels, parsing the output to an Accumulo security label expression.

```java
  @Override
  public String toExpression(String s) throws SecurityLabelParserException {

    return PASSENGER_MAP.get(s);
  }
}
```

Build the package with a `mvn package` upload the target jar file to the addons page.