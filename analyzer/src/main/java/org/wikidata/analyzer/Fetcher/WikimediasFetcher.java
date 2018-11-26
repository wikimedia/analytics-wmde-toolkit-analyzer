package org.wikidata.analyzer.Fetcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class WikimediasFetcher extends WikiDataFetcher {

    static final String CACHE_FILENAME = "mediawikis.json";
    public static final String ENTITYID_KEY = "entityID";
    public static final String DBNAME_KEY = "dbname";

    public WikimediasFetcher(File dataDirectory) {
        super(dataDirectory);
    }

    public List<HashMap<String, String>> getMediawikis() {
        String location = dataDirectory.getAbsolutePath() + CACHE_FILENAME;
        long now = System.currentTimeMillis();
        File cacheFile = new File(location);
        if (
                cacheFile.exists()
                && now - cacheFile.lastModified() < MAX_CACHE_AGE
        ) {
            return readWikimediasFromCacheFile(location);
        }

        return getWikimediasFromWikidata();
    }

    private List<HashMap<String, String>> readWikimediasFromCacheFile(String cacheLocation) {
        JSONParser parser = new JSONParser();
        try {
            JSONArray jsonObject = (JSONArray) parser.parse(new FileReader(cacheLocation));
            List<HashMap<String, String>> result = (ArrayList<HashMap<String, String>>) jsonObject;
            return result;
        } catch (IOException e) {
            System.out.println("Error accessing cache file:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (ParseException e) {
            System.out.println("Error parsing mediawikis cache file with message:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    private List<HashMap<String, String>> getWikimediasFromWikidata() {
        String querySelect = "SELECT ?Wikimedia_project ?Wikimedia_database_name WHERE {\n" +
                "  ?Wikimedia_project wdt:P31?/wdt:P279* wd:Q14827288.\n" +
                "  ?Wikimedia_project wdt:P1800 ?Wikimedia_database_name.\n" +
                "}";
        JSONObject wikimediasJSON = queryDataFromWikidata(querySelect);
        List<HashMap<String, String>> mediawikis = this.parseJsonResponse(wikimediasJSON);
        writeCache(mediawikis);
        return mediawikis;
    }

    private List<HashMap<String, String>> parseJsonResponse(JSONObject apiObject) {
        Integer urlPrefixLength = ENTITY_URL_PREFIX.length();

        JSONArray resultItems = getResultsList(apiObject);
        List<HashMap<String, String>> resultList = new ArrayList<>();

        for (JSONObject item : (Iterable<JSONObject>) resultItems) {
            if (!item.containsKey("Wikimedia_database_name")) {
                continue;
            }
            JSONObject langEditionItem = (JSONObject) item.get("Wikimedia_project");
            String entityId = ((String) langEditionItem.get("value")).substring(urlPrefixLength);

            JSONObject dbNameObject = (JSONObject) item.get("Wikimedia_database_name");
            String dbName = (String) dbNameObject.get("value");
            HashMap<String, String> itemMap = new HashMap<>();
            itemMap.put(ENTITYID_KEY, entityId);
            itemMap.put(DBNAME_KEY, dbName);

            resultList.add(itemMap);
        }

        return resultList;
    }

    private void writeCache(List<HashMap<String, String>> cacheData) {
        ObjectMapper mapper = new ObjectMapper();
        String filename = dataDirectory.getAbsolutePath() + CACHE_FILENAME;
        try {
            mapper.writeValue(new File(filename), cacheData);
        } catch (IOException e) {
            System.out.println("Error writing data retrieved from wikidata to filesystem cache: " + filename);
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

}
