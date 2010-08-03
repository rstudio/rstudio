/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.sample.dynatablerf.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.sample.dynatablerf.client.events.NavigationEvent;
import com.google.gwt.sample.dynatablerf.client.events.DataAvailableEvent;
import com.google.gwt.sample.dynatablerf.shared.PersonProxy;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.List;

/**
 * A composite Widget that implements the main interface for the dynamic table,
 * including the data table, status indicators, and paging buttons.
 */
public class DynaTableWidget extends Composite {

  interface Binder extends UiBinder<Widget, DynaTableWidget> {
  }

  /**
   * A dialog box for displaying an error.
   */
  private static class ErrorDialog extends DialogBox implements ClickHandler {
    private HTML body = new HTML("");

    public ErrorDialog() {
      setStylePrimaryName("DynaTable-ErrorDialog");
      Button closeButton = new Button("Close", this);
      VerticalPanel panel = new VerticalPanel();
      panel.setSpacing(4);
      panel.add(body);
      panel.add(closeButton);
      panel.setCellHorizontalAlignment(closeButton, VerticalPanel.ALIGN_RIGHT);
      setWidget(panel);
    }

    public void onClick(ClickEvent event) {
      hide();
    }

    public void setBody(String html) {
      body.setHTML(html);
    }
  }

  // TODO: Re-add error handling
  @SuppressWarnings("unused")
  private static final String NO_CONNECTION_MESSAGE = "<p>The DynaTableRf example uses a "
      + "RequestFactory "
      + "to request data from the server.  In order for the RequestFactory to "
      + "successfully return data, the server component must be available.</p>"
      + "<p>If you are running this demo from compiled code, the server "
      + "component may not be available to respond to requests from "
      + "DynaTableRf.  Try running DynaTable in development mode to see the demo "
      + "in action.</p> ";

  @UiField
  Grid grid;

  @UiField
  NavBar navbar;

  private ErrorDialog errorDialog = null;

  private int startRow = 0;

  private final CalendarProvider provider;

  private HandlerRegistration rowDataRegistration;

  public DynaTableWidget(CalendarProvider provider, int rowCount) {
    this.provider = provider;
    Binder binder = GWT.create(Binder.class);
    initWidget(binder.createAndBindUi(this));
    initTable(rowCount);
  }

  public void clearStatusText() {
    navbar.status.setInnerHTML("&nbsp;");
  }

  public void refresh() {
    // Disable buttons temporarily to stop the user from running off the end.
    //
    navbar.gotoFirst.setEnabled(false);
    navbar.gotoPrev.setEnabled(false);
    navbar.gotoNext.setEnabled(false);

    setStatusText("Please wait...");
    provider.updateRowData(startRow, grid.getRowCount() - 1);
  }

  public void setRowCount(int rows) {
    grid.resizeRows(rows);
  }

  public void setStatusText(String text) {
    navbar.status.setInnerText(text);
  }

  /**
   * Attach to the event bus only when the widget is attached to the DOM.
   */
  @Override
  protected void onLoad() {
    rowDataRegistration = provider.addRowDataHandler(new DataAvailableEvent.Handler() {
      public void onRowData(DataAvailableEvent event) {
        accept(event.getStartRow(), event.getPeople());
      }
    });
  }

  @Override
  protected void onUnload() {
    rowDataRegistration.removeHandler();
  }

  @UiHandler("navbar")
  void onNavigation(NavigationEvent e) {
    switch (e.getDirection()) {
      case BACKWARD:
        startRow -= getDataRowCount();
        break;
      case FORWARD:
        startRow += getDataRowCount();
        break;
      case START:
        startRow = 0;
        break;
    }
    refresh();
  }

  private void accept(int startRow, List<PersonProxy> people) {

    int destRowCount = getDataRowCount();
    int destColCount = grid.getCellCount(0);
    assert (people.size() <= destRowCount) : "Too many rows";

    int srcRowIndex = 0;
    int srcRowCount = people.size();
    int destRowIndex = 1; // skip navbar row
    for (; srcRowIndex < srcRowCount; ++srcRowIndex, ++destRowIndex) {
      PersonProxy p = people.get(srcRowIndex);
      grid.setText(destRowIndex, 0, p.getName());
      grid.setText(destRowIndex, 1, p.getDescription());
      grid.setText(destRowIndex, 2, p.getSchedule());
    }

    // Clear remaining table rows.
    //
    boolean isLastPage = false;
    for (; destRowIndex < destRowCount + 1; ++destRowIndex) {
      isLastPage = true;
      for (int destColIndex = 0; destColIndex < destColCount; ++destColIndex) {
        grid.clearCell(destRowIndex, destColIndex);
      }
    }

    // Synchronize the nav buttons.
    navbar.gotoNext.setEnabled(!isLastPage);
    navbar.gotoFirst.setEnabled(startRow > 0);
    navbar.gotoPrev.setEnabled(startRow > 0);

    // Update the status message.
    setStatusText((startRow + 1) + " - " + (startRow + srcRowCount));
  }

  // TODO: Re-add error handling
  @SuppressWarnings("unused")
  private void failed(Throwable caught) {
    setStatusText("Error");
    if (errorDialog == null) {
      errorDialog = new ErrorDialog();
    }
    errorDialog.setText("Unexcepted Error processing remote call");
    errorDialog.setBody(caught.getMessage());

    errorDialog.center();
  }

  private int getDataRowCount() {
    return grid.getRowCount() - 1;
  }

  private void initTable(int rowCount) {
    // Set up the header row. It's one greater than the number of visible rows.
    //
    grid.resizeRows(rowCount + 1);
  }
}
