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
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.uibinder.rebind.model.ImplicitCssResource;
import com.google.gwt.uibinder.rebind.model.OwnerField;
import com.google.gwt.uibinder.rebind.model.OwnerClass;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * This class handles all {@link FieldWriter} instances created for the current
 * template.
 */
public class FieldManager {

  private static final String DUPLICATE_FIELD_ERROR = "Duplicate declaration of field %1$s.";

  private static final Comparator<FieldWriter> BUILD_DEFINITION_SORT =
      new Comparator<FieldWriter>() {
    public int compare(FieldWriter field1, FieldWriter field2) {
      // First get type precedence, if ties the field precedence is used.
      int precedence = field2.getFieldType().getBuildPrecedence()
          - field1.getFieldType().getBuildPrecedence();
      if (precedence == 0) {
        precedence = field2.getBuildPrecedence() - field1.getBuildPrecedence();
      }
      return precedence;
    }
  };

  public static String getFieldBuilder(String fieldName) {
    return String.format("build_%s()", fieldName);
  }

  public static String getFieldGetter(String fieldName) {
    return String.format("get_%s()", fieldName);
  }

  private final TypeOracle types;
  private final MortalLogger logger;

  /**
   * Map of field name to FieldWriter. Note its a LinkedHashMap--we want to
   * write these out in the order they're declared.
   */
  private final LinkedHashMap<String, FieldWriter> fieldsMap =
      new LinkedHashMap<String, FieldWriter>();

  /**
   * A stack of the fields.
   */
  private final LinkedList<FieldWriter> parsedFieldStack = new LinkedList<FieldWriter>();

  private LinkedHashMap<String, FieldReference> fieldReferences =
      new LinkedHashMap<String, FieldReference>();

  /**
   * Counts the number of times a getter field is called, this important to
   * decide which strategy to take when outputing getters and builders.
   * {@see com.google.gwt.uibinder.rebind.FieldWriter#writeFieldDefinition}.
   */
  private final Map<String, Integer> gettersCounter = new HashMap<String, Integer>();

  /**
   * Whether to use the new strategy of generating UiBinder code.
   */
  private final boolean useLazyWidgetBuilders;

  public FieldManager(TypeOracle types, MortalLogger logger, boolean useLazyWidgetBuilders) {
    this.types = types;
    this.logger = logger;
    this.useLazyWidgetBuilders = useLazyWidgetBuilders;
  }

  /**
   * Converts the given field to its getter. Example:
   *  <li> myWidgetX = get_myWidgetX()
   *  <li> f_html1 = get_f_html1()
   */
  public String convertFieldToGetter(String fieldName) {
    // TODO(hermes, rjrjr, rdcastro): revisit this and evaluate if this
    // conversion can be made directly in FieldWriter.
    if (!useLazyWidgetBuilders) {
      return fieldName;
    }

    int count = getGetterCounter(fieldName) + 1;
    gettersCounter.put(fieldName, count);
    return getFieldGetter(fieldName);
  }

  /**
   * Initialize with field builders the generated <b>Widgets</b> inner class.
   * {@see com.google.gwt.uibinder.rebind.FieldWriter#writeFieldBuilder}.
   */
  public void initializeWidgetsInnerClass(IndentedWriter w,
      OwnerClass ownerClass) throws UnableToCompleteException {

    FieldWriter[] fields = fieldsMap.values().toArray(
        new FieldWriter[fieldsMap.size()]);
    Arrays.sort(fields, BUILD_DEFINITION_SORT);

    for (FieldWriter field : fields) {
      int count = getGetterCounter(field.getName());
      field.writeFieldBuilder(w, count, ownerClass.getUiField(field.getName()));
    }
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
   * fields (see {@link UiBinderWriter#declareDomIdHolder()}) used by an HTMLPanel
   * will be declared before it is.
   *
   * @param fieldWriterType the field writer type associated
   * @param fieldType the type of the new field
   * @param fieldName the name of the new field
   * @return a new {@link FieldWriter} instance
   * @throws UnableToCompleteException on duplicate name
   */
  public FieldWriter registerField(FieldWriterType fieldWriterType,
      JClassType fieldType, String fieldName) throws UnableToCompleteException {
    FieldWriter field = new FieldWriterOfExistingType(
        fieldWriterType, fieldType, fieldName, logger);
    return registerField(fieldName, field);
  }

  public FieldWriter registerField(JClassType fieldType, String fieldName)
      throws UnableToCompleteException {
    return registerField(FieldWriterType.DEFAULT, fieldType, fieldName);
  }

  public FieldWriter registerField(String type, String fieldName)
      throws UnableToCompleteException {
    return registerField(types.findType(type), fieldName);
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
   * fields (see {@link UiBinderWriter#declareDomIdHolder()}) used by an HTMLPanel
   * will be declared before it is.
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
   * Register a new field for {@link com.google.gwt.uibinder.client.LazyDomElement}
   * types. LazyDomElement fields can only be associated with html elements. Example:
   *
   *  <li>LazyDomElement&lt;DivElement&gt; -&gt; &lt;div&gt;</li>
   *  <li>LazyDomElement&lt;Element&gt; -&gt; &lt;div&gt;</li>
   *  <li>LazyDomElement&lt;SpanElement&gt; -&gt; &lt;span&gt;</li>
   *
   * @param templateFieldType the html type to bind, eg, SpanElement, DivElement, etc
   * @param ownerField the field instance
   */
  public FieldWriter registerFieldForLazyDomElement(JClassType templateFieldType,
      OwnerField ownerField) throws UnableToCompleteException {
    if (ownerField == null) {
      throw new RuntimeException("Cannot register a null owner field for LazyDomElement.");
    }
    FieldWriter field = new FieldWriterOfLazyDomElement(
        templateFieldType, ownerField, logger);
    return registerField(ownerField.getName(), field);
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
   * fields (see {@link UiBinderWriter#declareDomIdHolder()}) used by an HTMLPanel
   * will be declared before it is.
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
   * Gets a FieldWriter given its name or throws a RuntimeException if not found.
   * @param fieldName the name of the {@link FieldWriter} to find
   * @return the {@link FieldWriter} instance indexed by fieldName
   */
  public FieldWriter require(String fieldName) {
    FieldWriter fieldWriter = lookup(fieldName);
    if (fieldWriter == null) {
      throw new RuntimeException("The required field %s doesn't exist.");
    }
    return fieldWriter;
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
          logger.getTreeLogger().branch(TreeLogger.TRACE, "validating " + ref));
      ref.validate(monitoredLogger);
      failed |= monitoredLogger.hasErrors();
    }
    if (failed) {
      throw new UnableToCompleteException();
    }
  }

  /**
   * Outputs the getter and builder definitions for all fields.
   * {@see com.google.gwt.uibinder.rebind.AbstractFieldWriter#writeFieldDefinition}.
   */
  public void writeFieldDefinitions(IndentedWriter writer, TypeOracle typeOracle,
      OwnerClass ownerClass, DesignTimeUtils designTime)
      throws UnableToCompleteException {
    Collection<FieldWriter> fields = fieldsMap.values();
    for (FieldWriter field : fields) {
      int counter = getGetterCounter(field.getName());
      field.writeFieldDefinition(
          writer,
          typeOracle,
          ownerClass.getUiField(field.getName()),
          designTime,
          counter,
          useLazyWidgetBuilders);
    }
  }

  /**
   * Writes all stored gwt fields.
   *
   * @param writer the writer to output
   */
  public void writeGwtFieldsDeclaration(IndentedWriter writer) throws UnableToCompleteException {
    Collection<FieldWriter> fields = fieldsMap.values();
    for (FieldWriter field : fields) {
      field.write(writer);
    }
  }

  /**
   * Gets the number of times a getter for the given field is called.
   */
  private int getGetterCounter(String fieldName) {
    Integer count = gettersCounter.get(fieldName);
    return (count == null) ? 0 : count;
  }

  private FieldWriter peek() {
    return parsedFieldStack.getFirst();
  }

  private FieldWriter registerField(String fieldName, FieldWriter field)
      throws UnableToCompleteException {
    requireUnique(fieldName);
    fieldsMap.put(fieldName, field);

    if (parsedFieldStack.size() > 0) {
      FieldWriter parent = peek();
      field.setBuildPrecedence(parent.getBuildPrecedence() + 1);
      parent.needs(field);
    }

    return field;
  }

  private void requireUnique(String fieldName) throws UnableToCompleteException {
    if (fieldsMap.containsKey(fieldName)) {
      logger.die(DUPLICATE_FIELD_ERROR, fieldName);
    }
  }
}
