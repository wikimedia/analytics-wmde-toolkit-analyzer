package main.java.org.wikidata.analyzer.Processor;

import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentProcessor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author Addshore
 *
 * To be implemented by every EntityDocumentProcessor to be run by WikidataAnalyzer
 */
public abstract class WikidataAnalyzerProcessor implements EntityDocumentProcessor {

    protected File outputDir;

    public WikidataAnalyzerProcessor(){
        try {
            this.outputDir = Files.createTempDirectory("WikidataAnalyzer-data").toFile();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void setOutputDir( File outputDir ) {
        this.outputDir = outputDir;
    }

    /**
     * Sets up the Processor.
     * Show open files ready for writing etc.
     */
    public void setUp(){};

    /**
     * Tears down the Processor.
     * Should close files & finish writing etc.
     * @return boolean success of the tearDown
     */
    public boolean tearDown(){
        return true;
    };

}
