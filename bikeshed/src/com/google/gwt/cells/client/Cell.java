package com.google.gwt.cells.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;

public abstract class Cell<C> {

  public void onBrowserEvent(Element parent, C value, NativeEvent event,
      Mutator<C, C> mutator) {
  }

  // TODO: render needs a way of assuming text by default, but allowing HTML.
  public abstract void render(C value, StringBuilder sb);

  public void setValue(Element parent, C value) {
    StringBuilder sb = new StringBuilder();
    render(value, sb);
    parent.setInnerHTML(sb.toString());
  }
}
