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
package com.google.gwt.sample.mail.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;

/**
 * A composite that displays a list of emails that can be selected.
 */
public class MailList extends ResizeComposite {

  /**
   * Callback when mail items are selected.
   */
  public interface Listener {
    void onItemSelected(MailItem item);
  }

  interface Binder extends UiBinder<Widget, MailList> { }
  interface SelectionStyle extends CssResource {
    String selectedRow();
  }

  private static final Binder binder = GWT.create(Binder.class);
  static final int VISIBLE_EMAIL_COUNT = 20;

  @UiField FlexTable header;
  @UiField FlexTable table;
  @UiField SelectionStyle selectionStyle;

  private Listener listener;
  private int startIndex, selectedRow = -1;
  private NavBar navBar;

  public MailList() {
    initWidget(binder.createAndBindUi(this));
    navBar = new NavBar(this);

    initTable();
    update();
  }

  /**
   * Sets the listener that will be notified when an item is selected.
   */
  public void setListener(Listener listener) {
    this.listener = listener;
  }

  @Override
  protected void onLoad() {
    // Select the first row if none is selected.
    if (selectedRow == -1) {
      selectRow(0);
    }
  }

  void newer() {
    // Move back a page.
    startIndex -= VISIBLE_EMAIL_COUNT;
    if (startIndex < 0) {
      startIndex = 0;
    } else {
      styleRow(selectedRow, false);
      selectedRow = -1;
      update();
    }
  }

  void older() {
    // Move forward a page.
    startIndex += VISIBLE_EMAIL_COUNT;
    if (startIndex >= MailItems.getMailItemCount()) {
      startIndex -= VISIBLE_EMAIL_COUNT;
    } else {
      styleRow(selectedRow, false);
      selectedRow = -1;
      update();
    }
  }

  @UiHandler("table")
  void onTableClicked(ClickEvent event) {
    // Select the row that was clicked (-1 to account for header row).
    Cell cell = table.getCellForEvent(event);
    if (cell != null) {
      int row = cell.getRowIndex();
      selectRow(row);
    }
  }

  /**
   * Initializes the table so that it contains enough rows for a full page of
   * emails. Also creates the images that will be used as 'read' flags.
   */
  private void initTable() {
    // Initialize the header.
    header.getColumnFormatter().setWidth(0, "128px");
    header.getColumnFormatter().setWidth(1, "192px");
    header.getColumnFormatter().setWidth(3, "256px");

    header.setText(0, 0, "Sender");
    header.setText(0, 1, "Email");
    header.setText(0, 2, "Subject");
    header.setWidget(0, 3, navBar);
    header.getCellFormatter().setHorizontalAlignment(0, 3, HasHorizontalAlignment.ALIGN_RIGHT);

    // Initialize the table.
    table.getColumnFormatter().setWidth(0, "128px");
    table.getColumnFormatter().setWidth(1, "192px");
  }

  /**
   * Selects the given row (relative to the current page).
   *
   * @param row the row to be selected
   */
  private void selectRow(int row) {
    // When a row (other than the first one, which is used as a header) is
    // selected, display its associated MailItem.
    MailItem item = MailItems.getMailItem(startIndex + row);
    if (item == null) {
      return;
    }

    styleRow(selectedRow, false);
    styleRow(row, true);

    item.read = true;
    selectedRow = row;

    if (listener != null) {
      listener.onItemSelected(item);
    }
  }

  private void styleRow(int row, boolean selected) {
    if (row != -1) {
      String style = selectionStyle.selectedRow();

      if (selected) {
        table.getRowFormatter().addStyleName(row, style);
      } else {
        table.getRowFormatter().removeStyleName(row, style);
      }
    }
  }

  private void update() {
    // Update the older/newer buttons & label.
    int count = MailItems.getMailItemCount();
    int max = startIndex + VISIBLE_EMAIL_COUNT;
    if (max > count) {
      max = count;
    }

    // Update the nav bar.
    navBar.update(startIndex, count, max);

    // Show the selected emails.
    int i = 0;
    for (; i < VISIBLE_EMAIL_COUNT; ++i) {
      // Don't read past the end.
      if (startIndex + i >= MailItems.getMailItemCount()) {
        break;
      }

      MailItem item = MailItems.getMailItem(startIndex + i);

      // Add a new row to the table, then set each of its columns to the
      // email's sender and subject values.
      table.setText(i, 0, item.sender);
      table.setText(i, 1, item.email);
      table.setText(i, 2, item.subject);
    }

    // Clear any remaining slots.
    for (; i < VISIBLE_EMAIL_COUNT; ++i) {
      table.removeRow(table.getRowCount() - 1);
    }
  }
}
