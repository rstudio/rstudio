/*
 * Copyright 2010 Google Inc.
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Utilities used for implementing design time support of UiBinder.
 */
public interface DesignTimeUtils {

  /**
   * Adds declarations for design time artifacts.
   */
  void addDeclarations(IndentedWriter w);

  /**
   * Returns the name of "Impl", unique each time if it is design time.
   */
  String getImplName(String implName);

  /**
   * Returns the path of given {@link Element}.
   */
  String getPath(Element element);

  /**
   * Returns the source to access "@UiFactory" method.
   */
  String getProvidedFactory(String typeName, String methodName, String args);

  /**
   * Returns the source to access "@UiField(provided)" instance of some object.
   */
  String getProvidedField(String typeName, String fieldName);

  /**
   * Returns the design time content of <code>*.ui.xml</code> template to parse,
   * or <code>null</code> if not design time, or this template is not under
   * design.
   */
  String getTemplateContent(String path);

  /**
   * Notifies tool about <code>UIObject</code> creation.
   */
  void handleUIObject(Statements writer, XMLElement elem, String fieldName);

  /**
   * Returns <code>true</code> if this template is under design now, so some of
   * UiBinder features should be disables. This includes assigning values into
   * "@UiField", processing "@UiHandler".
   */
  boolean isDesignTime();

  /**
   * Remembers value of attribute, for given {@link XMLElement}.
   */
  void putAttribute(XMLElement elem, String name, String value);

  /**
   * Remembers value of attribute, for given {@link XMLElement}.
   */
  void putAttribute(XMLElement elem, String name, String[] values);

  /**
   * Fills {@value #elementPaths} with paths for given and child {@link Element}
   * s.
   */
  void rememberPathForElements(Document doc);

  /**
   * Writes remembered values of attributes.
   */
  void writeAttributes(Statements writer);
}
