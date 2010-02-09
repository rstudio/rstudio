package com.google.gwt.cells.client;

public class ButtonCell extends Cell<String> {

  @Override
  public void render(String data, StringBuilder sb) {
    sb.append("<button>");
    if (data != null) {
      sb.append(data);
    }
    sb.append("</button>");
  }
}
