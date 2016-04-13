package org.wikidata.analyzer.Fetcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.dumpfiles.DumpContentType;
import org.wikidata.wdtk.dumpfiles.MwDumpFile;
import org.wikidata.wdtk.dumpfiles.wmf.WmfDumpFile;
import org.wikidata.wdtk.util.CompressionType;
import org.wikidata.wdtk.util.DirectoryManager;
import org.wikidata.wdtk.util.WebResourceFetcher;

import java.io.IOException;
import java.io.InputStream;

/**
 * Class to download dumps from archive.org
 *
 * @author Addshore
 */
class ArchiveOrgJsonOnlineDumpFile extends WmfDumpFile implements MwDumpFile {

    private static final Logger logger = LoggerFactory
            .getLogger(ArchiveOrgJsonOnlineDumpFile.class);

    private final WebResourceFetcher webResourceFetcher;
    private final DirectoryManager dumpfileDirectoryManager;

    private boolean isPrepared;

    /**
     * Constructor. Currently only "wikidatawiki" is supported as a project.
     *
     * @param dateStamp
     *            dump date in format YYYYMMDD
     * @param projectName
     *            project name string (e.g. "wikidatawiki")
     * @param webResourceFetcher
     *            object to use for accessing the web
     * @param dumpfileDirectoryManager
     *            the directory manager for the directory where dumps should be
     *            downloaded to
     */
    ArchiveOrgJsonOnlineDumpFile(String dateStamp, String projectName,
                                 WebResourceFetcher webResourceFetcher,
                                 DirectoryManager dumpfileDirectoryManager) {
        super(dateStamp, projectName);
        this.webResourceFetcher = webResourceFetcher;
        this.dumpfileDirectoryManager = dumpfileDirectoryManager;
    }

    @Override
    public DumpContentType getDumpContentType() {
        return DumpContentType.JSON;
    }

    @Override
    protected boolean fetchIsDone() {
        return true;
    }

    @Override
    public InputStream getDumpFileStream() throws IOException {
        prepareDumpFile();

        String fileName = WmfDumpFile.getDumpFileName(DumpContentType.JSON,
                this.projectName, this.dateStamp);
        DirectoryManager dailyDirectoryManager = this.dumpfileDirectoryManager
                .getSubdirectoryManager(WmfDumpFile.getDumpFileDirectoryName(
                        DumpContentType.JSON, this.dateStamp));

        return dailyDirectoryManager.getInputStreamForFile(
                fileName,
                CompressionType.GZIP
        );
    }

    @Override
    public void prepareDumpFile() throws IOException {
        if (this.isPrepared) {
            return;
        }

        String fileName = WmfDumpFile.getDumpFileName(DumpContentType.JSON,
                this.projectName, this.dateStamp);

        // Like http://archive.org/download/wikidata-json-20160104/wikidata-20160104-all.json.gz
        String urlString =  "http://archive.org/download/wikidata-json-" + this.dateStamp + "/wikidata-" + this.dateStamp + "-all.json.gz";

        logger.info("Downloading JSON dump file " + fileName + " from "
                + urlString + " ...");

        if (!isAvailable()) {
            throw new IOException(
                    "Dump file not available (yet). Aborting dump retrieval.");
        }

        DirectoryManager dailyDirectoryManager = this.dumpfileDirectoryManager
                .getSubdirectoryManager(WmfDumpFile.getDumpFileDirectoryName(
                        DumpContentType.JSON, this.dateStamp));

        try (InputStream inputStream = webResourceFetcher
                .getInputStreamForUrl(urlString)) {
            dailyDirectoryManager.createFileAtomic(fileName, inputStream);
        }

        this.isPrepared = true;

        logger.info("... completed download of JSON dump file " + fileName
                + " from " + urlString);
    }
}
