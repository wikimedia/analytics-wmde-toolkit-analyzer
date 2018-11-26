package org.wikidata.analyzer.Fetcher;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class RefPropFetcher extends WikiDataFetcher {
    private static final String CACHE_FILENAME = "refProps.json";

    public RefPropFetcher(File dataDirectory) {
        super(dataDirectory);
    }

    public List<String> getReferenceProperties() {
        String location = dataDirectory.getAbsolutePath() + CACHE_FILENAME;
        long now = System.currentTimeMillis();
        File cacheFile = new File(location);
        if (
                cacheFile.exists()
                        && now - cacheFile.lastModified() < MAX_CACHE_AGE
        ) {
            return readPropertiesFromCacheFile(cacheFile);
        }

        return getPropertiesFromWikidata();
    }

    private List<String> readPropertiesFromCacheFile(File cacheFile) {
        List<String> line = new ArrayList<>();
        try (Scanner scanner = new Scanner(cacheFile)) {
            while (scanner.hasNext()) {
                line.addAll(Arrays.asList(scanner.nextLine().split(",")));
            }
        } catch (IOException ioe) {
            System.out.println("Error accessing cache file: " + cacheFile.getAbsolutePath());
            System.out.println(ioe.getMessage());
            ioe.printStackTrace();
            System.exit(1);
        }
        return line;
    }

    private List<String> getPropertiesFromWikidata() {
        String querySelect = "SELECT DISTINCT ?prop WHERE {\n" +
                "  { ?prop p:P2302 [\n" +
                "    ps:P2302 wd:Q53869507;\n" +
                "    pq:P5314 wd:Q54828450\n" +
                "  ]. }\n" +
                "  UNION { ?prop wdt:P31/wdt:P279* wd:Q18608359. }\n" +
                "}";
        JSONObject propertyJSON = queryDataFromWikidata(querySelect);
        List<String> refProperties = parseJSONResponse(propertyJSON);
        writeCache(refProperties);
        return refProperties;
    }

    private List<String> parseJSONResponse(JSONObject propertyJSON) {
        Integer urlPrefixLength = ENTITY_URL_PREFIX.length();

        JSONArray resultItems = getResultsList(propertyJSON);
        List<String> resultList = new ArrayList<>();

        for (JSONObject item : (Iterable<JSONObject>) resultItems) {
            if (!item.containsKey("prop")) {
                continue;
            }
            JSONObject property = (JSONObject) item.get("prop");
            String entityId = ((String) property.get("value")).substring(urlPrefixLength);

            resultList.add(entityId);
        }

        return resultList;
    }

    private void writeCache(List<String> cacheData) {
        String filename = dataDirectory.getAbsolutePath() + CACHE_FILENAME;

        String collect = String.join(",", cacheData);
        try (FileWriter writer = new FileWriter(filename)){
            writer.write(collect);
        } catch (IOException e) {
            System.out.println("Error writing data retrieved from wikidata to filesystem cache: " + filename);
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

}
