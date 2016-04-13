package org.wikidata.analyzer.Fetcher;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.wikidata.wdtk.util.WebResourceFetcher;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * @author Addshore
 */
public class DumpDateFetcher {

    /**
     * This presumes that the newest dump is at the bottom of the list (ordered by name)
     *
     * @return String in format yyyymmdd eg. 20150525
     * @throws IOException
     */
    public String getLatestOnlineDumpDate() throws IOException {
        String html = this.getJsonDumpsPageHtml();
        Document doc = Jsoup.parse(html);
        Element finalLink = doc.select("a").last();
        String fileName = finalLink.html();
        return fileName.substring(0, fileName.length() - 8);// remove 8 chars
    }

    /**
     * @return String html of the json dumps page for wikidata
     * @throws IOException
     */
    private String getJsonDumpsPageHtml() throws IOException {
        WebResourceFetcher fetcher = new RedirectFollowingWebResourceFetcherImpl();
        Reader r = new InputStreamReader(fetcher.getInputStreamForUrl("http://dumps.wikimedia.org/other/wikidata/"));
        StringBuilder buf = new StringBuilder();
        while (true) {
            int ch = r.read();
            if (ch < 0)
                break;
            buf.append((char) ch);
        }
        return buf.toString();
    }

}
