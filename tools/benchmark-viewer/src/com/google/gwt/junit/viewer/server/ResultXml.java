/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.junit.viewer.server;

import com.google.gwt.junit.viewer.client.Result;

import org.w3c.dom.Element;

import java.util.List;
import java.util.ArrayList;

/**
 * Hydrates a benchmark Result from an XML Element.
 *
 */
public class ResultXml {
  public static Result fromXml( Element element ) {
    Result result = new Result();
    result.setAgent(element.getAttribute( "agent" ));
    result.setHost(element.getAttribute( "host" ));

    List/*<Element>*/ children = ReportXml.getElementChildren( element, "trial" );

    ArrayList trials = new ArrayList( children.size() );
    result.setTrials(trials);

    for ( int i = 0; i < children.size(); ++i ) {
      trials.add( TrialXml.fromXml( (Element) children.get(i) ));
    }

    // TODO(tobyr) Put some type information in here for the variables

    return result;
  }
}
