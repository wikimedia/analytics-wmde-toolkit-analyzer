package main.java.org.wikidata.analyzer.Processor;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wikidata.wdtk.datamodel.implementation.PropertyIdValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.*;

import java.io.*;
import java.util.*;

/**
 * MapProcessor for wikidata-analysis
 *
 * @author Addshore
 */
public class MapProcessor extends WikidataAnalyzerProcessor {

    private JSONObject geoDataOut = new JSONObject();
    private JSONObject graphOut = new JSONObject();

    private PropertyIdValue coordinateLocation = PropertyIdValueImpl.create("P625", "http://www.wikidata.org/entity/");

    // This list is filled in the constructor
    private List<String> graphRelations = new ArrayList<>();

    public MapProcessor() {
        this.populateGraphRelations();
    }

    public boolean tearDown() {
        boolean success = true;

        System.out.println("Writing map wdlabel.json");
        File mapLabelFile = new File(outputDir.getAbsolutePath() + File.separator + "wdlabel.json");
        try {
            BufferedWriter mapLabelWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mapLabelFile)));
            this.geoDataOut.writeJSONString(mapLabelWriter);
            mapLabelWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        System.out.println("Writing map graph.json");
        File mapGraphFile = new File(outputDir.getAbsolutePath() + File.separator + "graph.json");
        try {
            BufferedWriter mapGraphWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mapGraphFile)));
            this.graphOut.writeJSONString(mapGraphWriter);
            mapGraphWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        return success;
    }

    public void populateGraphRelations() {
        // Fill the list of graphRelations
        // The previous script actually generated graph data for ALL properties
        this.graphRelations.add("P17");// Country
        this.graphRelations.add("P36");// Capital
        this.graphRelations.add("P47");// Shared border with
        this.graphRelations.add("P138");// Named after
        this.graphRelations.add("P150");// Subdivision
        this.graphRelations.add("P190");// Twin cities
        this.graphRelations.add("P197");// Adjacent station
        this.graphRelations.add("P403");// Mouth of watercourse
    }

    @Override
    public void processItemDocument(ItemDocument item) {
        boolean itemHasCoordinateValue = false;

        for (Iterator<Statement> statements = item.getAllStatements(); statements.hasNext(); ) {
            Statement statement = statements.next();
            if (statement.getClaim().getMainSnak().getPropertyId().equals(this.coordinateLocation)) {
                Snak snak = statement.getClaim().getMainSnak();
                if (snak instanceof ValueSnak) {
                    itemHasCoordinateValue = true;
                    GlobeCoordinatesValue value = (GlobeCoordinatesValue) ((ValueSnak) snak).getValue();
                    JSONObject geoData = new JSONObject();
                    geoData.put("x", value.getLatitude());
                    geoData.put("y", value.getLongitude());
                    MonolingualTextValue label = item.getLabels().get("en");
                    if (label == null) {
                        // TODO fallback of label
                        geoData.put("label", "-");
                    } else {
                        geoData.put("label", label.getText());
                    }
                    this.geoDataOut.put(item.getItemId().getId(), geoData);
                }
                break;
            }
        }

        if (itemHasCoordinateValue) {
            for (StatementGroup statementGroup : item.getStatementGroups()) {
                String statementGroupPropertyString = statementGroup.getProperty().getId();
                if (this.graphRelations.contains(statementGroupPropertyString)) {
                    // TODO rather than doing the below if in this loop, it could be done once in the constructor
                    if (!this.graphOut.containsKey(statementGroupPropertyString)) {
                        this.graphOut.put(statementGroupPropertyString, new JSONObject());
                    }
                    JSONArray graphData = new JSONArray();
                    for (Statement statement : statementGroup.getStatements()) {
                        Snak snak = statement.getClaim().getMainSnak();
                        if (snak instanceof ValueSnak) {
                            ItemIdValue value = (ItemIdValue) ((ValueSnak) snak).getValue();
                            graphData.add(value.getId());
                        }
                    }
                    ((JSONObject) this.graphOut.get(statementGroupPropertyString)).put(item.getItemId().getId(), graphData);
                }
            }
        }
    }

    @Override
    public void processPropertyDocument(PropertyDocument property) {
    }

}