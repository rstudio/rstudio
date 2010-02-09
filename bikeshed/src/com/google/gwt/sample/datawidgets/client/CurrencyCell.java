package com.google.gwt.sample.datawidgets.client;

import com.google.gwt.cells.client.Cell;

public class CurrencyCell extends Cell<Integer> {

  @Override
  public void render(Integer price, StringBuilder sb) {
    int dollars = price / 100;
    int cents = price % 100;
    
    sb.append("$ ");
    sb.append(dollars);
    sb.append('.');
    if (cents < 10) {
      sb.append('0');
    }
    sb.append(cents);
  }
}
