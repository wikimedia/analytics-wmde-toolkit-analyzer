package org.wikidata.analyzer;

import org.wikidata.analyzer.Fetcher.DumpDateFetcher;
import org.wikidata.analyzer.Fetcher.DumpFetcher;
import org.wikidata.analyzer.Processor.NoisyProcessor;
import org.wikidata.analyzer.Processor.WikidataAnalyzerProcessor;
import org.wikidata.wdtk.dumpfiles.DumpProcessingController;
import org.wikidata.wdtk.dumpfiles.MwDumpFile;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * @author Addshore
 */
public class WikidataAnalyzer {

    /**
     * Folder that all data should be stored in
     */
    private File dataDir = null;

    /**
     * The target date string
     */
    private String targetDate;

    /**
     * A list of processorClasses that need to be run
     */
    private List<Class<?>> processorClasses = new ArrayList<>();

    /**
     * A list of processor objects that are being run
     */
    private List<WikidataAnalyzerProcessor> processorObjects = new ArrayList<>();

    /**
     * Main entry point.
     * Instantiates and runs the analyzer
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) throws IOException {
        WikidataAnalyzer analyzer = new WikidataAnalyzer();
        analyzer.run(args);
    }

    /**
     * Main entry point of the WikidataAnalyzer class
     *
     * @param args Command line arguments
     *             A collection of Processors to run (each as a single argument) eg. BadDate Map
     *             The data directory to use eg. ~/data
     *             The date of the dump to target eg. latest OR 20151230
     *             eg. java -Xmx2g -jar ~/wikidata-analyzer.jar Reference ~/data latest
     */
    public void run( String[] args ) {
        this.printHeader();

        long startTime = System.currentTimeMillis();

        try {
            this.scan(args);
            System.out.println("All Done!");
        } catch (IOException e) {
            System.out.println("Something went wrong!");
            e.printStackTrace();
        }

        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("Execution time: " + elapsedSeconds / 60 + ":" + elapsedSeconds % 60);
    }

    private void printHeader() {
        System.out.println("**************************************************************************************");
        System.out.println("***                       Wikidata Toolkit: ToolkitAnalyzer                        ***");
        System.out.println("********************************** Example  Usage ************************************");
        System.out.println("* toolkit-analyzer.jar Metric ~/toolkit-analyzer/data latest                         *");
        System.out.println("* toolkit-analyzer.jar Metric ~/toolkit-analyzer/data 20160104                       *");
        System.out.println("* toolkit-analyzer.jar BadDate Map MonolingualTest ~/toolkit-analyzer/data 20150104  *");
        System.out.println("******************************* Data Directory Layout ********************************");
        System.out.println("* Data directory: data/                                                              *");
        System.out.println("* Downloaded dump locations: data/dumpfiles/json-<DATE>/<DATE>-all.json.gz           *");
        System.out.println("* Processor output location: data/<DATE>/                                            *");
        System.out.println("**************************************************************************************");
    }

    private void printMemoryWarning() {
        // Check memory limit
        if (Runtime.getRuntime().maxMemory() / 1024 / 1024 <= 1500) {
            System.out.println("WARNING: You may need to increase your memory limit!");
        }
    }

    public void scan(String[] args) throws IOException {
        // Get the parameters
        try {
            targetDate = args[args.length - 1];
            dataDir = new File(args[args.length - 2]);
        } catch (ArrayIndexOutOfBoundsException exception) {
            System.out.println("Error: Not enough parameters. You must pass a data dir and a target date!");
            System.exit(1);
        }

        this.printMemoryWarning();

        // Check the date
        if (targetDate.equals("latest")) {
            DumpDateFetcher dateFetcher = new DumpDateFetcher();
            targetDate = dateFetcher.getLatestOnlineDumpDate();
            System.out.println("Targeting latest dump: " + targetDate);
        } else if (targetDate.matches("[0-9]+")) {
            System.out.println("Targeting dump from: " + targetDate);
        } else {
            System.out.println("Error: Date looks wrong. Must be in the format '20160101' or 'latest'.");
            System.exit(1);
        }

        // Check the data directory
        if (!dataDir.exists()) {
            System.out.println("Error: Data directory specified does not exist.");
            System.exit(1);
        }
        System.out.println("Using data directory: " + dataDir.getAbsolutePath());

        // And create the output directory if it doesn't already exist
        File outputDir = new File(dataDir.getAbsolutePath() + File.separator + targetDate);
        if (!outputDir.exists()) {
            Files.createDirectory( outputDir.toPath() );
        }

        // Get the list of processorClasses
        for (String value : Arrays.copyOf(args, args.length - 2)) {
            try {
                processorClasses.add(Class.forName("org.wikidata.analyzer.Processor." + value + "Processor"));
            } catch (ClassNotFoundException e) {
                System.out.println("Error: " + value + "Processor not found");
                System.exit(1);
            }
            System.out.println(value + "Processor enabled");
        }

        // Set up controller
        DumpProcessingController controller = new DumpProcessingController("wikidatawiki");
        controller.setOfflineMode(false);

        // Set all the processors up and add them to the controller
        for (Class<?> classObject : this.processorClasses) {
            try {
                WikidataAnalyzerProcessor processor = (WikidataAnalyzerProcessor) classObject.newInstance();
                processor.setOutputDir( outputDir );
                processor.setUp();
                processor.doPreProcessing();
                controller.registerEntityDocumentProcessor(
                        processor,
                        null,
                        true
                );
                this.processorObjects.add( processor );
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        // Always add the noisy processor....
        controller.registerEntityDocumentProcessor(new NoisyProcessor(), null, true);

        // Fetch and process dump
        DumpFetcher fetcher = new DumpFetcher(dataDir);
        System.out.println("Fetching dump");
        MwDumpFile dump = fetcher.getDump(targetDate);
        System.out.println("Processing dump");
        controller.processDump(dump);
        System.out.println("Processed!");
        System.out.println("Memory Usage (MB): " + Runtime.getRuntime().totalMemory() / 1024 / 1024);

        // Tear all the processors down
        for (WikidataAnalyzerProcessor processor : this.processorObjects) {
            processor.doPostProcessing();
            processor.tearDown();
        }

    }

}
