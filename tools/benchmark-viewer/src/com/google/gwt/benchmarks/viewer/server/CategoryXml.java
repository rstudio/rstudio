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

import com.google.gwt.benchmarks.viewer.client.Benchmark;
import com.google.gwt.benchmarks.viewer.client.Category;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts an XML Element to a Category object.
 * 
 */
class CategoryXml {
  public static Category fromXml(Element element) {
    Category category = new Category();
    category.setName(element.getAttribute("name"));
    category.setDescription(element.getAttribute("description"));

    List<Element> children = ReportXml.getElementChildren(element, "benchmark");
    category.setBenchmarks(new ArrayList<Benchmark>(children.size()));
    for (int i = 0; i < children.size(); ++i) {
      category.getBenchmarks().add(BenchmarkXml.fromXml(children.get(i)));
    }

    return category;
  }
}
