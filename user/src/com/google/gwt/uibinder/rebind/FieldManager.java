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
import com.google.gwt.core.ext.typeinfo.TypeOracle;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * This class handles all {@link FieldWriter} instances created for the
 * current template.
 */
class FieldManager {

  private static final String NO_DEFAULT_CTOR_ERROR =
      "%1$s has no default (zero args) constructor. To fix this, you can define"
      + " a @UiFactory method on %2$s, or annotate a constructor of %3$s with"
      + " @UiConstructor.";

  private static final String DUPLICATED_FIELD_ERROR =
      "Duplicate declaration of field %1$s.";

  /**
   * Map of field name to FieldWriter. Note its a LinkedHashMap--we want to
   * write these out in the order they're declared.
   */
  private final LinkedHashMap<String, FieldWriter> fieldsMap =
      new LinkedHashMap<String, FieldWriter>();

  /**
   * A stack of the fields.
   */
  private final LinkedList<FieldWriter> parsedFieldStack =
      new LinkedList<FieldWriter>();

  private TypeOracle oracle;
  private TreeLogger logger;

  /**
   * Basic constructor just injects an oracle instance.
   */
  public FieldManager(TypeOracle oracle, TreeLogger logger) {
    this.oracle = oracle;
    this.logger = logger;
  }

  /**
   * Post an error message and halt processing. This method always throws an
   * {@link UnableToCompleteException}
   */
  public void die(String message, Object... params)
      throws UnableToCompleteException {
    logger.log(TreeLogger.ERROR, String.format(message, params));
    throw new UnableToCompleteException();
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
   *        {@link #parsedFieldStack}
   */
  public void push(FieldWriter fieldWriter) {
    parsedFieldStack.addFirst(fieldWriter);
  }

  /**
   * This is where {@link FieldWriter} instances come from. When making a field
   * we peek at the {@link #parsedFieldStack} to make sure that the field that
   * holds the widget currently being parsed will depended upon the field being
   * declared. This ensures, for example, that dom id fields (see
   * {@link #declareDomIdHolder()}) used by an HTMLPanel will be declared
   * before it is.
   *
   * @param typeName the full qualified name for the class associated with the
   *        field
   * @param fieldName the name of the field
   * @return a new {@link FieldWriter} instance, <b>null</b> in case fieldName
   *         is already indexed
   */
  public FieldWriter registerField(String typeName, String fieldName)
      throws UnableToCompleteException {
    JClassType fieldType = oracle.findType(typeName);
    if (fieldsMap.containsKey(fieldName)) {
      die(DUPLICATED_FIELD_ERROR, fieldName);
    }

    FieldWriter field = new FieldWriter(fieldType, fieldName);
    fieldsMap.put(fieldName, field);

    if (parsedFieldStack.size() > 0) {
      parsedFieldStack.getFirst().needs(field);
    }

    return field;
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
      if (!field.write(writer)) {
        die(NO_DEFAULT_CTOR_ERROR,
          field.getType().getQualifiedSourceName(),
          ownerTypeName,
          field.getType().getName());
      }
    }
  }
}
