/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.sample.dynatable.client;

import com.google.gwt.sample.dynatable.client.DynaTableDataProvider.RowDataAcceptor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

public class DynaTableWidget extends Composite {

  private class NavBar extends Composite implements ClickListener {

    public NavBar() {
      initWidget(bar);
      bar.setStyleName("navbar");
      status.setStyleName("status");

      HorizontalPanel buttons = new HorizontalPanel();
      buttons.add(gotoFirst);
      buttons.add(gotoPrev);
      buttons.add(gotoNext);
      bar.add(buttons, DockPanel.EAST);
      bar.setCellHorizontalAlignment(buttons, DockPanel.ALIGN_RIGHT);
      bar.add(status, DockPanel.CENTER);
      bar.setVerticalAlignment(DockPanel.ALIGN_MIDDLE);
      bar.setCellHorizontalAlignment(status, HasAlignment.ALIGN_RIGHT);
      bar.setCellVerticalAlignment(status, HasAlignment.ALIGN_MIDDLE);
      bar.setCellWidth(status, "100%");

      // Initialize prev & first button to disabled.
      //
      gotoPrev.setEnabled(false);
      gotoFirst.setEnabled(false);
    }

    public void onClick(Widget sender) {
      if (sender == gotoNext) {
        startRow += getDataRowCount();
        refresh();
      } else if (sender == gotoPrev) {
        startRow -= getDataRowCount();
        if (startRow < 0) {
          startRow = 0;
        }
        refresh();
      } else if (sender == gotoFirst) {
        startRow = 0;
        refresh();
      }
    }

    public final DockPanel bar = new DockPanel();
    public final Button gotoFirst = new Button("&lt;&lt;", this);
    public final Button gotoNext = new Button("&gt;", this);
    public final Button gotoPrev = new Button("&lt;", this);
    public final HTML status = new HTML();
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
      //
      navbar.gotoNext.setEnabled(!isLastPage);
      navbar.gotoFirst.setEnabled(startRow > 0);
      navbar.gotoPrev.setEnabled(startRow > 0);

      // Update the status message.
      //
      setStatusText((startRow + 1) + " - " + (startRow + srcRowCount));
    }

    public void failed(Throwable caught) {
      String msg = "Failed to access data";
      if (caught != null) {
        msg += ": " + caught.getMessage();
      }
      setStatusText(msg);
    }
  }

  public DynaTableWidget(DynaTableDataProvider provider, String[] columns,
      String[] columnStyles, int rowCount) {

    if (columns.length == 0) {
      throw new IllegalArgumentException(
        "expecting a positive number of columns");
    }

    if (columnStyles != null && columns.length != columnStyles.length) {
      throw new IllegalArgumentException("expecting as many styles as columns");
    }

    this.provider = provider;
    initWidget(outer);
    grid.setStyleName("table");
    outer.add(navbar, DockPanel.NORTH);
    outer.add(grid, DockPanel.CENTER);
    initTable(columns, columnStyles, rowCount);
    setStyleName("DynaTable-DynaTableWidget");
  }

  private void initTable(String[] columns, String[] columnStyles, int rowCount) {
    // Set up the header row.  It's one greater than the number of visible rows.
    //
    grid.resize(rowCount+1, columns.length);
    for (int i = 0, n = columns.length; i < n; i++) {
      grid.setText(0, i, columns[i]);
      if (columnStyles != null) {
        grid.getCellFormatter().setStyleName(0, i, columnStyles[i] + " header");
      }
    }
  }

  public void setStatusText(String text) {
    navbar.status.setText(text);
  }

  public void clearStatusText() {
    navbar.status.setHTML("&nbsp;");
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

  private int getDataRowCount() {
    return grid.getRowCount() - 1;
  }

  private final RowDataAcceptor acceptor = new RowDataAcceptorImpl();
  private final NavBar navbar = new NavBar();
  private final DockPanel outer = new DockPanel();
  private final DynaTableDataProvider provider;
  private int startRow = 0;
  private final Grid grid = new Grid();
}
