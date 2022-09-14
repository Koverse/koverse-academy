KDP3 provides the functionality to extend data ingestion to custom sources like databases, data publishers, different filesystems, and in this case, web URL resources.
To start a project containing a custom source, we’ll need to include the Koverse SDK.
In this example we’ll ingest data from a URL source.
First we’ll start off by subclassing AbstractFileBasedSource:

```java
import static com.koverse.com.google.common.base.Preconditions.checkNotNull;
import static com.koverse.com.google.common.base.Predicates.notNull;
import static com.koverse.com.google.common.collect.Lists.newArrayList;

import com.koverse.com.google.common.base.Function;
import com.koverse.com.google.common.base.Splitter;
import com.koverse.com.google.common.collect.FluentIterable;
import com.koverse.sdk.data.Parameter;
import com.koverse.sdk.source.AbstractFileBasedSource;
import com.koverse.sdk.source.ImportSourcePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class UrlSource extends AbstractFileBasedSource {
```

If you’re using an IDE such as Eclipse or Netbeans you can use the IDE’s function to generate stubs for the functions required to be implemented by the UrlSource class.

We’ll begin by defining a parameter that our user will use to input their selection of URLs, set up the logger, and define an additional parameter which will be used later on to pass the URLs to a mapreduce process:

```java
  protected static final String URLS_PARAMETER = "koverse_input_urls";

  private static final Logger logger = LoggerFactory.getLogger(UrlSource.class);
  private FluentIterable<String> urls;
```

Next, we will have to define ClasspathUrlStreamHandler, a subclass of URLStreamHandler that handles URLs of the type classpath:path.

```java
  private static class ClasspathUrlStreamHandler extends URLStreamHandler {
    private final ClassLoader classLoader;

    public ClasspathUrlStreamHandler() {
      classLoader = checkNotNull(getClass().getClassLoader(), "Could not obtain class loader");
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
      final String urlPath =
          checkNotNull(checkNotNull(url, "URL was null").getPath(), "Path was null for URL %s", url);
      final URL resource =
          checkNotNull(classLoader.getResource(urlPath),
              "Could not get resource at path  %s for URL %s", urlPath, url);
      return checkNotNull(resource.openConnection(),
          "could not open connection to resource %s using for URL %s", resource, url);
    }
  }
```

Next, we can implement a method to read the user-defined inputs and set them up for more processing downstream, setting them to the urls variable that we defined at the top of the class. 

```java
  @Override
  public void configureFileBasedSource() throws IOException {

    urls =
        FluentIterable.from(Splitter.on(',').trimResults().omitEmptyStrings()
            .split(getContext().getParameterValues().get(URLS_PARAMETER)));
  }
```

Since we are using the AbstractFileBasedSource, we can define the getFileBasedFlowParameters to set the user-defined parameters, in this case a list of URLs. We set the getParameters method to return an empty list since we're not using it.

```java
  @Override
  public List<Parameter> getParameters() {
    return Collections.emptyList();
  }

  @Override
  public List<Parameter> getFileBasedFlowParameters() {
    return newArrayList(
            Parameter.newBuilder()
                    .parameterName(URLS_PARAMETER)
                    .displayName("Comma-separated list of URLs")
                    .type(Parameter.TYPE_TEXTAREA)
                    .parameterGroup("Target")
                    .required(Boolean.TRUE)
                    .placeholder("http://www.example.com/path/file.csv, http://www.example.com/path//to/other/file.csv")
                    .position(1)
                    .build());
  }
  ```

  We can define some meta data about the class itself here, including some user viewable information.

```java
    @Override
  public String getName() {

    return "URL Source";
  }

  @Override
  public String getVersion() {
    return "1.1.1";
  }

  @Override
  public String getSourceTypeId() {
    return "url";
  }

  @Override
  public String getDescription() {
    return "Retrieve the contents of one or more URLs. Koverse will connect to the URL and download a single file, "
        + "if one is specified, or else can optionally try to download all of the files listed in the response from the URL.";
  }
```

Since we must pass in an inputStream object to ingest the data from our iterable of URL strings, we define our streamForItem method here.

```java
  @Override
  public InputStream streamForItem(String item) throws IOException {
    final URL url = createUrl(item);
    final URLConnection conn = url.openConnection();

    return conn.getInputStream();
  }
```

Here, we handle our URL strings, and generate URL objects, handling any classpath:path URLs as needed.

```java
  private URL createUrl(String spec) throws MalformedURLException {
    if (checkNotNull(spec, "Spec was null").startsWith("classpath:")) {
      return new URL(null, spec, new ClasspathUrlStreamHandler());
    } else {
      return new URL(spec);
    }
  }
```

Here, define transformStringToImportSourcePath, which calls createUrl, and turns our URL strings into URL objects, and turns those URL objects into ImportSourcePath objects, the object needed for the final ingestion logic in enumerateUnfilteredList.

```java
  @Override
  public final Boolean isContinuous() {
    return false;
  }

  private Function<String, ImportSourcePath> transformStringToImportSourcePath() {
    return new Function<String, ImportSourcePath>() {
      @Override
      public ImportSourcePath apply(String urlString) {
        try {
          final URL urlCheck = createUrl(urlString);
          final URLConnection connCheck = urlCheck.openConnection();
          final long lastModifiedTime = connCheck.getLastModified();
          final Date lastModifiedDate;

          if (lastModifiedTime == 0) {
            lastModifiedDate = new Date();
          } else {
            lastModifiedDate = new Date(lastModifiedTime);
          }

          return new ImportSourcePath.Builder().path(urlString).lastModifiedDate(lastModifiedDate)
              .fileSize(connCheck.getContentLengthLong()).build();
        } catch (MalformedURLException ex) {
          logger.warn("Malformed URL: {}. Skipping", urlString, ex);
          return null;
        } catch (IOException ex) {
          logger.warn("Could not open URL: {}. Skipping", urlString, ex);
          return null;
        }
      }
    };
  }
```
Here, we can finally call all of these helper classes and methods to iterate through all of our URLs that we want to ingest data from.

```java
  @Override
  public Iterable<ImportSourcePath> enumerateUnfilteredList() throws Exception {

    return urls.transform(transformStringToImportSourcePath()).filter(notNull()).toList();
  }

  @Override
  public boolean supportsInputStreamReset() {
    return false;
  }
  ```
