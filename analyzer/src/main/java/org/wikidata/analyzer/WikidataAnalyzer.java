package org.wikidata.analyzer;

import org.apache.commons.cli.*;
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
        analyzer.init(args);
    }

    public void init( String[] args ) throws IOException {
        Options options = new Options();

        options.addOption("h", "help", false, "Print help for the command");
        options.addOption("d", "date", true, "Target date in format 20160104");
        options.addOption("l", "latest", false, "Target the latest dump according to dumps.wikimedia.org");
        options.addOption("s", "store", true, "Target storage directory (REQUIRED)");
        options.addOption("p", "processors", true, "Processors to run (REQUIRED)");

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            // Output help when help was requested
            if (cmd.hasOption("help")) {
                this.printHelpAndExit(options);
            }
            if (!cmd.hasOption("store")) {
                this.printHelpAndExit(options, "Missing store option");
            }
            if (!cmd.hasOption("processors")) {
                this.printHelpAndExit(options, "Missing processors option");
            }

            // Extract the other things
            String targetDate = null;
            if (cmd.hasOption("latest")) {
                targetDate = "latest";
            } else if (cmd.hasOption("date")) {
                targetDate = cmd.getOptionValue("date");
            } else {
                this.printHelpAndExit(options, "Missing latest option or a date");
            }
            String dataDir = cmd.getOptionValue("store");
            String[] processors = cmd.getOptionValues("processors");

            this.run( targetDate, new File( dataDir ), processors );

        } catch (ParseException e) {
            this.printHelpAndExit( options, e.getMessage() );
        }
    }

    private void printHelpAndExit( Options options ) {
        this.printHelpAndExit( options, "" );
    }

    private void printHelpAndExit( Options options, String reason ) {
        this.printHeader();
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("toolkit-analyzer", options);
        if( !reason.equals("") ) {
            System.out.println( "\n" + reason );
        }
        System.exit(1);
    }

    private void printHeader() {
        System.out.println("****************************************************************************");
        System.out.println("***                       Wikidata Toolkit: ToolkitAnalyzer              ***");
        System.out.println("******************************* Data Directory Layout **********************");
        System.out.println("* Target storage directory : data/                                         *");
        System.out.println("* Downloaded dump locations: data/dumpfiles/json-<DATE>/<DATE>-all.json.gz *");
        System.out.println("* Processor output location: data/<DATE>/                                  *");
        System.out.println("****************************************************************************");
    }

    private void printMemoryWarning() {
        // Check memory limit
        if (Runtime.getRuntime().maxMemory() / 1024 / 1024 <= 1500) {
            System.out.println("WARNING: You may need to increase your memory limit!");
        }
    }

    public void run( String targetDate, File dataDir, String[] processors ) throws IOException {
        this.printHeader();

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

        long startTime = System.currentTimeMillis();

        try {
            this.scan(targetDate, dataDir, outputDir, processors);
            System.out.println("All Done!");
        } catch (IOException e) {
            System.out.println("Something went wrong!");
            e.printStackTrace();
        }

        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("Execution time: " + elapsedSeconds / 60 + ":" + elapsedSeconds % 60);
    }

    public void scan( String targetDate, File dataDir, File outputDir, String[] processors ) throws IOException {
        this.printMemoryWarning();

        // Get the list of processorClasses
        for (String value : processors) {
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
