package org.wikidata.analyzer.Processor;

import org.json.simple.JSONObject;
import org.wikidata.wdtk.datamodel.interfaces.*;

import java.io.*;
import java.util.*;

/**
 * ExactValueQuantityProcessor for wikidata-analysis
 *
 * @author Addshore
 */
public class ExactValueQuantityProcessor extends WikidataAnalyzerProcessor {

    private Map<String, Double> counters = new HashMap<>();

    public ExactValueQuantityProcessor() {
        super();
    }

    private void increment(String counter) {
        this.increment(counter, 1);
    }

    private void increment(String counter, double quantity) {
        this.initiateCounterIfNotReady(counter);
        this.counters.put(counter, this.counters.get(counter) + quantity);
    }

    private void initiateCounterIfNotReady(String counter) {
        if (!this.counters.containsKey(counter)) {
            this.counters.put(counter, (double) 0);
        }
    }

    public boolean tearDown() {
        try {
            File metricsJsonFile = new File(outputDir.getAbsolutePath() + File.separator + "exactValueQuantityMetrics.json");
            BufferedWriter metricsJsonWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(metricsJsonFile)));
            new JSONObject(this.counters).writeJSONString(metricsJsonWriter);
            metricsJsonWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void processItemDocument(ItemDocument item) {
        for (Iterator<Statement> statements = item.getAllStatements(); statements.hasNext(); ) {
            Statement statement = statements.next();

            this.processSnak(statement.getClaim().getMainSnak(), "main");


            for (Iterator<Snak> qualifiers = statement.getClaim().getAllQualifiers(); qualifiers.hasNext(); ) {
                this.processSnak(qualifiers.next(), "qualifier");
            }

            for (Reference reference : statement.getReferences()) {
                for (Iterator<Snak> referenceSnaks = reference.getAllSnaks(); referenceSnaks.hasNext(); ) {
                    this.processSnak(referenceSnaks.next(), "reference");
                }
            }
        }
    }

    private void processSnak(Snak snak, String type) {
        if (snak instanceof ValueSnak) {
            Value value = snak.getValue();
            if (value instanceof QuantityValue) {
                QuantityValue quantityValue = (QuantityValue) value;
                String propertyId = snak.getPropertyId().getId();

                this.increment("property." + propertyId);
                this.increment("type." + type + "." + propertyId);

                // number of values with +/-0 bounds (upper bound == lower bound)
                if (Objects.equals(quantityValue.getUpperBound(), quantityValue.getLowerBound())) {
                    this.increment("counters.noBound");
                    this.increment("propertyCounters.noBound." + propertyId);
                }

                /**
                 * number of values with no unit
                 * in the JSON dump this means units of value "1" or units pointing to Q199
                 * the toolkit converts "1" to "" in JacksonInnerQuantity::getUnit
                 */
                if (quantityValue.getUnit().equals("") || quantityValue.getUnit().equals("http://www.wikidata.org/entity/Q199")) {
                    this.increment("counters.noUnit");
                    this.increment("propertyCounters.noUnit." + propertyId);
                }

                // number of values with no decimal point (i.e. whole numbers)
                if (quantityValue.getNumericValue().scale() <= 0) {
                    this.increment("counters.noDecimal");
                    this.increment("propertyCounters.noDecimal." + propertyId);
                }

            }
        }
    }

    @Override
    public void processPropertyDocument(PropertyDocument property) {
    }

}