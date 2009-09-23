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

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.LayoutComposite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.HTMLTable.Cell;

/**
 * A composite that displays a list of emails that can be selected.
 */
public class MailList extends LayoutComposite implements ClickHandler {

  private static final int VISIBLE_EMAIL_COUNT = 20;

  /**
   * Callback when mail items are selected. 
   */
  public interface Listener {
    void onItemSelected(MailItem item);
  }

  private Listener listener;

  private HTML countLabel = new HTML();
  private HTML newerButton = new HTML("<a href='javascript:;'>&lt; newer</a>",
      true);
  private HTML olderButton = new HTML("<a href='javascript:;'>older &gt;</a>",
      true);
  private int startIndex, selectedRow = -1;
  private FlexTable header = new FlexTable();
  private FlexTable table = new FlexTable();
  private HorizontalPanel navBar = new HorizontalPanel();

  public MailList() {
    // Setup the table.
    table.setCellSpacing(0);
    table.setCellPadding(0);
    table.setWidth("100%");

    // Hook up events.
    table.addClickHandler(this);
    newerButton.addClickHandler(this);
    olderButton.addClickHandler(this);

    // Create the 'navigation' bar at the upper-right.
    HorizontalPanel innerNavBar = new HorizontalPanel();
    navBar.setStyleName("mail-ListNavBar");
    innerNavBar.add(newerButton);
    innerNavBar.add(countLabel);
    innerNavBar.add(olderButton);

    navBar.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
    navBar.add(innerNavBar);
    navBar.setWidth("100%");

    DockLayoutPanel dock = new DockLayoutPanel(Unit.EM);
    dock.addNorth(header, 2);
    dock.add(new ScrollPanel(table));
    header.setWidth("100%");
    table.setWidth("100%");
    dock.layout();
    initWidget(dock);
    setStyleName("mail-List");

    initTable();
    update();
  }

  public void onClick(ClickEvent event) {
    Object sender = event.getSource();
    if (sender == olderButton) {
      // Move forward a page.
      startIndex += VISIBLE_EMAIL_COUNT;
      if (startIndex >= MailItems.getMailItemCount()) {
        startIndex -= VISIBLE_EMAIL_COUNT;
      } else {
        styleRow(selectedRow, false);
        selectedRow = -1;
        update();
      }
    } else if (sender == newerButton) {
      // Move back a page.
      startIndex -= VISIBLE_EMAIL_COUNT;
      if (startIndex < 0) {
        startIndex = 0;
      } else {
        styleRow(selectedRow, false);
        selectedRow = -1;
        update();
      }
    } else if (sender == table) {
      // Select the row that was clicked (-1 to account for header row).
      Cell cell = table.getCellForEvent(event);
      if (cell != null) {
        int row = cell.getRowIndex();
        selectRow(row);
      }
    }
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

  /**
   * Initializes the table so that it contains enough rows for a full page of
   * emails. Also creates the images that will be used as 'read' flags.
   */
  private void initTable() {
    // Create the header.
    header.getRowFormatter().setStyleName(0, "mail-ListHeader");
    header.getElement().getStyle().setProperty("tableLayout", "fixed");
    header.setCellSpacing(0);

    header.getColumnFormatter().setWidth(0, "96px");
    header.getColumnFormatter().setWidth(1, "160px");
    header.getColumnFormatter().setWidth(3, "256px");

    header.setText(0, 0, "Sender");
    header.setText(0, 1, "Email");
    header.setText(0, 2, "Subject");
    header.setWidget(0, 3, navBar);

    // Initialize the table.
    table.getElement().getStyle().setProperty("tableLayout", "fixed");
    header.setCellSpacing(0);

    table.getColumnFormatter().setWidth(0, "96px");
    table.getColumnFormatter().setWidth(1, "160px");

    for (int i = 0; i < VISIBLE_EMAIL_COUNT; ++i) {
      table.setText(i, 0, "");
      table.setText(i, 1, "");
      table.setText(i, 2, "");
      table.getCellFormatter().setWordWrap(i, 0, false);
      table.getCellFormatter().setWordWrap(i, 1, false);
      table.getCellFormatter().setWordWrap(i, 2, false);
      table.getFlexCellFormatter().setColSpan(i, 2, 2);
    }
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
      if (selected) {
        table.getRowFormatter().addStyleName(row, "mail-SelectedRow");
      } else {
        table.getRowFormatter().removeStyleName(row, "mail-SelectedRow");
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

    newerButton.setVisible(startIndex != 0);
    olderButton.setVisible(startIndex + VISIBLE_EMAIL_COUNT < count);
    countLabel.setText("" + (startIndex + 1) + " - " + max + " of " + count);

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
      table.setHTML(i, 0, "&nbsp;");
      table.setHTML(i, 1, "&nbsp;");
      table.setHTML(i, 2, "&nbsp;");
    }
  }
}
