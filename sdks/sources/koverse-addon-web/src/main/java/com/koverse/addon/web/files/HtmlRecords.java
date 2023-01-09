package com.koverse.addon.web.files;

import static com.koverse.com.google.common.collect.Sets.newHashSet;

import com.koverse.sdk.data.Parameter;
import com.koverse.sdk.data.SimpleRecord;
import com.koverse.sdk.ingest.records.AbstractFileBasedRecordsProvider;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HtmlRecords extends AbstractFileBasedRecordsProvider {

  private final Logger logger = LoggerFactory.getLogger(HtmlRecords.class);
  private InputStream stream = null;

  @Override
  public String getName() {
    return "HTML";
  }

  @Override
  public String getTypeId() {
    return "parser_html";
  }

  @Override
  public String getDescription() {
    return "Parse HTML files into records, one per file.";
  }

  @Override
  public String getVersion() {
    return "0.1";
  }

  @Override
  public List<Parameter> getParameters() {
    return Collections.EMPTY_LIST;
  }

  @Override
  public void close() throws IOException {
    if (this.stream != null) {
      this.stream.close();
    }
    stream = null;
  }

  @Override
  public Iterator<SimpleRecord> iterator() {
    SimpleRecord record = recordFromStream(stream);
    if (record != null) {
      return Collections.singleton(record).iterator();
    }
    return Collections.EMPTY_LIST.iterator();
  }

  @Override
  public int getPriority() {
    return 1;
  }

  @Override
  public Set<String> getSupportedExtensions() {
    return newHashSet("html", "htm", "xhtml");
  }

  @Override
  public void setInputStream(InputStream input) throws IOException {
    this.stream = input;
  }

  @Override
  public void configure(Map<String, String> options) {

  }

  @Override
  public boolean flatSchema() {
    return true;
  }

  private SimpleRecord recordFromStream(InputStream input) {

    Document doc;
    try {
      doc = Jsoup.parse(IOUtils.toString(input));
    } catch (IOException e) {
      logger.warn("could not parse inputstream, returning null", e);
      logParsingProblem(e.getLocalizedMessage());
      return null;
    }

    if (doc == null) {
      return null;
    }
    
    SimpleRecord r = new SimpleRecord();

    r.put("rawHtml", doc.html());
    
    Elements title = doc.getElementsByTag("title");
    Iterator<Element> iter = title.iterator();
    if (iter.hasNext()) {
      r.put("title", iter.next().data());
    }

    String text = doc.body().text();
    if (text.length() > 0) {
      r.put("text", text);
    }

    // get all link text
    StringBuilder linkText = new StringBuilder();
    for (Element linkElement : doc.getElementsByTag("a")) {
      linkText.append(linkElement.attr("href"));
      linkText.append(" ");
    }

    if (linkText.length() > 0) {
      r.put("links", linkText.toString());
    }

    r.put("filename", getCurrentFilename());
    
    return r;
  }
}
