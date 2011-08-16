package com.google.gwt.reference.microbenchmark.client;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.user.client.ui.Widget;

/**
 * Run by {@link MicrobenchmarkSurvey}, see name for details.
 */
public class TestEmptyRealisticDomCrawl extends Widget {
  public static class Maker extends MicrobenchmarkSurvey.WidgetMaker {
    Maker() {
      super("Empty UI via innerHTML, no widgets, get children by nav from root");
    }

    @Override
    public Widget make() {
      return new TestEmptyRealisticDomCrawl();
    }
  }

  Element elm;
  DivElement div1;
  DivElement div2;
  DivElement div3;
  DivElement div4;
  SpanElement span1;

  SpanElement span2;
  
  private TestEmptyRealisticDomCrawl() {
    Element root = Util.fromHtml(Util.EMPTY_OUTER_HTML);

    div1 = root.getFirstChildElement().cast();
    assert div1.getId().equals("div1");
    div2 = root.getFirstChildElement().getFirstChildElement().cast();
    assert div2.getId().equals("div2");
    span1 = root.getFirstChildElement().getFirstChildElement().getNextSiblingElement().cast();
    assert span1.getId().equals("span1");
    
    div3 = root.getFirstChildElement().getNextSiblingElement().getFirstChildElement().cast();
    assert div3.getId().equals("div3");
    div4 = root.getFirstChildElement().getNextSiblingElement().getFirstChildElement().getFirstChildElement().cast();
    assert div4.getId().equals("div4");
    span2 = root.getFirstChildElement().getNextSiblingElement().getFirstChildElement().getFirstChildElement().getNextSiblingElement().cast();
    assert span2.getId().equals("span2");
    
    setElement(root);
  }
}
