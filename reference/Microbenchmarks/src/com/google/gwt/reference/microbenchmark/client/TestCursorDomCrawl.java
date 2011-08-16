package com.google.gwt.reference.microbenchmark.client;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.user.client.ui.Widget;

/**
 * Run by {@link MicrobenchmarkSurvey}, see name for details.
 */
public class TestCursorDomCrawl extends Widget {
  public static class Maker extends MicrobenchmarkSurvey.WidgetMaker {
    Maker() {
      super("Text heavy UI via innerHTML, no widgets, get children by idealized crawl");
    }

    @Override
    public Widget make() {
      return new TestCursorDomCrawl();
    }
  }

  Element elm;
  DivElement div1;
  DivElement div2;
  DivElement div3;
  DivElement div4;
  SpanElement span1;
  SpanElement span2;
  
  private TestCursorDomCrawl() {
    Element root = Util.fromHtml(Util.TEXTY_OUTER_HTML);
    
    Element cursor = root;
    div1 = (cursor = cursor.getFirstChildElement()).cast();
    assert div1.getId().equals("div1");
    div2 = (cursor = cursor.getFirstChildElement()).cast();
    assert div2.getId().equals("div2");
    span1 = (cursor = cursor.getNextSiblingElement()).cast();
    assert span1.getId().equals("span1");
    
    cursor = div1.getNextSiblingElement();
    div3 = (cursor = cursor.getFirstChildElement()).cast();
    assert div3.getId().equals("div3");
    div4 = (cursor = cursor.getFirstChildElement()).cast();
    assert div4.getId().equals("div4");
    span2 = (cursor = cursor.getNextSiblingElement()).cast();
    assert span2.getId().equals("span2");
    
    setElement(root);
  }
}
