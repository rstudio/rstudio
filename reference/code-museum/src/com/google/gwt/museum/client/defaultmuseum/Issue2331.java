package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.StackPanel;
import com.google.gwt.user.client.ui.Widget;

public class Issue2331 extends AbstractIssue {

  @Override
  public Widget createIssue() {
    StackPanel p = new StackPanel();
    p.add(new Label("A"),"A");
    p.add(new Label("B"),"B");
    return p;
  }

  @Override
  public String getInstructions() {
    return "Click on B";
  }

  @Override
  public String getSummary() {
    return "Stack Panel does not response to switching stacks";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

}
