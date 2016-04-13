package org.wikidata.analyzer.Fetcher;

import org.wikidata.wdtk.dumpfiles.MwDumpFile;
import org.wikidata.wdtk.dumpfiles.MwLocalDumpFile;
import org.wikidata.wdtk.dumpfiles.wmf.JsonOnlineDumpFile;
import org.wikidata.wdtk.util.DirectoryManager;
import org.wikidata.wdtk.util.DirectoryManagerImpl;
import org.wikidata.wdtk.util.WebResourceFetcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Addshore
 */
public class DumpFetcher {

    private File dataDirectory;

    public DumpFetcher(File dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    /**
     * Look for the most recent dump date online and try to retrieve as dump object with fallback:
     * 1 - Look for local dump copies (in a collection of locations)
     * 2 - Look online & download dumps
     *
     * @return MwDumpFile
     * @throws IOException
     */
    public MwDumpFile getDump( String dumpDate ) throws IOException {
        System.out.println("Getting dump with date " + dumpDate);

        // Look for the dump in a list of possible local locations
        List<String> directoryList = new ArrayList<>();
        //Local data dir location
        directoryList.add(this.dataDirectory + "/dumpfiles/json-" + dumpDate + "/");
        //Labs dump location
        directoryList.add("/public/dumps/public/wikidatawiki/entities/" + dumpDate + "/");
        //Stat1002 dump location
        directoryList.add("/mnt/data/xmldatadumps/public/wikidatawiki/entities/" + dumpDate + "/");

        for (String dumpDirectory: directoryList) {
            System.out.println("Looking for dump files in: " + dumpDirectory);

            // Try and few different file names
            List<String> fileList = new ArrayList<>();
            fileList.add(dumpDirectory + dumpDate + ".json.gz");
            fileList.add(dumpDirectory + dumpDate + "-all.json.gz");
            fileList.add(dumpDirectory + "wikidata-" + dumpDate + ".json.gz");
            fileList.add(dumpDirectory + "wikidata-" + dumpDate + "-all.json.gz");

            for (String dumpLocation: fileList) {
                if (Files.exists(Paths.get(dumpLocation)) && Files.isReadable(Paths.get(dumpLocation))) {
                    MwLocalDumpFile localDumpFile = new MwLocalDumpFile( dumpLocation );
                    if( localDumpFile.isAvailable() ) {
                        System.out.println("Using dump file from: " + dumpLocation);
                        localDumpFile.prepareDumpFile();
                        return localDumpFile;
                    }
                }
            }

        }

        // Get ready to try online dumps
        DirectoryManager localDirectoryManager = new DirectoryManagerImpl(
                Paths.get(this.dataDirectory.getAbsolutePath() + File.separator + "dumpfiles"),
                false
        );
        WebResourceFetcher fetcher = new RedirectFollowingWebResourceFetcherImpl();

        // List the online dumps
        Map<String, MwDumpFile> onlineDumpMap = new HashMap<String, MwDumpFile>();
        // dumps.wikimedia.org
        onlineDumpMap.put(
                "dumps.wikimedia.org",
                new JsonOnlineDumpFile(dumpDate, "wikidatawiki", fetcher, localDirectoryManager)
        );
        onlineDumpMap.put(
                "archive.org",
                new ArchiveOrgJsonOnlineDumpFile(dumpDate, "wikidatawiki", fetcher, localDirectoryManager)
        );

        // Try the online dumps
        for ( Map.Entry<String, MwDumpFile> entry : onlineDumpMap.entrySet() ) {
            String dumpLocation = entry.getKey();
            MwDumpFile onlineDump = entry.getValue();
            try{
                System.out.println("Looking for / downloading online dump from: " + dumpLocation);
                onlineDump.prepareDumpFile();
                System.out.println("Using dump from: " + dumpLocation);
                return onlineDump;
            } catch ( IOException exception ) {
                // Ignore the exception so we can try the next online dump
            }
        }

        // Everything failed! :(
        throw new IOException("Failed to get dump from any sources");
    }
}
