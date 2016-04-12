package main.java.org.wikidata.analyzer.Fetcher;

import org.wikidata.wdtk.dumpfiles.MwDumpFile;
import org.wikidata.wdtk.dumpfiles.MwLocalDumpFile;
import org.wikidata.wdtk.dumpfiles.wmf.JsonOnlineDumpFile;
import org.wikidata.wdtk.util.DirectoryManager;
import org.wikidata.wdtk.util.DirectoryManagerImpl;
import org.wikidata.wdtk.util.WebResourceFetcher;
import org.wikidata.wdtk.util.WebResourceFetcherImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Addshore
 */
public class DumpFetcher {

    protected File dataDirectory;

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
        WebResourceFetcher fetcher = new WebResourceFetcherImpl();

        // Try a dump from dumps.wikimedia.org
        JsonOnlineDumpFile onlineDumpFile = new JsonOnlineDumpFile(
                dumpDate,
                "wikidatawiki",
                fetcher,
                localDirectoryManager
        );
        try{
            onlineDumpFile.prepareDumpFile();
        } catch ( IOException exception ) {
            throw new IOException("Failed to get dump from any sources");
        }

        System.out.println("Using online dump file");
        return onlineDumpFile;
    }
}
