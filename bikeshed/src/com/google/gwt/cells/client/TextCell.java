package com.google.gwt.cells.client;

public class TextCell extends Cell<String> {

  @Override
  public void render(String value, StringBuilder sb) {
    if (value != null) {
      sb.append(value);
    }
  }
}
