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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.beans.Beans;
import java.util.HashMap;
import java.util.Map;

/**
 * Design time implementation of {@link DesignTimeUtils}.
 */
public class DesignTimeUtilsImpl implements DesignTimeUtils {
  /**
   * Returns <code>true</code> if given "Binder" is under design now. We should
   * not use "design time" globally, because while one widget in under design,
   * it may use other widgets and we want to execute them as is, without design
   * time tweaks.
   */
  public static boolean isDesignTime(String fqInterfaceName) {
    if (Beans.isDesignTime()) {
      String key = "gwt.UiBinder.isDesignTime " + fqInterfaceName;
      return System.getProperty(key) != null;
    }
    return false;
  }

  private final Map<Element, String> elementPaths = new HashMap<Element, String>();
  private final Map<String, String> attributes = new HashMap<String, String>();

  public void addDeclarations(IndentedWriter w) {
    // handler
    w.write("public static interface DTObjectHandler {");
    {
      w.indent();
      w.write("void handle(String path, Object object);");
      w.write("Object provideFactory(Class rawType, String methodName, Object[] args);");
      w.write("Object provideField(Class rawType, String fieldName);");
      w.outdent();
    }
    w.write("}");
    w.write("public DTObjectHandler dtObjectHandler;");
    // attributes
    w.write("public final java.util.Map dtAttributes = new java.util.HashMap();");
    w.write("private void dtPutAttribute(String key, Object...values) {");
    {
      w.indent();
      w.write("if (values.length == 1) {");
      {
        w.indent();
        w.write("dtAttributes.put(key, values[0]);");
        w.outdent();
      }
      w.write("} else {");
      {
        w.indent();
        w.write("dtAttributes.put(key, java.util.Arrays.asList(values));");
        w.outdent();
      }
      w.write("}");
      w.outdent();
    }
    w.write("}");
  }

  public String getImplName(String implName) {
    return implName + "_designTime" + System.currentTimeMillis();
  }

  public String getPath(Element element) {
    return elementPaths.get(element);
  }

  public String getProvidedFactory(String typeName, String methodName,
      String args) {
    return String.format(
        "(%1$s) dtObjectHandler.provideFactory(%1$s.class, \"%2$s\", new Object[] {%3$s})",
        typeName, methodName, args);
  }

  public String getProvidedField(String typeName, String fieldName) {
    return String.format(
        "(%1$s) dtObjectHandler.provideField(%1s.class, \"%2$s\")", typeName,
        fieldName);
  }

  public String getTemplateContent(String path) {
    return System.getProperty("gwt.UiBinder.designTime " + path);
  }

  public void handleUIObject(Statements writer, XMLElement elem,
      String fieldName) {
    writer.addStatement(
        "if (dtObjectHandler != null) dtObjectHandler.handle(\"%s\", %s);",
        elem.getDesignTimePath(), fieldName);
  }

  public boolean isDesignTime() {
    return true;
  }

  public void putAttribute(XMLElement elem, String name, String value) {
    String path = elem.getDesignTimePath();
    String key = path + " " + name;
    attributes.put(key, value);
  }

  public void putAttribute(XMLElement elem, String name, String[] values) {
    if (values.length == 0) {
      return;
    }
    StringBuilder sb = new StringBuilder();
    for (String value : values) {
      if (sb.length() != 0) {
        sb.append(", ");
      }
      sb.append(value);
    }
    sb.insert(0, "new String[] {");
    sb.append("}");
    putAttribute(elem, name, sb.toString());
  }

  public void rememberPathForElements(Document doc) {
    rememberPathForElements(doc.getDocumentElement(), "0");
  }

  public void writeAttributes(Statements writer) {
    for (Map.Entry<String, String> entry : attributes.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      writer.addStatement("dtPutAttribute(\"%s\", %s);", key, value);
    }
    attributes.clear();
  }

  /**
   * Returns remembered attributes, used during tests only.
   */
  Map<String, String> getAttributes() {
    return attributes;
  }

  /**
   * Recursive implementation of {@link #rememberPathForElements(Document)}.
   */
  private void rememberPathForElements(Element element, String path) {
    elementPaths.put(element, path);
    NodeList childNodes = element.getChildNodes();
    int elementIndex = 0;
    for (int i = 0; i < childNodes.getLength(); ++i) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeType() == Node.ELEMENT_NODE) {
        Element childElement = (Element) childNode;
        rememberPathForElements(childElement, path + "/" + elementIndex++);
      }
    }
  }
}
