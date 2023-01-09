package com.koverse.addon.web.files;

import com.koverse.sdk.data.Parameter;
import com.koverse.sdk.data.SimpleRecord;
import com.koverse.sdk.ingest.records.RecordsProvider;

import info.bliki.wiki.dump.IArticleFilter;
import info.bliki.wiki.dump.Siteinfo;
import info.bliki.wiki.dump.WikiArticle;
import info.bliki.wiki.dump.WikiXMLParser;
import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WikipediaRecords implements RecordsProvider {
  private static final String NAME = "Wikipedia Export";
  private static final String TYPE_ID = "parser_wikipedia";
  private static final String DESC = "Parses Wikipedia data.";
  private static final String VERSION = "0.1";

  private WikiXMLParser wxp;
  private PlainTextConverter converter;
  private WikiModel model;
  private BlockingQueue<SimpleRecord> queue;
  public static final SimpleRecord END_OF_RECORDS = new SimpleRecord();
  private boolean done;

  public WikipediaRecords() {}

  public WikipediaRecords(InputStream stream) throws IOException {
    try {

      queue = new ArrayBlockingQueue<SimpleRecord>(20);
      done = false;

      IArticleFilter handler = new WArticleFilter();
      wxp = new WikiXMLParser(stream, handler);

      // setup the wikipedia parser
      converter = new PlainTextConverter(true);
      model = new WikiModel("", "");

      Thread t = new Thread(new Runnable() {

        @Override
        public void run() {
          try {
            wxp.parse();
            System.out.println("done parsing");
            queue.put(END_OF_RECORDS);
          } catch (IOException ex) {
            Logger.getLogger(WikipediaRecords.class.getName()).log(Level.SEVERE, null, ex);
          } catch (SAXException ex) {
            Logger.getLogger(WikipediaRecords.class.getName()).log(Level.SEVERE, null, ex);
          } catch (InterruptedException ex) {
            Logger.getLogger(WikipediaRecords.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      });
      t.start();

    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

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
  public List<Parameter> getParameters() {
    return new ArrayList<Parameter>();
  }


  @Override
  public void close() throws IOException {
    // stream is closed by WikipediaSource
  }

  /**
   * Iterator that blocks on hasNext until there is an item on the queue. queue is considered empty when the END_OF_RECORD has been placed
   * on it
   */
  @Override
  public Iterator<SimpleRecord> iterator() {
    return new Iterator<SimpleRecord>() {

      private SimpleRecord next = null;

      @Override
      public boolean hasNext() {
        if (next == null) {
          try {
            next = queue.take();
          } catch (InterruptedException ex) {
            Logger.getLogger(WikipediaRecords.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
        // END_OF_RECORDS is an empty record
        return next.size() > 0;
      }

      @Override
      public SimpleRecord next() {
        SimpleRecord ret = next;
        next = null;
        return ret;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose Tools | Templates.
      }
    };
  }

  private class WArticleFilter implements IArticleFilter {

    @Override
    public void process(WikiArticle article, Siteinfo info) throws SAXException {


      // only get articles
      if (article.getNamespace().length() > 0) {
        return;
      }

      String wikitext = article.getText();
      String plaintext = model.render(converter, wikitext).replace("{{", " ").replace("}}", " ");

      if (plaintext.isEmpty()) {
        return;
      }

      SimpleRecord record = new SimpleRecord();

      record.put("title", article.getTitle());
      record.put("article", plaintext);
      record.put("timestamp", article.getTimeStamp());
      record.put("id", Long.parseLong(article.getId()));
      record.put("revision", Long.parseLong(article.getRevisionId()));

      try {
        queue.put(record);
      } catch (InterruptedException ex) {
        Logger.getLogger(WikipediaRecords.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
}
