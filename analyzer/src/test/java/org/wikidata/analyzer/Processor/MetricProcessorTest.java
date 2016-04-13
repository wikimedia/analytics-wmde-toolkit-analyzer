package org.wikidata.analyzer.Processor;

import junit.framework.TestCase;
import org.wikidata.wdtk.datamodel.helpers.*;
import org.wikidata.wdtk.datamodel.implementation.DataObjectFactoryImpl;
import org.wikidata.wdtk.datamodel.implementation.ItemIdValueImpl;
import org.wikidata.wdtk.datamodel.implementation.PropertyIdValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Addshore
 */
public class MetricProcessorTest extends TestCase {

    private void assertCounter( Map<String, Long> counters, String counter, int expected ) {
        assertTrue( "Assert counter name exists '" + counter + "'", counters.containsKey( counter ) );
        assertEquals( "Assert counter '" + counter + "'value correct", (long)expected, (long)counters.get( counter ) );
    }

    public void testProcessItemDocument() throws Exception {
        Map<String, Long> counters = new HashMap<>();
        MetricProcessor processor = new MetricProcessor();
        processor.overrideCounters( counters );

        ItemIdValue itemId = ItemIdValueImpl.create("Q42", "foo");
        ItemDocument itemDoc = ItemDocumentBuilder.forItemId(itemId)
                .withStatement(
                        StatementBuilder
                                .forSubjectAndProperty(itemId, PropertyIdValueImpl.create("P1", "bar"))
                                .withQualifier(Datamodel.makeValueSnak(PropertyIdValueImpl.create("P1", "bar"), Datamodel.makeStringValue("baz")))
                                .withReference(ReferenceBuilder.newInstance().withNoValue(PropertyIdValueImpl.create("P143", "Foo")).build())
                                .withReference(ReferenceBuilder.newInstance().withSomeValue(PropertyIdValueImpl.create("P99", "Foo")).build())
                                .withReference(ReferenceBuilder.newInstance().withPropertyValue(
                                        PropertyIdValueImpl.create("P2", "Foo"),
                                        new DataObjectFactoryImpl().getStringValue("")
                                ).build())
                                .build()
                )
                .withStatement(
                        StatementBuilder
                                .forSubjectAndProperty(itemId, PropertyIdValueImpl.create("P1", "bar"))
                                .withQualifier(Datamodel.makeValueSnak(PropertyIdValueImpl.create("P1", "bar"), Datamodel.makeStringValue("baz")))
                                .withQualifier(Datamodel.makeValueSnak(PropertyIdValueImpl.create("P1", "bar"), Datamodel.makeStringValue("baz")))
                                .withReference(
                                        ReferenceBuilder.newInstance()
                                                .withPropertyValue(
                                                        PropertyIdValueImpl.create("P55", "Foo"),
                                                        new DataObjectFactoryImpl().getStringValue("")
                                                )
                                                .withNoValue(PropertyIdValueImpl.create("P66", "Foo"))
                                                .build())
                                .build()
                )
                .withStatement(
                        StatementBuilder
                        .forSubjectAndProperty(itemId, PropertyIdValueImpl.create("P100", "foo"))
                        .build()
                )
                .build();

        PropertyIdValue propId = PropertyIdValueImpl.create("P166", "foo");
        PropertyDocument propDoc = PropertyDocumentBuilder.forPropertyIdAndDatatype( propId, "dataTypeFoo" )
                .withStatement(
                        StatementBuilder
                                .forSubjectAndProperty(propId, PropertyIdValueImpl.create("P1", "bar"))
                                .build()
                ).build();



        processor.doPreProcessing();
        processor.processItemDocument( itemDoc );
        processor.processPropertyDocument( propDoc );
        processor.doPostProcessing();

        this.assertCounter(counters, "qualifiers", 3);
        this.assertCounter(counters, "references", 4);
        this.assertCounter(counters, "statements.referenced", 2 );
        this.assertCounter(counters, "statements.unreferenced", 1 );
        this.assertCounter(counters, "references.snaks", 5 );
        this.assertCounter(counters, "references.snaks.prop.P143", 1 );
        this.assertCounter(counters, "references.snaks.type.value", 2 );
        this.assertCounter(counters, "references.snaks.type.somevalue", 1 );
        this.assertCounter(counters, "references.snaks.type.novalue", 2 );
        this.assertCounter(counters, "item.count", 1 );
        this.assertCounter(counters, "item.statements.total", 3 );
        this.assertCounter(counters, "item.statements.avg", 3 );
        this.assertCounter(counters, "property.count", 1 );
        this.assertCounter(counters, "property.statements.total", 1 );
        this.assertCounter(counters, "property.statements.avg", 1 );
    }

}