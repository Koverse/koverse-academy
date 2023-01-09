package com.koverse.addon.web.source;

import static com.koverse.com.google.common.base.Preconditions.checkNotNull;
import static com.koverse.com.google.common.collect.Iterables.transform;
import static com.koverse.com.google.common.collect.Lists.newArrayList;

import com.koverse.addon.web.files.HtmlRecords;
import com.koverse.com.google.common.base.Function;
import com.koverse.sdk.data.Parameter;
import com.koverse.sdk.data.SimpleRecord;
import com.koverse.sdk.source.AbstractListMapReduceSource;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class NewsfeedSource extends AbstractListMapReduceSource {

  private static final String DELIMITER = "\t";
  private static final String HTML_MIME_TYPE = "text/html";
  private static final String URL_PARAMETER = "urlParameter";
  private static final Logger logger = LoggerFactory.getLogger(NewsfeedSource.class);
  private UrlSource records = new UrlSource();


  @Override
  public void configure() throws IOException {
    if (null == records.getRecordsProvider()) {
      records.setRecordsProvider(new HtmlRecords());
    }
  }

  @Override
  public final List<Parameter> getParameters() {
    return newArrayList(
            Parameter.newBuilder()
                    .parameterName(URL_PARAMETER)
                    .displayName("RSS Feed URL")
                    .type(Parameter.TYPE_URL)
                    .parameterGroup("Target")
                    .required(Boolean.TRUE)
                    .placeholder("http://rssfeedurl.xml")
                    .position(1)
                    .build());
    // new Parameter(URL_PARAMETER, "RSS Feed URL", Parameter.TYPE_STRING, "http://rssfeedurl.xml")
  }

  @Override
  public List<Parameter> getFlowParameters() {
    return Collections.emptyList();
  }


  @Override
  public String getName() {
    return "Newsfeed Source";
  }

  @Override
  public String getVersion() {
    return "1.1";
  }

  @Override
  public String getSourceTypeId() {
    return "newsfeedSource";
  }

  @Override
  public String getDescription() {
    return "Import all articles from an RSS newsfeed.";
  }


  @Override
  public Iterable<String> enumerateList() {

    Iterable<String> articleLinks = Collections.<String>emptySet();

    String urlString = getContext().getParameterValues().get(URL_PARAMETER).toString().trim();
    try {

      URL url = new URL(urlString);
      articleLinks = transform(retrieveFeedEntries(url), new Function<SyndEntry, String>() {
        public String apply(SyndEntry entry) {
          String path = entry.getLink();
          String fileSize = String.valueOf(0L);
          String lastModifedDate = (new Date()).toString();

          return (new StringBuilder())
                  .append(path)
                  .append(DELIMITER)
                  .append(fileSize)
                  .append(DELIMITER)
                  .append(lastModifedDate)
                  .append(DELIMITER)
                  .append(HTML_MIME_TYPE)
                  .toString();
        }
      });

    } catch (Exception e) {
      logger.warn("Exception getting paths for " + urlString + " " + e.getLocalizedMessage());
    }

    return articleLinks;
  }

  @Override
  public Iterable<SimpleRecord> recordsForItem(String item) throws IOException {
    return records.recordsForItem(item);
  }

  /**
   * Retrieves all RSS feed entries from the given feed URL.
   *
   * @param url The RSS feed URL.
   * @return An iterable of feed entries.
   */
  @SuppressWarnings("unchecked")
  private static Iterable<SyndEntry> retrieveFeedEntries(final URL url) throws IOException,
      IllegalArgumentException, FeedException {
    final SyndFeedInput feedInput = new SyndFeedInput();
    final XmlReader xmlReader = new XmlReader(checkNotNull(url, "Feed URL must not be null"));
    final SyndFeed feed = feedInput.build(xmlReader);

    return feed.getEntries();
  }

  @Override
  public void close() throws IOException {
    if (records != null) {
      records.close();
    }
    records = null;
  }

  @Override
  public final Boolean isContinuous() {
    return false;
  }

  @Override
  public boolean flatSchema() {
    return true;
  }

}
