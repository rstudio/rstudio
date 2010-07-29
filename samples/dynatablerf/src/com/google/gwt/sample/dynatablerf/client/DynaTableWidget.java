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
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.sample.dynatablerf.client.DynaTableDataProvider.RowDataAcceptor;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A composite Widget that implements the main interface for the dynamic table,
 * including the data table, status indicators, and paging buttons.
 */
public class DynaTableWidget extends Composite {

  class NavBar extends Composite {

    @UiField
    Button gotoFirst;

    @UiField
    Button gotoNext;

    @UiField
    Button gotoPrev;

    @UiField
    DivElement status;

    public NavBar() {
      NavBarBinder b = GWT.create(NavBarBinder.class);
      initWidget(b.createAndBindUi(this));
    }

    @UiHandler("gotoFirst")
    void onFirst(ClickEvent event) {
      startRow = 0;
      refresh();
    }

    @UiHandler("gotoNext")
    void onNext(ClickEvent event) {
      startRow += getDataRowCount();
      refresh();
    }

    @UiHandler("gotoPrev")
    void onPrev(ClickEvent event) {
      startRow -= getDataRowCount();
      if (startRow < 0) {
        startRow = 0;
      }
      refresh();
    }
  }

  interface Binder extends UiBinder<Widget, DynaTableWidget> {
  }

  @UiTemplate("NavBar.ui.xml")
  interface NavBarBinder extends UiBinder<Widget, NavBar> {
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

  private class RowDataAcceptorImpl implements RowDataAcceptor {
    public void accept(int startRow, String[][] data) {

      int destRowCount = getDataRowCount();
      int destColCount = grid.getCellCount(0);
      assert (data.length <= destRowCount) : "Too many rows";

      int srcRowIndex = 0;
      int srcRowCount = data.length;
      int destRowIndex = 1; // skip navbar row
      for (; srcRowIndex < srcRowCount; ++srcRowIndex, ++destRowIndex) {
        String[] srcRowData = data[srcRowIndex];
        assert (srcRowData.length == destColCount) : " Column count mismatch";
        for (int srcColIndex = 0; srcColIndex < destColCount; ++srcColIndex) {
          String cellHTML = srcRowData[srcColIndex];
          grid.setText(destRowIndex, srcColIndex, cellHTML);
        }
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
      //
      setStatusText((startRow + 1) + " - " + (startRow + srcRowCount));
    }

    public void failed(Throwable caught) {
      setStatusText("Error");
      if (errorDialog == null) {
        errorDialog = new ErrorDialog();
      }
      errorDialog.setText("Unexcepted Error processing remote call");
      errorDialog.setBody(caught.getMessage());

      errorDialog.center();
    }
  }

  private static final String NO_CONNECTION_MESSAGE = "<p>The DynaTableRf example uses a "
      + "RequestFactory "
      + "to request data from the server.  In order for the RequestFactory to "
      + "successfully return data, the server component must be available.</p>"
      + "<p>If you are running this demo from compiled code, the server "
      + "component may not be available to respond to requests from "
      + "DynaTableRf.  Try running DynaTable in development mode to see the demo "
      + "in action.</p> ";

  private final RowDataAcceptor acceptor = new RowDataAcceptorImpl();

  @UiField
  Grid grid;

  @UiField(provided = true)
  NavBar navbar = new NavBar();

  private ErrorDialog errorDialog = null;

  private final DynaTableDataProvider provider;

  private int startRow = 0;

  public DynaTableWidget(DynaTableDataProvider provider, int rowCount) {
    this.provider = provider;
    initWidget(GWT.<Binder> create(Binder.class).createAndBindUi(this));
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
    provider.updateRowData(startRow, grid.getRowCount() - 1, acceptor);
  }

  public void setRowCount(int rows) {
    grid.resizeRows(rows);
  }

  public void setStatusText(String text) {
    navbar.status.setInnerText(text);
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
