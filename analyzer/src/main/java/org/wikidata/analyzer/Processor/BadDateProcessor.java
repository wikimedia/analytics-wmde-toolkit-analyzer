package org.wikidata.analyzer.Processor;

import org.wikidata.wdtk.datamodel.interfaces.*;

import java.io.*;
import java.util.*;

/**
 * BadDateProcessor for wikidata-analysis
 *
 * @author Addshore
 */
public class BadDateProcessor extends WikidataAnalyzerProcessor {

    private Writer writer1;
    private Writer writer2;

    public BadDateProcessor() {
        super();
    }

    public void overrideWriters(Writer writer1, Writer writer2) {
        this.writer1 = writer1;
        this.writer2 = writer2;
        this.addHeadersToWriters();
    }

    public void setUp() {
        File list1 = new File(outputDir.getAbsolutePath() + File.separator + "date_list1.txt");
        File list2 = new File(outputDir.getAbsolutePath() + File.separator + "date_list2.txt");

        try {
            writer1 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(list1)));
            writer2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(list2)));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        this.addHeadersToWriters();
    }

    private void addHeadersToWriters() {
        try {
            writer1.write("Dates marked as Julian that are more precise than year\n----\n");
            writer2.write("Dates marked as gregorian, before 1584\n----\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean tearDown() {
        try {
            writer1.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            writer2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void processItemDocument(ItemDocument item) {
        for (Iterator<Statement> statements = item.getAllStatements(); statements.hasNext(); ) {
            Statement statement = statements.next();
            Snak snak = statement.getClaim().getMainSnak();
            if (snak instanceof ValueSnak) {
                Value value = ((ValueSnak) snak).getValue();
                if (value instanceof TimeValue) {
                    TimeValue timeValue = (TimeValue) value;

                    //List1 - marked as Julian and are more precise than year
                    if (timeValue.getPreferredCalendarModel().equals(TimeValue.CM_JULIAN_PRO)
                            && timeValue.getPrecision() > 9) {
                        try {
                            this.writer1.write(statement.getStatementId() + "\n");
                        } catch (IOException e) {
                            System.out.println("Failed to write line to writer1");
                        }
                    }

                    //List2 - marked as gregorian, before 1584
                    if (timeValue.getPreferredCalendarModel().equals(TimeValue.CM_GREGORIAN_PRO)
                            && timeValue.getYear() < 1584) {
                        try {
                            this.writer2.write(statement.getStatementId() + "\n");
                        } catch (IOException e) {
                            System.out.println("Failed to write line to writer2");
                        }
                    }

                }
            }
        }
    }

    @Override
    public void processPropertyDocument(PropertyDocument property) {
    }

}