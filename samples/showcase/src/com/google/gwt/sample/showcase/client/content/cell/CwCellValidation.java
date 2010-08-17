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
package com.google.gwt.sample.showcase.client.content.cell;

import com.google.gwt.cell.client.AbstractEditableCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseRaw;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.content.cell.ContactDatabase.ContactInfo;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 */
@ShowcaseRaw({"ContactDatabase.java"})
public class CwCellValidation extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants
      extends Constants, ContentWidget.CwConstants {
    String cwCellValidationColumnAddress();

    String cwCellValidationColumnName();

    String cwCellValidationDescription();

    String cwCellValidationError();

    String cwCellValidationName();
  }

  /**
   * The ViewData used by {@link ValidatableInputCell}.
   */
  @ShowcaseSource
  private static class ValidationData {
    private String value;
    private boolean invalid;

    public String getValue() {
      return value;
    }

    public boolean isInvalid() {
      return invalid;
    }

    public void setInvalid(boolean invalid) {
      this.invalid = invalid;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  /**
   * An input cell that changes color based on the validation status.
   */
  @ShowcaseSource
  private static class ValidatableInputCell extends AbstractEditableCell<
      String, ValidationData> {

    private String errorMessage;

    public ValidatableInputCell(String errorMessage) {
      super("change");
      this.errorMessage = errorMessage;
    }

    @Override
    public void onBrowserEvent(Element parent, String value, Object key,
        NativeEvent event, ValueUpdater<String> valueUpdater) {
      ValidationData viewData = getViewData(key);

      if (event.getType().equals("change")) {
        InputElement input = parent.getFirstChild().cast();

        // Mark cell as containing a pending change
        input.getStyle().setColor("blue");

        // Save the new value in the view data.
        if (viewData == null) {
          viewData = new ValidationData();
          setViewData(key, viewData);
        }
        String newValue = input.getValue();
        viewData.setValue(newValue);

        // Update the value updater, which updates the field updater.
        if (valueUpdater != null) {
          valueUpdater.update(newValue);
        }
      }
    }

    @Override
    public void render(String value, Object key, StringBuilder sb) {
      // Get the view data.
      ValidationData viewData = getViewData(key);
      if (viewData != null && viewData.getValue().equals(value)) {
        // Clear the view data if the value is the same as the current value.
        clearViewData(key);
        viewData = null;
      }

      /*
       * If viewData is null, just paint the contents black. If it is non-null,
       * show the pending value and paint the contents red if they are known to
       * be invalid.
       */
      String pendingValue = (viewData == null) ? null : viewData.getValue();
      boolean invalid = (viewData == null) ? false : viewData.isInvalid();

      sb.append("<input type=\"text\" value=\"");
      if (pendingValue != null) {
        sb.append(pendingValue);
      } else {
        sb.append(value);
      }
      sb.append("\" style=\"color:");
      if (pendingValue != null) {
        sb.append(invalid ? "red" : "blue");
      } else {
        sb.append("black");
      }
      sb.append("\"></input>");

      if (invalid) {
        sb.append("<span style='color:red;'>");
        sb.append(errorMessage);
        sb.append("</span>");
      }
    }
  }

  /**
   * Checks if an address is valid. A valid address consists of a number
   * followed by a street name, which may be composed of multiple words.
   *
   * @param address the address
   * @return true if valid, false if invalid
   */
  @ShowcaseSource
  public static boolean isAddressValid(String address) {
    // Cannot be null.
    if (address == null) {
      return false;
    }

    // Must have two or more parts.
    String[] parts = address.split(" ");
    if (parts.length < 2) {
      return false;
    }

    // First part is a number.
    try {
      Integer.parseInt(parts[0]);
    } catch (NumberFormatException e) {
      return false;
    }

    // The remaining parts form the street name.
    return true;
  }

  /**
   * An instance of the constants.
   */
  @ShowcaseData
  private CwConstants constants;

  /**
   * Constructor.
   *
   * @param constants the constants
   */
  public CwCellValidation(CwConstants constants) {
    super(constants);
    this.constants = constants;
    registerSource("ContactDatabase.java");
  }

  @Override
  public String getDescription() {
    return constants.cwCellValidationDescription();
  }

  @Override
  public String getName() {
    return constants.cwCellValidationName();
  }

  @Override
  public boolean hasStyle() {
    return false;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Create a table.
    final CellTable<ContactInfo> table = new CellTable<ContactInfo>(10);
    table.setKeyProvider(ContactInfo.KEY_PROVIDER);

    // Add the Name column.
    table.addColumn(new Column<ContactInfo, String>(new TextCell()) {
      @Override
      public String getValue(ContactInfo object) {
        return object.getFullName();
      }
    }, constants.cwCellValidationColumnName());

    // Add an editable address column.
    final ValidatableInputCell addressCell = new ValidatableInputCell(
        constants.cwCellValidationError());
    Column<ContactInfo, String> addressColumn = new Column<ContactInfo, String>(
        addressCell) {
      @Override
      public String getValue(ContactInfo object) {
        return object.getAddress();
      }
    };
    table.addColumn(addressColumn, constants.cwCellValidationColumnAddress());
    addressColumn.setFieldUpdater(new FieldUpdater<ContactInfo, String>() {
      public void update(
          int index, final ContactInfo object, final String value) {
        // Perform validation after 2 seconds to simulate network delay.
        new Timer() {
          @Override
          public void run() {
            if (isAddressValid(value)) {
              // The cell will clear the view data when it sees the updated
              // value.
              object.setAddress(value);

              // Push the change to the views.
              ContactDatabase.get().refreshDisplays();
            } else {
              // Update the view data to mark the pending value as invalid.
              ValidationData viewData = addressCell.getViewData(
                  ContactInfo.KEY_PROVIDER.getKey(object));
              viewData.setInvalid(true);

              // We only modified the cell, so do a local redraw.
              table.redraw();
            }
          }
        }.schedule(1000);
      }
    });

    // Add the table to the database.
    ContactDatabase.get().addDataDisplay(table);

    return table;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwCellValidation.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }

  @Override
  protected void setRunAsyncPrefetches() {
    prefetchCellWidgets();
  }
}
