package com.koverse.addon.web.source;

import static com.koverse.com.google.common.collect.Lists.newArrayList;

import com.koverse.addon.web.files.WikipediaRecords;
import com.koverse.com.google.common.base.Joiner;
import com.koverse.sdk.data.Parameter;
import com.koverse.sdk.data.SimpleRecord;
import com.koverse.sdk.source.AbstractListMapReduceSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;


public class WikipediaPagesSource extends AbstractListMapReduceSource {

  public static final String PAGE_TITLE_LIST = "pageTitleListParam";
  private InputStream stream;
  private String[] pages;

  @Override
  public Iterable<String> enumerateList() {
    ArrayList<String> pageBatches = new ArrayList<>();
    ArrayList<String> batch = new ArrayList<>();

    for (String page : pages) {
      batch.add(page);
      if (batch.size() >= 15) {
        String pagesString = Joiner.on("%0A").join(batch);
        pageBatches.add(pagesString);
        batch.clear();
      }
    }
    if (!batch.isEmpty()) {
      String pagesString = Joiner.on("%0A").join(batch);
      pageBatches.add(pagesString);
    }

    return pageBatches;
  }

  @Override
  public Iterable<SimpleRecord> recordsForItem(String item) throws IOException {

    URL url =
        new URL("https://en.wikipedia.org/w/index.php?title=Special:Export&pages=" + item
            + "&curonly&action=submit");
    System.out.println(url);


    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

    conn.setRequestMethod("POST");
    conn.setRequestProperty("User-Agent", "Koverse Inc.");
    conn.setDoOutput(true);
    conn.getOutputStream().write("".getBytes("UTF-8"));
    conn.getOutputStream().flush();


    stream = conn.getInputStream();

    // stream = new BZip2CompressorInputStream(s);
    return new WikipediaRecords(stream);
  }


  @Override
  public List<Parameter> getParameters() {
    return Collections.emptyList();
  }

  @Override
  public List<Parameter> getFlowParameters() {
    return newArrayList(
            Parameter.newBuilder()
                    .parameterName(PAGE_TITLE_LIST)
                    .displayName("Article Title List")
                    .type(Parameter.TYPE_STRING)
                    .parameterGroup("Target")
                    .placeholder("Article_One Article_Two")
                    .position(1)
                    .required(Boolean.TRUE)
                    .build());
  }

  @Override
  public void configure() throws IOException {

    pages = ((String) getContext().getParameterValues().get(PAGE_TITLE_LIST)).split("\\s+");
  }

  @Override
  public String getName() {
    return "Wikipedia Pages";
  }

  @Override
  public String getVersion() {
    return "0.1.1";
  }

  @Override
  public String getSourceTypeId() {
    return "wikipedia-pages-source";
  }

  @Override
  public String getDescription() {
    return "Download a list of articles, specified by titles, from Wikipedia.";
  }

  @Override
  public Boolean isContinuous() {
    return false;
  }

  @Override
  public void close() throws IOException {
    if (stream != null) {
      stream.close();
    }
    stream = null;
  }

  @Override
  public boolean flatSchema() {
    return true;
  }
}
