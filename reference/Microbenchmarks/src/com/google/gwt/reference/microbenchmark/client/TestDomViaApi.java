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
public class TestDomViaApi extends Widget {
  public static class Maker extends MicrobenchmarkSurvey.WidgetMaker {
    Maker() {
      super("Text heavy UI via DOM api calls, no widgets");
    }

    @Override
    public Widget make() {
      return new TestDomViaApi();
    }
  }

  DivElement root;
  DivElement div1;
  DivElement div2;
  DivElement div3;
  DivElement div4;
  SpanElement span1;
  SpanElement span2;

  private TestDomViaApi() {
    Document d = Document.get();
    root = d.createDivElement();
    root.appendChild(d.createTextNode("Div root"));

    div1 = d.createDivElement();
    Util.addText(div1, "Div1");
    root.appendChild(div1);

    div2 = d.createDivElement();
    Util.addText(div2, "Div2");
    div1.appendChild(div2);

    span1 = d.createSpanElement();
    Util.addText(span1, "Span1");
    div1.appendChild(span1);

    DivElement anon = d.createDivElement();
    Util.addText(anon, "Div anon");
    root.appendChild(anon);

    div3 = d.createDivElement();
    Util.addText(div3, "Div3");
    anon.appendChild(div3);

    div4 = d.createDivElement();
    Util.addText(div4, "Div4");
    div3.appendChild(div4);

    span2 = d.createSpanElement();
    Util.addText(span2, "Span2");
    div3.appendChild(span2);

    Util.addText(div1, " Div1 end");
    Util.addText(div3, " Div3 end");
    Util.addText(anon, " Div anon end");
    Util.addText(root, " Div root end");

    setElement(root);
  }
}
