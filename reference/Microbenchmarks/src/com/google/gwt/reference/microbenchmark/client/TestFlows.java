package com.google.gwt.reference.microbenchmark.client;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * Run by {@link WidgetCreation}, see {@link Maker#name} for details.
 */
public class TestFlows extends Composite {
  public static class Maker extends WidgetCreation.Maker {
    Maker() {
      super("Complex UI via FlowPanels (DIVs) and Labels (SPANs)");
    }
    public Widget make() {
      return new TestFlows();
    }
  }
  
  private TestFlows() {
    FlowPanel root = new FlowPanel();
    FlowPanel div1 = new FlowPanel();
    FlowPanel div2 = new FlowPanel();
    FlowPanel child2 = new FlowPanel();
    FlowPanel div3 = new FlowPanel();
    FlowPanel div4 = new FlowPanel();
    Label span1 = new Label();
    Label span2 = new Label();
    
    div1.add(div2);
    div1.add(span1);
    root.add(div1);
    
    child2.add(div3);
    div3.add(div4);
    div3.add(span2);
    root.add(child2);
    
    initWidget(root);
  }
}
