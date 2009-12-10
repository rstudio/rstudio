/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.benchmarks.viewer.server;

import com.google.gwt.benchmarks.viewer.client.Category;
import com.google.gwt.benchmarks.viewer.client.Report;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hydrates a Report from its XML representation.
 */
class ReportXml {

  /**
   * Hydrates a Report from its XML representation.
   * 
   * @param element The XML element to hydrate from.
   * @return a new report (with null id)
   */
  public static Report fromXml(Element element) {

    Report report = new Report();
    String dateString = element.getAttribute("date");

    try {
      DateFormat format = DateFormat.getDateTimeInstance();
      Date d = format.parse(dateString);
      report.setDate(d);
      report.setDateString(format.format(d));
    } catch (ParseException e) {
      // let date remain null if it doesn't parse correctly
    }

    report.setGwtVersion(element.getAttribute("gwt_version"));

    List<Element> children = getElementChildren(element, "category");
    Map<String, Category> categories = new HashMap<String, Category>();
    for (Element child : children) {
      Category newCategory = CategoryXml.fromXml(child);
      Category oldCategory = categories.get(newCategory.getName());
      if (oldCategory != null) {
        // if a category with the same name exists, combine the benchmarks
        oldCategory.getBenchmarks().addAll(newCategory.getBenchmarks());
      } else {
        categories.put(newCategory.getName(), newCategory);
      }
    }
    report.setCategories(new ArrayList<Category>(categories.values()));

    return report;
  }

  static Element getElementChild(Element e, String name) {
    NodeList children = e.getElementsByTagName(name);
    return children.getLength() == 0 ? null : (Element) children.item(0);
  }

  static List<Element> getElementChildren(Element e, String name) {
    NodeList children = e.getElementsByTagName(name);
    int numElements = children.getLength();
    List<Element> elements = new ArrayList<Element>(numElements);
    for (int i = 0; i < children.getLength(); ++i) {
      Node n = children.item(i);
      elements.add((Element) n);
    }
    return elements;
  }

  static String getText(Element e) {
    Node n = e.getFirstChild();
    return n == null ? null : n.getNodeValue();
  }
}
