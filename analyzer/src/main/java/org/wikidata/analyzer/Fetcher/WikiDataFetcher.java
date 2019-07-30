package org.wikidata.analyzer.Fetcher;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Scanner;

public abstract class WikiDataFetcher {
    static final int MAX_CACHE_AGE = 14 * 24 * 60 * 60 * 1000;
    static final String ENTITY_URL_PREFIX = "http://www.wikidata.org/entity/";

    File dataDirectory;

    public WikiDataFetcher(File dataDirectory) {
        this.dataDirectory = dataDirectory;
    }


    JSONObject queryDataFromWikidata(String querySelect) {

        String endpoint = "http://wdqs1005.eqiad.wmnet/sparql";
        String url = null;
        try {
            url = endpoint + "?query=" + URLEncoder.encode(querySelect, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
            // this will never happen as UTF-8 is supported and hard-coded
        }

        URLConnection connection = null;
        try {
            connection = new URL(url).openConnection();
            connection.setRequestProperty("Accept", "application/sparql-results+json");
        } catch (IOException e) {
            System.err.println("Error opening connection to " + endpoint);
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        JSONParser jsonParser = new JSONParser();
        try (Scanner scanner = new Scanner(connection.getInputStream(), "UTF-8")) {
            scanner.useDelimiter("\\A");
            String jsonResponse = scanner.next();

            return (JSONObject) jsonParser.parse(jsonResponse);
        } catch (IOException e) {
            System.err.println("Error getting data from " + endpoint);
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (ParseException e) {
            System.err.println("Error parsing data from " + endpoint);
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    JSONArray getResultsList(JSONObject apiJSON) {
        JSONObject resultsObject = (JSONObject) apiJSON.get("results");
        return (JSONArray) resultsObject.get("bindings");
    }
}
