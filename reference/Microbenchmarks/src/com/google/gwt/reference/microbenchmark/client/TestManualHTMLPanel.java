package com.google.gwt.reference.microbenchmark.client;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Run by {@link MicrobenchmarkSurvey}, see name for details.
 */
public class TestManualHTMLPanel extends Composite {
  public static class Maker extends MicrobenchmarkSurvey.WidgetMaker {
    Maker() {
      super("Text heavy UI via typical manual HTMLPanel usage");
    }

    @Override
    public Widget make() {
      return new TestManualHTMLPanel();
    }
  }
  DivElement div1;
  DivElement div2;
  DivElement div3;
  DivElement div4;
  SpanElement span1;
  
  
  SpanElement span2;
  
  private TestManualHTMLPanel() {
    HTMLPanel p = new HTMLPanel(Util.TEXTY_INNER_HTML);
    initWidget(p);
    
    div1 = p.getElementById("div1").cast();
    div2 = p.getElementById("div2").cast();
    div3 = p.getElementById("div3").cast();
    div4 = p.getElementById("div4").cast();
    span1 = p.getElementById("span1").cast();
    span2 = p.getElementById("span2").cast();
  }
}
