package org.wikidata.analyzer.Fetcher;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RefPropFetcherTest extends TestCase {

    public void testFetchingRefProps() {
        File tempdir = null;
        try {
            tempdir = Files.createTempDirectory("WikidataAnalyzer-data").toFile();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            fail("Couldn't create temp dir");
        }
        RefPropFetcher refPropFetcher = new RefPropFetcher(tempdir);
        List<String> actualList = refPropFetcher.getReferenceProperties();
        actualList.sort(null);

        assertTrue("we received some properties", actualList.size() > 0);

        String actualEntityId = actualList.get(0);
        Pattern entityIdPattern = Pattern.compile("P\\d+");
        Matcher entityMatch = entityIdPattern.matcher(actualEntityId);
        assertTrue("EntityId didn't match expected format but was " + actualEntityId, entityMatch.matches());
    }

}
