package org.wikidata.wdanalysis.Fetcher;

import org.wikidata.wdanalysis.DumpFile.JsonLabsDumpFile;
import org.wikidata.wdanalysis.DumpFile.JsonLocalDumpFileImpl;
import org.wikidata.wdanalysis.Fetcher.DumpDateFetcher;
import org.wikidata.wdtk.dumpfiles.MwDumpFile;
import org.wikidata.wdtk.dumpfiles.wmf.JsonOnlineDumpFile;
import org.wikidata.wdtk.util.DirectoryManager;
import org.wikidata.wdtk.util.DirectoryManagerImpl;
import org.wikidata.wdtk.util.WebResourceFetcher;
import org.wikidata.wdtk.util.WebResourceFetcherImpl;

import java.io.File;
import java.io.IOException;

/**
 * @author Addshore
 */
public class DumpFetcher {

    /**
     * Look for the most recent dump date online and try to retrieve as dump object with fallback:
     * 1 - Look for dumps in location expected on labs
     * 2 - Look for dumps in the dump directory for this tool
     * 3 - Look online & download dumps
     *
     * @return MwDumpFile
     * @throws IOException
     */
    public MwDumpFile getMostRecentDump() throws IOException {
        DumpDateFetcher dateFetcher = new DumpDateFetcher();
        String latestDumpDate = dateFetcher.getLatestOnlineDumpDate();
        System.out.println("Latest dump date stamp is " + latestDumpDate);

        // 1) Try to process the file from labs storage
        JsonLabsDumpFile labsDumpFile = new JsonLabsDumpFile(latestDumpDate);
        if (labsDumpFile.isAvailable()) {
            System.out.println("Using dump file from labs storage");
            return labsDumpFile;
        }

        // 2) Try to use our local storage directory
        JsonLocalDumpFileImpl localDumpFile = new JsonLocalDumpFileImpl(latestDumpDate);
        if (localDumpFile.isAvailable()) {
            System.out.println("Using dump file from local storage");
            return localDumpFile;
        }

        // 3) Fallback to downloading the dump ourselves
        DirectoryManager localDirectoryManager = new DirectoryManagerImpl(System.getProperty("user.dir") + File.pathSeparator + "dumpfiles");
        WebResourceFetcher fetcher = new WebResourceFetcherImpl();
        JsonOnlineDumpFile onlineDumpFile = new JsonOnlineDumpFile(
                latestDumpDate,
                "wikidatawiki",
                fetcher,
                localDirectoryManager
        );
        if (onlineDumpFile.isAvailable()) {
            System.out.println("Using online dump file");
            return onlineDumpFile;
        }

        throw new IOException("Failed to get most recent dump from any sources");
    }
}