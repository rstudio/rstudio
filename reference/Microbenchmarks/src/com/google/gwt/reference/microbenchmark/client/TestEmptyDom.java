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
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.user.client.ui.Widget;

/**
 * Run by {@link MicrobenchmarkSurvey}, see name for details.
 */
public class TestEmptyDom extends Widget {
  public static class Maker extends MicrobenchmarkSurvey.WidgetMaker {
    Maker() {
      super("Empty UI via innerHTML, no widgets, get children by id");
    }

    @Override
    public Widget make() {
      return new TestEmptyDom();
    }
  }

  Element root;
  DivElement div1;
  DivElement div2;
  DivElement div3;

  DivElement div4;
  SpanElement span1;
  
  SpanElement span2;

  private TestEmptyDom() {
    root = Util.fromHtml(Util.EMPTY_OUTER_HTML);
    
    Document.get().getBody().appendChild(root);
    div1 = Document.get().getElementById("div1").cast();
    div2 = Document.get().getElementById("div2").cast();
    div3 = Document.get().getElementById("div3").cast();
    div4 = Document.get().getElementById("div4").cast();
    span1 = Document.get().getElementById("span1").cast();
    span2 = Document.get().getElementById("span2").cast();
    
    Document.get().getBody().removeChild(root);
    div1.removeAttribute("id");
    div2.removeAttribute("id");
    div3.removeAttribute("id");
    div4.removeAttribute("id");
    span1.removeAttribute("id");
    span2.removeAttribute("id");

    setElement(root);
  }
}
