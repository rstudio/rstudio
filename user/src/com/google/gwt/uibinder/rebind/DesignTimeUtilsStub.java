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
 * Empty implementation of {@link DesignTimeUtils}.
 */
public class DesignTimeUtilsStub implements DesignTimeUtils {
  public static final DesignTimeUtils EMPTY = new DesignTimeUtilsStub();

  public void addDeclarations(IndentedWriter w) {
  }

  public String getImplName(String implName) {
    return implName;
  }

  public String getPath(Element element) {
    return null;
  }

  public String getProvidedFactory(String typeName, String methodName,
      String args) {
    return null;
  }

  public String getProvidedField(String typeName, String fieldName) {
    return null;
  }

  public String getTemplateContent(String path) {
    return null;
  }

  public void handleUIObject(Statements writer, XMLElement elem,
      String fieldName) {
  }

  public boolean isDesignTime() {
    return false;
  }

  public void putAttribute(XMLElement elem, String name, String value) {
  }

  public void putAttribute(XMLElement elem, String name, String[] values) {
  }

  public void rememberPathForElements(Document doc) {
  }

  public void writeAttributes(Statements writer) {
  }
}