/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.index.schema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.MultiUpdate;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.ReplaceField;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.Update;
import org.roda.core.data.exceptions.GenericException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaBuilder {
  private static final Logger LOGGER = LoggerFactory.getLogger(SchemaBuilder.class);

  private final Set<Field> fields = new HashSet<>();
  private final Set<Field> replaceFields = new HashSet<>();
  private final Set<CopyField> copyFields = new HashSet<>();
  private final Set<DynamicField> dynamicFields = new HashSet<>();

  public Field replaceField(Field field) {
    replaceFields.add(field);
    return field;
  }

  public Field addField(Field field) {
    fields.add(field);
    return field;
  }

  /**
   * @see https://lucene.apache.org/solr/guide/defining-fields.html
   *
   * @param name
   * @param type
   * @return
   */
  public Field addField(String name, String type) {
    return addField(new Field(name, type));
  }

  public DynamicField addDynamicField(DynamicField field) {
    dynamicFields.add(field);
    return field;
  }

  public DynamicField addDynamicField(String name, String type) {
    return addDynamicField(new DynamicField(name, type));
  }

  public CopyField addCopyField(CopyField copyField) {
    copyFields.add(copyField);
    return copyField;
  }

  public CopyField addCopyField(String source, List<String> destinations) {
    return addCopyField(new CopyField(source, destinations));
  }

  public CopyField addCopyField(String source, String... destinations) {
    return addCopyField(new CopyField(source, Arrays.asList(destinations)));
  }

  public boolean isEmpty() {
    return fields.isEmpty() && replaceFields.isEmpty() && dynamicFields.isEmpty() && copyFields.isEmpty();
  }

  public void build(SolrClient client, String collection) throws GenericException {
    List<Update> updates = new ArrayList<>();

    fields.forEach(f -> updates.add(f.buildCreate()));
    replaceFields.forEach(f -> updates.add(new ReplaceField(f.getFieldAttributes())));
    dynamicFields.forEach(df -> updates.add(df.buildCreate()));
    copyFields.forEach(cf -> updates.add(cf.buildCreate()));

    MultiUpdate multi = new MultiUpdate(updates);
    try {
      LOGGER.info("Updating {} collection schema: {} new fields, {} replaced fields, {} dynamic fields, {} copy fields",
        collection, fields.size(), replaceFields.size(), dynamicFields.size(), copyFields.size());

      multi.process(client, collection);

      client.commit(collection);
    } catch (SolrServerException | IOException e) {
      LOGGER.error("Error bootstraping schemas", e);
      throw new GenericException("Error bootstraping schemas", e);
    } 
  }

}
