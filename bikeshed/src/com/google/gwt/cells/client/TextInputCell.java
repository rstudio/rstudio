package com.google.gwt.cells.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;

public class TextInputCell extends Cell<String> {

  @Override
  public void render(String data, StringBuilder sb) {
    sb.append("<input type='text'");
    if (data != null) {
      sb.append(" value='" + data + "'");
    }
    sb.append("></input>");
  }

  @Override
  public void onBrowserEvent(Element parent, String value, NativeEvent event,
      Mutator<String, String> mutator) {
    if (mutator == null) {
      return;
    }

    if ("change".equals(event.getType())) {
      InputElement input = parent.getFirstChild().cast();
      mutator.mutate(value, input.getValue());
    }
  }
}
