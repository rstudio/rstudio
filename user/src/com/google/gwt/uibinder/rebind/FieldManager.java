/*
 * Copyright 2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.uibinder.rebind.model.ImplicitCssResource;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * This class handles all {@link FieldWriter} instances created for the current
 * template.
 */
public class FieldManager {

  private static final String DUPLICATE_FIELD_ERROR = "Duplicate declaration of field %1$s.";

  private final TypeOracle types;
  private final MortalLogger logger;

  /**
   * Map of field name to FieldWriter. Note its a LinkedHashMap--we want to
   * write these out in the order they're declared.
   */
  private final LinkedHashMap<String, FieldWriter> fieldsMap = new LinkedHashMap<String, FieldWriter>();

  /**
   * A stack of the fields.
   */
  private final LinkedList<FieldWriter> parsedFieldStack = new LinkedList<FieldWriter>();

  private LinkedHashMap<String, FieldReference> fieldReferences = new LinkedHashMap<String, FieldReference>();

  public FieldManager(TypeOracle types, MortalLogger logger) {
    this.types = types;
    this.logger = logger;
  }

  /**
   * @param fieldName the name of the {@link FieldWriter} to find
   * @return the {@link FieldWriter} instance indexed by fieldName or
   *         <b>null</b> in case fieldName is not found
   */
  public FieldWriter lookup(String fieldName) {
    return fieldsMap.get(fieldName);
  }

  /**
   * Remove the field at the top of the {@link #parsedFieldStack}.
   */
  public void pop() {
    parsedFieldStack.removeFirst();
  }

  /**
   * @param fieldWriter the field to push on the top of the
   *          {@link #parsedFieldStack}
   */
  public void push(FieldWriter fieldWriter) {
    parsedFieldStack.addFirst(fieldWriter);
  }

  /**
   * Used to declare fields of an existing type. If your field will hold a type
   * that is being generated, see {@link #registerFieldOfGeneratedType}.
   * <p>
   * When making a field we peek at the {@link #parsedFieldStack} to make sure
   * that the field that holds the widget currently being parsed will depended
   * upon the field being declared. This ensures, for example, that dom id
   * fields (see {@link #declareDomIdHolder()}) used by an HTMLPanel will be
   * declared before it is.
   * 
   * @param fieldType the type of the new field
   * @param fieldName the name of the new field
   * @return a new {@link FieldWriter} instance
   * @throws UnableToCompleteException on duplicate name
   */
  public FieldWriter registerField(JClassType fieldType, String fieldName)
      throws UnableToCompleteException {
    FieldWriter field = new FieldWriterOfExistingType(fieldType, fieldName,
        logger);
    return registerField(fieldName, field);
  }

  /**
   * Used to declare fields that will hold generated instances generated
   * CssResource interfaces. If your field will hold a reference of an existing
   * type, see {@link #registerField}. For other generated types, use
   * {@link #registerFieldOfGeneratedType}
   * {@link #registerFieldForGeneratedCssResource}.
   * <p>
   * When making a field we peek at the {@link #parsedFieldStack} to make sure
   * that the field that holds the widget currently being parsed will depended
   * upon the field being declared. This ensures, for example, that dom id
   * fields (see {@link #declareDomIdHolder()}) used by an HTMLPanel will be
   * declared before it is.
   * 
   * @throws UnableToCompleteException on duplicate name
   * @return a new {@link FieldWriter} instance
   */
  public FieldWriter registerFieldForGeneratedCssResource(
      ImplicitCssResource cssResource) throws UnableToCompleteException {
    FieldWriter field = new FieldWriterOfGeneratedCssResource(
        types.findType(String.class.getCanonicalName()), cssResource, logger);
    return registerField(cssResource.getName(), field);
  }

  /**
   * Used to declare fields of a type (other than CssResource) that is to be
   * generated. If your field will hold a reference of an existing type, see
   * {@link #registerField}. For generated CssResources, see
   * {@link #registerFieldForGeneratedCssResource}.
   * <p>
   * When making a field we peek at the {@link #parsedFieldStack} to make sure
   * that the field that holds the widget currently being parsed will depended
   * upon the field being declared. This ensures, for example, that dom id
   * fields (see {@link #declareDomIdHolder()}) used by an HTMLPanel will be
   * declared before it is.
   * 
   * @param assignableType class or interface extened or implemented by this
   *          type
   * @param typeName the full qualified name for the class associated with the
   *          field
   * @param fieldName the name of the field
   * @throws UnableToCompleteException on duplicate name
   * @return a new {@link FieldWriter} instance
   */
  public FieldWriter registerFieldOfGeneratedType(JClassType assignableType,
      String typePackage, String typeName, String fieldName)
      throws UnableToCompleteException {
    FieldWriter field = new FieldWriterOfGeneratedType(assignableType,
        typePackage, typeName, fieldName, logger);
    return registerField(fieldName, field);
  }

  /**
   * Called to register a <code>{field.reference}</code> encountered during
   * parsing, to be validated against the type oracle once parsing is complete.
   * 
   * @throws UnableToCompleteException
   */
  public void registerFieldReference(String fieldReferenceString, JType type) {
    FieldReference fieldReference = fieldReferences.get(fieldReferenceString);

    if (fieldReference == null) {
      fieldReference = new FieldReference(fieldReferenceString, this, types);
      fieldReferences.put(fieldReferenceString, fieldReference);
    }

    fieldReference.addLeftHandType(type);
  }

  /**
   * To be called after parsing is complete. Surveys all
   * <code>{field.reference}</code>s and checks they refer to existing types,
   * and have appropriate return types.
   * 
   * @throws UnableToCompleteException if any <code>{field.references}</code>
   *           can't be resolved
   */
  public void validate() throws UnableToCompleteException {
    boolean failed = false;

    for (Map.Entry<String, FieldReference> entry : fieldReferences.entrySet()) {
      FieldReference ref = entry.getValue();
      MonitoredLogger monitoredLogger = new MonitoredLogger(
          logger.getTreeLogger().branch(Type.TRACE, "validating " + ref));
      ref.validate(monitoredLogger);
      failed |= monitoredLogger.hasErrors();
    }
    if (failed) {
      throw new UnableToCompleteException();
    }
  }

  /**
   * Writes all stored gwt fields.
   * 
   * @param writer the writer to output
   * @param ownerTypeName the name of the class being processed
   */
  public void writeGwtFieldsDeclaration(IndentedWriter writer,
      String ownerTypeName) throws UnableToCompleteException {
    Collection<FieldWriter> fields = fieldsMap.values();
    for (FieldWriter field : fields) {
      field.write(writer);
    }
  }

  private FieldWriter registerField(String fieldName, FieldWriter field)
      throws UnableToCompleteException {
    requireUnique(fieldName);
    fieldsMap.put(fieldName, field);

    if (parsedFieldStack.size() > 0) {
      parsedFieldStack.getFirst().needs(field);
    }

    return field;
  }

  private void requireUnique(String fieldName) throws UnableToCompleteException {
    if (fieldsMap.containsKey(fieldName)) {
      logger.die(DUPLICATE_FIELD_ERROR, fieldName);
    }
  }
}
