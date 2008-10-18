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

import com.google.gwt.benchmarks.viewer.client.Result;
import com.google.gwt.benchmarks.viewer.client.Trial;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Hydrates a benchmark Result from an XML Element.
 */
public class ResultXml {
  public static Result fromXml(Element element) {
    Result result = new Result();
    result.setAgent(element.getAttribute("agent"));
    result.setHost(element.getAttribute("host"));
    Element exception = ReportXml.getElementChild(element, "exception");
    if (exception != null) {
      result.setException(ReportXml.getText(exception));
    }

    List<Element> children = ReportXml.getElementChildren(element, "trial");

    ArrayList<Trial> trials = new ArrayList<Trial>(children.size());
    result.setTrials(trials);

    for (int i = 0; i < children.size(); ++i) {
      trials.add(TrialXml.fromXml(children.get(i)));
    }

    // TODO(tobyr) Put some type information in here for the variables

    return result;
  }
}
