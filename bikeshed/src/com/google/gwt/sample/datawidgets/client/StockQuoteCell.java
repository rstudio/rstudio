package com.google.gwt.sample.datawidgets.client;

import com.google.gwt.cells.client.Cell;
import com.google.gwt.sample.datawidgets.shared.StockQuote;

public class StockQuoteCell extends Cell<StockQuote> {

  @Override
  public void render(StockQuote value, StringBuilder sb) {
    sb.append(value.getTicker() + " (" + value.getName() + "): "
        + value.getDisplayPrice());
  }
}
