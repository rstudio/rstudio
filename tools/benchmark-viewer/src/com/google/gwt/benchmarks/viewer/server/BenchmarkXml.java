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
import com.google.gwt.benchmarks.viewer.client.Result;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts an XML element to a Benchmark object.
 * 
 */
class BenchmarkXml {

  public static Benchmark fromXml(Element element) {
    Benchmark benchmark = new Benchmark();
    benchmark.setClassName(element.getAttribute("class"));
    benchmark.setName(element.getAttribute("name"));
    benchmark.setDescription(element.getAttribute("description"));

    List<Element> children = ReportXml.getElementChildren(element, "result");
    benchmark.setResults(new ArrayList<Result>(children.size()));
    for (int i = 0; i < children.size(); ++i) {
      benchmark.getResults().add(ResultXml.fromXml(children.get(i)));
    }

    Element code = ReportXml.getElementChild(element, "source_code");
    if (code != null) {
      benchmark.setSourceCode(ReportXml.getText(code));
    }

    return benchmark;
  }
}
