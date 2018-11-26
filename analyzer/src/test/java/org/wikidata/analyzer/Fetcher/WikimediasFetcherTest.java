package org.wikidata.analyzer.Fetcher;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikimediasFetcherTest extends TestCase {

    public void testFetchingWikis() {
        File tempdir = null;
        try {
            tempdir = Files.createTempDirectory("WikidataAnalyzer-data").toFile();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            fail("Couldn't create temp dir");
        }
        WikimediasFetcher wikimediasFetcher = new WikimediasFetcher(tempdir);
        List<HashMap<String, String>> actualResult;

        actualResult = wikimediasFetcher.getMediawikis();

        assertTrue("No items were retrieved", actualResult.size() > 0);
        assertTrue("Item doesn't contain expected key", actualResult.get(0).containsKey(WikimediasFetcher.ENTITYID_KEY));

        String actualEntityId = actualResult.get(0).get(WikimediasFetcher.ENTITYID_KEY);
        Pattern entityIdPattern = Pattern.compile("Q\\d+");
        Matcher entityMatch = entityIdPattern.matcher(actualEntityId);
        assertTrue("EntityId didn't match expected format but was " + actualEntityId, entityMatch.matches());

        String expectedCachefile = tempdir.getAbsolutePath() + WikimediasFetcher.CACHE_FILENAME;
        assertTrue("Missing cache file " + expectedCachefile, new File(expectedCachefile).exists());
    }
}
