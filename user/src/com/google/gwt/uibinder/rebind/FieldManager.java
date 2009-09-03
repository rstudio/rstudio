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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * This class handles all {@link FieldWriter} instances created for the current
 * template.
 */
class FieldManager {

  private static final String DUPLICATE_FIELD_ERROR = "Duplicate declaration of field %1$s.";

  /**
   * Map of field name to FieldWriter. Note its a LinkedHashMap--we want to
   * write these out in the order they're declared.
   */
  private final LinkedHashMap<String, FieldWriter> fieldsMap = new LinkedHashMap<String, FieldWriter>();

  /**
   * A stack of the fields.
   */
  private final LinkedList<FieldWriter> parsedFieldStack = new LinkedList<FieldWriter>();

  private TreeLogger logger;

  /**
   * Basic constructor just injects an oracle instance.
   */
  public FieldManager(TreeLogger logger) {
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
   * Used to declare fields of a type that is to be generated. If your field
   * will hold a reference of an existing tyupe, see {@link #registerField}.
   * <p>
   * When making a field we peek at the {@link #parsedFieldStack} to make sure
   * that the field that holds the widget currently being parsed will depended
   * upon the field being declared. This ensures, for example, that dom id
   * fields (see {@link #declareDomIdHolder()}) used by an HTMLPanel will be
   * declared before it is.
   *
   * @param typeName the full qualified name for the class associated with the
   *          field
   * @param fieldName the name of the field
   * @throws UnableToCompleteException on duplicate name
   * @return a new {@link FieldWriter} instance
   */
  public FieldWriter registerFieldOfGeneratedType(String typePackage,
      String typeName, String fieldName) throws UnableToCompleteException {
    FieldWriter field = new FieldWriterOfGeneratedType(typePackage, typeName,
        fieldName);
    return registerField(fieldName, field);
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
      field.write(writer, logger);
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
      Object[] params = {fieldName};
      logger.log(TreeLogger.ERROR, String.format(DUPLICATE_FIELD_ERROR, params));
      throw new UnableToCompleteException();
    }
  }
}
