package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

public class DOMTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testGetParent() {
    Element element = RootPanel.get().getElement();
    int i = 0;
    while (i < 10 && element != null) {
      element = DOM.getParent(element);
      i++;
    }
    // If we got here we looped "forever" or passed, as no exception was thrown.
    if(i==10) {
      fail("Cyclic parent structure detected.");
    }
    // If we get here, we pass, because we encountered no errors going to the 
    // top of the parent hierarchy.
  }
  
  public void testToString() {
    Button b = new Button("abcdef");
    assertTrue(b.toString().indexOf("abcdef")!=-1);
    assertTrue(b.toString().toLowerCase().indexOf("button")!=-1);
  }
}
