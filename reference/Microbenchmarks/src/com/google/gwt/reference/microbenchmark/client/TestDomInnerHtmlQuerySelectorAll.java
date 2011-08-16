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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.user.client.ui.Widget;

/**
 * Run by {@link MicrobenchmarkSurvey}, see name for details.
 */
public class TestDomInnerHtmlQuerySelectorAll extends Widget {
  public static class Maker extends MicrobenchmarkSurvey.WidgetMaker {
    Maker() {
      super("Text heavy UI via innerHTML, no widgets, querySelectorAll");
    }

    @Override
    public Widget make() {
      return new TestDomInnerHtmlQuerySelectorAll();
    }
  }

  Element root;
  DivElement div1;
  DivElement div2;
  DivElement div3;

  DivElement div4;
  SpanElement span1;
  
  SpanElement span2;

  private TestDomInnerHtmlQuerySelectorAll() {
    root = Util.fromHtml(Util.TEXTY_OUTER_HTML);
    
    String query = "#div1, #div2, #div3, #div4, #span1, #span2";
    JsArray<Element> response = Util.querySelectorAll(root, query);
    assert 6 == response.length() : "response length should be 6: " + response.length();
    
    div1 = response.get(0).cast();
    div2 = response.get(1).cast();
    span1 = response.get(2).cast();
    div3 =  response.get(3).cast();
    div4 =  response.get(4).cast();
    span2 = response.get(5).cast();
    
    assert div1.getId().equals("div1");
    assert div2.getId().equals("div2");
    assert span1.getId().equals("span1");
    assert div3.getId().equals("div3");
    assert div4.getId().equals("div4");
    assert span2.getId().equals("span2");

    div1.removeAttribute("id");
    div2.removeAttribute("id");
    span1.removeAttribute("id");
    div3.removeAttribute("id");
    div4.removeAttribute("id");
    span2.removeAttribute("id");

    setElement(root);
  }
}
