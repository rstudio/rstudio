package com.google.gwt.reference.microbenchmark.client;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Run by {@link MicrobenchmarkSurvey}, see name for details.
 */
public class TestFlows extends Composite {
  public static class Maker extends MicrobenchmarkSurvey.WidgetMaker {
    Maker() {
      super("Text heavy UI via FlowPanels (DIVs) and InlineLabels (SPANs)");
    }

    @Override
    public Widget make() {
      return new TestFlows();
    }
  }
  
  private TestFlows() {
    FlowPanel root = new FlowPanel();
    Util.addText(root.getElement(), "Div root");
    
    FlowPanel div1 = new FlowPanel();
    Util.addText(div1.getElement(), "Div1");
    root.add(div1);

    FlowPanel div2 = new FlowPanel();
    Util.addText(div2.getElement(), "Div2");
    div1.add(div2);

    InlineLabel span1 = new InlineLabel();
    span1.setText("Span1");
    div1.add(span1);

    FlowPanel anon = new FlowPanel();
    Util.addText(anon.getElement(), "Div anon");
    root.add(anon);

    FlowPanel div3 = new FlowPanel();
    Util.addText(div3.getElement(), "Div3");
    anon.add(div3);

    FlowPanel div4 = new FlowPanel();
    Util.addText(div4.getElement(), "Div4");
    div3.add(div4);

    InlineLabel span2 = new InlineLabel();
    span2.setText("Span2");
    div3.add(span2);
    
    Util.addText(div1.getElement(), " Div1 end");
    Util.addText(div3.getElement(), " Div3 end");
    Util.addText(anon.getElement(), " Div anon end");
    Util.addText(root.getElement(), " Div root end");

    initWidget(root);
  }
}
