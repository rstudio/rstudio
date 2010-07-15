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
import java.util.WeakHashMap;

/**
 * Utilities used for implementing design time support of UiBinder.
 */
public class DesignTimeUtils {
  private static final WeakHashMap<Element, String> elementPaths = new WeakHashMap<Element, String>();

  /**
   * Adds declarations for design time artifacts.
   */
  public static void addDeclarations(IndentedWriter w) {
    if (!isDesignTime()) {
      return;
    }
    w.write("public static interface DTObjectHandler {void handle(String path, Object object);}");
    w.write("public DTObjectHandler dtObjectHandler;");
    w.write("public final java.util.Map dtAttributes = new java.util.HashMap();");
  }

  /**
   * @return the name of "Impl", unique each time if it is design time.
   */
  public static String getImplName(String implName) {
    if (isDesignTime()) {
      implName += "_designTime" + System.currentTimeMillis();
    }
    return implName;
  }

  /**
   * @return the path of given {@link Element}.
   */
  public static String getPath(Element element) {
    return elementPaths.get(element);
  }

  /**
   * @return the design time content of <code>*.ui.xml</code> template to parse,
   *         or <code>null</code> if not design time, or this template is not
   *         under design.
   */
  public static String getTemplateContent(String path) {
    if (DesignTimeUtils.isDesignTime()) {
      return System.getProperty("gwt.UiBinder.designTime " + path);
    }
    return null;
  }

  /**
   * Notifies tool about <code>UIObject</code> creation.
   */
  public static void handleUIObject(Statements writer,
      XMLElement elem, String fieldName) {
    if (!isDesignTime()) {
      return;
    }
    writer.addStatement(
        "if (dtObjectHandler != null) dtObjectHandler.handle(\"%s\", %s);",
        elem.getDesignTimePath(), fieldName);
  }

  /**
   * @return <code>true</code> if UiBinder works now in design time, so
   *         additional information should be provided in generated classes.
   */
  public static boolean isDesignTime() {
    return Beans.isDesignTime();
  }

  /**
   * Remembers value of attribute, for given {@link XMLElement}.
   */
  public static void putAttribute(Statements writer,
      XMLElement elem, String name, String value) {
    if (!isDesignTime()) {
      return;
    }
    String path = elem.getDesignTimePath();
    String key = path + " " + name;
    writer.addStatement("dtAttributes.put(\"%s\", %s);", key, value);
  }

  /**
   * Fills {@value #elementPaths} with paths for given and child {@link Element}
   * s.
   */
  public static void rememberPathForElements(Document doc) {
    if (!isDesignTime()) {
      return;
    }
    rememberPathForElements(doc.getDocumentElement(), "0");
  }

  /**
   * Recursive implementation of {@link #rememberPathForElements(Document)}.
   */
  private static void rememberPathForElements(Element element, String path) {
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