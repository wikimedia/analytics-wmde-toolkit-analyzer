package org.wikidata.analyzer.Processor;

import com.google.common.collect.Iterators;
import org.json.simple.JSONObject;
import org.wikidata.analyzer.Fetcher.RefPropFetcher;
import org.wikidata.analyzer.Fetcher.WikimediasFetcher;
import org.wikidata.wdtk.datamodel.interfaces.*;

import java.io.*;
import java.util.*;

/**
 * @author Addshore
 */
public class MetricProcessor extends WikidataAnalyzerProcessor {

    private Map<String, Double> counters = new HashMap<>();

    private Map<String, String> wikimedias = new HashMap<>();

    private List<String> referenceProperties = new ArrayList<>();

    public MetricProcessor() {
        super();
        this.populateWikimedias();
        this.populateReferenceProperties();
    }

    public void overrideCounters(Map<String, Double> counters) {
        this.counters = counters;
    }

    public void doPostProcessing() {
        // Quickly work out the average statements per item & property
        this.initiateCounterIfNotReady("item.statements.avg");
        this.initiateCounterIfNotReady("property.statements.avg");
        this.counters.put("item.statements.avg", this.counters.get("item.statements.total") / this.counters.get("item.count"));
        this.counters.put("property.statements.avg", this.counters.get("property.statements.total") / this.counters.get("property.count"));
    }

    public boolean tearDown() {
        // And then do the real tearDown
        try {
            File metricsJsonFile = new File(outputDir.getAbsolutePath() + File.separator + "metrics.json");
            BufferedWriter metricsJsonWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(metricsJsonFile)));
            new JSONObject(this.counters).writeJSONString(metricsJsonWriter);
            metricsJsonWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void populateWikimedias() {
        WikimediasFetcher fetcher = new WikimediasFetcher(outputDir);
        List<HashMap<String, String>> mediawikis = fetcher.getMediawikis();

        for (HashMap<String, String> entry : mediawikis) {
            this.wikimedias.put(entry.get(WikimediasFetcher.ENTITYID_KEY), entry.get(WikimediasFetcher.DBNAME_KEY));
        }
    }

    private void populateReferenceProperties() {
        RefPropFetcher fetcher = new RefPropFetcher(outputDir);
        this.referenceProperties = fetcher.getReferenceProperties();
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

    public void processItemDocument(ItemDocument document) {
        if (document != null) {
            this.increment("item.count");
            this.increment("item.statements.total", Iterators.size( document.getAllStatements() ));
            this.processStatementDocument(document);
        }
    }

    public void processPropertyDocument(PropertyDocument document) {
        if (document != null) {
            this.increment("property.count");
            this.increment("property.statements.total", Iterators.size( document.getAllStatements() ));
        }
    }

    private void processStatementDocument(StatementDocument document) {
        for (Iterator<Statement> statementIterator = document.getAllStatements(); statementIterator.hasNext(); ) {
            Statement statement = statementIterator.next();
            processStatement(statement);
        }
    }

    private void processStatement(Statement statement) {
        this.increment("qualifiers", Iterators.size(statement.getClaim().getAllQualifiers()));
        this.increment("references", statement.getReferences().size());

        if( statement.getReferences().size() == 0 ) {
            this.increment("statements.unreferenced");
        } else {
            this.increment("statements.referenced");
        }

        for (Reference reference : statement.getReferences()) {
            processReference(reference);
        }
    }

    private void processReference(Reference reference) {
        this.increment("references.snaks", Iterators.size(reference.getAllSnaks()));

        for( Iterator<Snak> snaks = reference.getAllSnaks(); snaks.hasNext(); ) {
            Snak snak = snaks.next();
            processReferenceSnak(snak);
        }
    }

    private void processReferenceSnak(Snak snak) {
        String propertyId = snak.getPropertyId().getId();

        //Only count the counts of non-"external id" property snaks intended for references
        if( this.referenceProperties.contains( propertyId ) ) {
            this.increment("references.snaks.prop." + propertyId);
        }

        if (snak instanceof ValueSnak) {
            this.increment("references.snaks.type.value");
            this.processReferenceValueSnak((ValueSnak)snak);
        } else if (snak instanceof SomeValueSnak) {
            this.increment("references.snaks.type.somevalue");
        } else if (snak instanceof NoValueSnak) {
            this.increment("references.snaks.type.novalue");
        }
    }

    private void processReferenceValueSnak(ValueSnak snak) {
        String propertyId = snak.getPropertyId().getId();
        //Look for snaks indicating a Wikimedia reference
        //Note: P143 (imported from), P248 (stated in)
        if( propertyId.equals( "P143" ) || propertyId.equals("P248") ) {
            //Note: must always be an EntityIdValue for the properties above
            EntityIdValue entityIdValue = (EntityIdValue) snak.getValue();
            if( this.wikimedias.containsKey( entityIdValue.getId() ) ) {
                this.increment("references.snaks.wm");
                this.increment("references.snaks.wm." + this.wikimedias.get( entityIdValue.getId() ));
            }
        }
    }

}
