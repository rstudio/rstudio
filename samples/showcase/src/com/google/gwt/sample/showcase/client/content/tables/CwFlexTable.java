/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.sample.showcase.client.content.tables;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.Showcase;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseStyle;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;

/**
 * Example file.
 */
@ShowcaseStyle(".cw-FlexTable")
public class CwFlexTable extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String cwFlexTableAddRow();

    String cwFlexTableDescription();

    String cwFlexTableDetails();

    String cwFlexTableName();

    String cwFlexTableRemoveRow();
  }

  /**
   * An instance of the constants.
   */
  @ShowcaseData
  private final CwConstants constants;

  /**
   * Constructor.
   *
   * @param constants the constants
   */
  public CwFlexTable(CwConstants constants) {
    super(
        constants.cwFlexTableName(), constants.cwFlexTableDescription(), true);
    this.constants = constants;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Create a Flex Table
    final FlexTable flexTable = new FlexTable();
    FlexCellFormatter cellFormatter = flexTable.getFlexCellFormatter();
    flexTable.addStyleName("cw-FlexTable");
    flexTable.setWidth("32em");
    flexTable.setCellSpacing(5);
    flexTable.setCellPadding(3);

    // Add some text
    cellFormatter.setHorizontalAlignment(
        0, 1, HasHorizontalAlignment.ALIGN_LEFT);
    flexTable.setHTML(0, 0, constants.cwFlexTableDetails());
    cellFormatter.setColSpan(0, 0, 2);

    // Add a button that will add more rows to the table
    Button addRowButton = new Button(
        constants.cwFlexTableAddRow(), new ClickHandler() {
          public void onClick(ClickEvent event) {
            addRow(flexTable);
          }
        });
    addRowButton.addStyleName("sc-FixedWidthButton");

    Button removeRowButton = new Button(
        constants.cwFlexTableRemoveRow(), new ClickHandler() {
          public void onClick(ClickEvent event) {
            removeRow(flexTable);
          }
        });
    removeRowButton.addStyleName("sc-FixedWidthButton");
    VerticalPanel buttonPanel = new VerticalPanel();
    buttonPanel.setStyleName("cw-FlexTable-buttonPanel");
    buttonPanel.add(addRowButton);
    buttonPanel.add(removeRowButton);
    flexTable.setWidget(0, 1, buttonPanel);
    cellFormatter.setVerticalAlignment(0, 1, HasVerticalAlignment.ALIGN_TOP);

    // Add two rows to start
    addRow(flexTable);
    addRow(flexTable);

    // Return the panel
    flexTable.ensureDebugId("cwFlexTable");
    return flexTable;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwFlexTable.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }

  /**
   * Add a row to the flex table.
   */
  @ShowcaseSource
  private void addRow(FlexTable flexTable) {
    int numRows = flexTable.getRowCount();
    flexTable.setWidget(numRows, 0, new Image(Showcase.images.gwtLogo()));
    flexTable.setWidget(numRows, 1, new Image(Showcase.images.gwtLogo()));
    flexTable.getFlexCellFormatter().setRowSpan(0, 1, numRows + 1);
  }

  /**
   * Remove a row from the flex table.
   */
  @ShowcaseSource
  private void removeRow(FlexTable flexTable) {
    int numRows = flexTable.getRowCount();
    if (numRows > 1) {
      flexTable.removeRow(numRows - 1);
      flexTable.getFlexCellFormatter().setRowSpan(0, 1, numRows - 1);
    }
  }
}
