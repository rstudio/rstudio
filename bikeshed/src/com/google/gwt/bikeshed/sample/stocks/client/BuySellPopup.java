/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.bikeshed.sample.stocks.client;

import com.google.gwt.bikeshed.sample.stocks.shared.StockQuote;
import com.google.gwt.bikeshed.sample.stocks.shared.Transaction;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.TextBox;

/**
 * A popup used for purchasing stock.
 */
public class BuySellPopup extends DialogBox {

  // Row numbers for popup fields
  private static final int TICKER = 0;
  private static final int NAME = 1;
  private static final int PRICE = 2;
  private static final int MAX_QUANTITY = 3;
  private static final int QUANTITY = 4;
  private static final int TOTAL = 5;
  private static final int AVAILABLE = 6;
  private static final int BUTTONS = 7;

  private int cash;

  /**
   * True if we are buying, false if hiding.
   */
  private boolean isBuying;

  /**
   * The table used for layout.
   */
  private FlexTable layout = new FlexTable();

  /**
   * The button used to buy or sell.
   */
  private Button opButton;

  /**
   * The box used to change the quantity.
   */
  private TextBox quantityBox = new TextBox();

  private StockQuote quote;

  /**
   * The last transaction.
   */
  private Transaction transaction;

  public BuySellPopup() {
    super(false, true);
    setGlassEnabled(true);
    setWidget(layout);

    layout.setHTML(TICKER, 0, "<b>Ticker:</b>");
    layout.setHTML(NAME, 0, "<b>Name:</b>");
    layout.setHTML(PRICE, 0, "<b>Price:</b>");
    layout.setHTML(MAX_QUANTITY, 0, "<b>Max Quantity:</b>");
    layout.setHTML(QUANTITY, 0, "<b>Quantity:</b>");
    layout.setWidget(QUANTITY, 1, quantityBox);
    layout.setHTML(TOTAL, 0, "<b>Total:</b>");
    layout.setHTML(AVAILABLE, 0, "<b>Available:</b>");

    // Update total price when the quantity changes.
    quantityBox.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        try {
          String text = quantityBox.getText();
          int quantity = text.length() == 0 ? 0 : Integer.parseInt(text);
          double totalPrice = quantity * quote.getPrice() / 100.0;
          layout.setText(TOTAL, 1, NumberFormat.getCurrencyFormat("USD").format(
              totalPrice));
        } catch (NumberFormatException e) {
          layout.setText(TOTAL, 1, "Invalid quantity");
        }
      }
    });

    // Buy Button.
    opButton = new Button("", new ClickHandler() {
      public void onClick(ClickEvent event) {
        try {
          int quantity = Integer.parseInt(quantityBox.getText());
          transaction = new Transaction(isBuying, quote.getTicker(), quantity);
          hide();
        } catch (NumberFormatException e) {
          Window.alert("You must enter a valid quantity");
        }
      }
    });
    layout.setWidget(BUTTONS, 0, opButton);

    // Cancel Button.
    Button cancelButton = new Button("Cancel", new ClickHandler() {
      public void onClick(ClickEvent event) {
        hide();
      }
    });
    layout.setWidget(BUTTONS, 1, cancelButton);
  }

  public StockQuote getStockQuote() {
    return quote;
  }

  /**
   * Get the last transaction.
   * 
   * @return the last transaction, or null if canceled
   */
  public Transaction getTransaction() {
    return transaction;
  }

  /**
   * Set the available cash.
   * 
   * @param cash the available cash
   */
  public void setAvailableCash(int cash) {
    // TODO: Bind the available cash field.
    this.cash = cash;
    layout.setText(AVAILABLE, 1, NumberFormat.getCurrencyFormat("USD").format(cash / 100.0));
  }

  /**
   * Set the current {@link StockQuote}.
   * 
   * @param quote the stock quote to buy
   * @param isBuying true if buying the stock
   */
  public void setStockQuote(StockQuote quote, boolean isBuying) {
    this.quote = quote;
    String op = isBuying ? "Buy" : "Sell";
    setText(op + " " + quote.getTicker() + " (" + quote.getName() + ")");
    layout.setText(TICKER, 1, quote.getTicker());
    layout.setText(NAME, 1, quote.getName());
    layout.setText(PRICE, 1, quote.getDisplayPrice());
    if (isBuying) {
      layout.setText(MAX_QUANTITY, 1, "" + (int) Math.floor(cash / quote.getPrice()));
    } else {
      layout.setText(MAX_QUANTITY, 1, "" + quote.getSharesOwned());
    }
    layout.setText(TOTAL, 1, NumberFormat.getCurrencyFormat("USD").format(0.0));
    quantityBox.setText("0");
    opButton.setText(op);
    this.isBuying = isBuying;
    transaction = null;
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    DeferredCommand.addCommand(new Command() {
      public void execute() {
        quantityBox.selectAll();
        quantityBox.setFocus(true);
      }
    });
  }
}
