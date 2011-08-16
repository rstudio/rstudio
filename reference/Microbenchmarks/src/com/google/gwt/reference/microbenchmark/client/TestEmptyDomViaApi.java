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
package com.google.gwt.reference.microbenchmark.client;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.user.client.ui.Widget;

/**
 * Run by {@link MicrobenchmarkSurvey}, see name for details.
 */
public class TestEmptyDomViaApi extends Widget {
  public static class Maker extends MicrobenchmarkSurvey.WidgetMaker {
    Maker() {
      super("Empty UI via DOM api calls, no widgets");
    }

    @Override
    public Widget make() {
      return new TestEmptyDomViaApi();
    }
  }

  DivElement root;
  DivElement div1;
  DivElement div2;
  DivElement div3;
  DivElement div4;
  SpanElement span1;
  SpanElement span2;

  private TestEmptyDomViaApi() {
    Document d = Document.get();
    root = d.createDivElement();
    root.appendChild(d.createTextNode("Div root"));

    div1 = d.createDivElement();
    root.appendChild(div1);

    div2 = d.createDivElement();
    div1.appendChild(div2);

    span1 = d.createSpanElement();
    div1.appendChild(span1);

    DivElement anon = d.createDivElement();
    root.appendChild(anon);

    div3 = d.createDivElement();
    anon.appendChild(div3);

    div4 = d.createDivElement();
    div3.appendChild(div4);

    span2 = d.createSpanElement();
    div3.appendChild(span2);

    setElement(root);
  }
}
