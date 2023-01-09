package com.koverse.addon.web.source;


import com.koverse.addon.web.files.WikipediaRecords;
import com.koverse.sdk.data.Parameter;
import com.koverse.sdk.data.SimpleRecord;
import com.koverse.sdk.source.AbstractListMapReduceSource;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class WikipediaDumpSource extends AbstractListMapReduceSource {

  public static final String DUMP_URL =
      "https://dumps.wikimedia.org/enwiki/latest/enwiki-latest-pages-articles.xml.bz2";
  private InputStream stream;

  @Override
  public Iterable<String> enumerateList() {
    return Collections.singleton(DUMP_URL);
  }

  @Override
  public Iterable<SimpleRecord> recordsForItem(String item) throws IOException {

    URL url = new URL(item);

    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
    conn.setRequestProperty("User-Agent", "Koverse Inc.");

    InputStream s = conn.getInputStream();

    stream = new BZip2CompressorInputStream(s);
    return new WikipediaRecords(stream);
  }


  @Override
  public List<Parameter> getParameters() {
    return Collections.emptyList();
  }

  @Override
  public void configure() throws IOException {

  }

  @Override
  public String getName() {
    return "All of Wikipedia";
  }

  @Override
  public String getVersion() {
    return "0.2";
  }

  @Override
  public String getSourceTypeId() {
    return "wikipedia-dump-source";
  }

  @Override
  public String getDescription() {
    return "Download all one-million-plus articles in the English version of Wikipedia. " 
            + "The entire ingest process may require a very long time.";
  }

  @Override
  public Boolean isContinuous() {
    return false;
  }

  @Override
  public void close() throws IOException {
    stream.close();
  }

  @Override
  public List<Parameter> getFlowParameters() {
    return Collections.emptyList();
  }

  @Override
  public boolean flatSchema() {
    return true;
  }

}
