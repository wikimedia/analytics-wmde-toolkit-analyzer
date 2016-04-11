package main.java.org.wikidata.analyzer.Fetcher;

import org.wikidata.wdtk.util.WebResourceFetcherImpl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Addshore
 */
public class RedirectFollowingWebResourceFetcherImpl extends WebResourceFetcherImpl {

    @Override
    public InputStream getInputStreamForUrl(String urlString)
            throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn;
        if (hasProxy()) {
            conn = (HttpURLConnection)url.openConnection(proxy);
        } else {
            conn = (HttpURLConnection)url.openConnection();
        }
        conn.setRequestProperty("User-Agent", userAgent);

        boolean redirect = false;

        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER)
                redirect = true;
        }

        if (redirect) {
            // get redirect url from "location" header field
            String newUrl = conn.getHeaderField("Location");
            // open the new connection again
            conn = (HttpURLConnection) new URL(newUrl).openConnection();
        }

        return conn.getInputStream();
    }

}
