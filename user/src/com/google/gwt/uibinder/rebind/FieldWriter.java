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
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.uibinder.rebind.model.OwnerField;

/**
 * Models a field to be written in the generated binder code. Note that this is
 * not necessarily a field that the user has declared. It's basically any
 * variable the generated UiBinder#createAndBindUi implementation will need.
 * <p>
 * A field can have a custom initialization statement, set via
 * {@link #setInitializer}. Without one it will be initialized via a
 * {@link com.google.gwt.core.client.GWT#create} call. (In the rare case that
 * you need a field not to be initialized, initialize it to "null".)
 * <p>
 * Dependencies can be declared between fields via {@link #needs}, to ensure
 * that one can be initialized via reference to another. Circular references are
 * not supported, nor detected.
 */
public interface FieldWriter {

  /**
   * Add a statement to be executed right after the current field is attached.
   *
   * <pre>
   *   HTMLPanel panel = new HTMLPanel(
   *      "<span id='someId' class='someclass'></span>...");
   *
   *   // statement section.
   *   widgetX.setStyleName("someCss");
   *   widgetX.setVisible(true);
   *
   *   // attach section.
   *   UiBinderUtil.TempAttachment attachRecord =
   *      UiBinderUtil.attachToDom(panel.getElement());
   *   get_domId0Element().get();
   *   get_domId1Element().get();
   *
   *   // detach section.
   *   attachRecord.detach();
   *   panel.addAndReplaceElement(get_someWidget(), get_domId0Element().get());
   *   panel.addAndReplaceElement(get_otherWidget(), get_domId1Element().get());
   * </pre>
   */
  void addAttachStatement(String format, Object... args);

  /**
   * Add a statement to be executed right after the current field is detached.
   * {@see #addAttachStatement}.
   */
  void addDetachStatement(String format, Object... args);

  /**
   * Add a statement for the given field, executed right after its creation. Example:
   *
   * <pre>
   *   WidgetX widgetX = GWT.create(WidgetX.class);
   *
   *   // statement section.
   *   widgetX.setStyleName("someCss");
   *   widgetX.setVisible(true);
   * </pre>
   */
  void addStatement(String format, Object... args);

  /**
   * Returns the type of this field, or for generated types the type it extends.
   */
  // TODO(rjrjr) When ui:style is able to implement multiple interfaces,
  // this will need to become a set
  JClassType getAssignableType();

  /**
   * Gets this field builder precedence.
   */
  int getBuildPrecedence();

  /**
   * Gets the type of this field.
   */
  FieldWriterType getFieldType();

  /**
   * Returns the string html representation of the field.
   */
  String getHtml();

  /**
   * Returns the custom initializer for this field, or null if it is not set.
   */
  String getInitializer();

  /**
   * Returns the type of this field, or null if this field is of a type that has
   * not yet been generated.
   */
  JClassType getInstantiableType();

  /**
   * Get the name of the field.
   */
  String getName();
  
  /**
   * Returns the expression that to evaluate to get the contents of this field.
   * Keeps a reference count, so if the field is to be used more than once it is
   * important to call this method each time.
   */
  String getNextReference();

  /**
   * Returns the qualified source name of this type.
   */
  String getQualifiedSourceName();

  /**
   * Returns the return type found at the end of the given method call
   * path, which must begin with the receiver's name, or null if the
   * path is invalid.
   */
  JType getReturnType(String[] path, MonitoredLogger logger);

  /**
   * Returns the string SafeHtml representation of the field.
   */
  String getSafeHtml();

  /**
   * Declares that the receiver depends upon the given field.
   */
  void needs(FieldWriter f);

  /**
   * Sets the precedence of this field builder. Field with higher values are
   * written first. This is automatically set by FieldManager when the field is
   * created. You should only change it somewhere else if you <b>really</b> know
   * what you are doing.
   */
  void setBuildPrecedence(int precedence);

  /**
   * Sets the html representation of the field for applicable field types.
   */
  void setHtml(String html);

  /**
   * Used to provide an initializer string to use instead of a
   * {@link com.google.gwt.core.client.GWT#create} call. Note that this is an
   * RHS expression. Don't include the leading '=', and don't end it with ';'.
   *
   * @throws IllegalStateException on second attempt to set the initializer
   */
  void setInitializer(String initializer);

  /**
   * Write the field declaration.
   */
  void write(IndentedWriter w) throws UnableToCompleteException;

  /**
   * Write this field builder in the <b>Widgets</b> inner class. There are 3
   * possible situations:
   *
   * <dl>
   *   <dt>getter never called</dt>
   *     <dd>write builder only if there's a ui:field associated</dd>
   *   <dt>getter called only once</dt>
   *     <dd>don't need to write the builder since the getter fallback to the
   *      builder when called</dd>
   *   <dt>getter called more than once</dt>
   *     <dd>in this case a field class is created, the builder is written
   *      and the getter returns the field class</dd>
   * </dl>
   *
   * {@see com.google.gwt.uibinder.rebind.FieldWriter#writeFieldGetter}.
   */
  void writeFieldBuilder(IndentedWriter w, int getterCount, OwnerField ownerField)
      throws UnableToCompleteException;

  /**
   * Output the getter and builder definitions for the given field.
   *
   *  <p>
   *  <b>Example for widgets called only once:</b>
   *  <pre>
   *  private WidgetX get_widgetX() {
   *    return build_widgetX();
   *  }
   *  private WidgetX build_widgetX() {
   *   widget = GWT.create(WidgetX.class);
   *   widget.setStyleName("css");
   *   return widgetX;
   *  }
   *  </pre>
   *  Notice that there's no field and the getter just fallback to the builder.
   *  </p>
   *
   *  <p><b>Example for widgets called more than once:</b>
   *  <pre>
   *  private WidgetX widgetX;
   *  private WidgetX get_widgetX() {
   *    return widgetX;
   *  }
   *  private WidgetX build_widgetX() {
   *   widget = GWT.create(WidgetX.class);
   *   widget.setStyleName("css");
   *   return widgetX;
   *  }
   * </pre>
   * Notice that the getter just returns the field. The builder is called in
   * the Widgets ctor.
   * </p>
   */
  void writeFieldDefinition(IndentedWriter w, TypeOracle typeOracle,
      OwnerField ownerField, DesignTimeUtils designTime, int getterCount,
      boolean useLazyWidgetBuilders)
      throws UnableToCompleteException;
}
