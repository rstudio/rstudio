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
package com.google.gwt.examples.cellview;

import com.google.gwt.cell.client.DatePickerCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextInputCell;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.view.client.ProvidesKey;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Example of using a {@link FieldUpdater} with a {@link CellTable}.
 */
public class CellTableFieldUpdaterExampleComplex implements EntryPoint {

  /**
   * A simple data type that represents a contact with a unique ID.
   */
  private static class Contact {
    private static int nextId = 0;

    private final int id;
    private final String address;
    private Date birthday;
    private String name;

    public Contact(String name, Date birthday, String address) {
      nextId++;
      this.id = nextId;
      this.name = name;
      this.birthday = birthday;
      this.address = address;
    }
  }

  /**
   * The list of data to display.
   */
  private static final List<Contact> CONTACTS = Arrays.asList(
      new Contact("John", new Date(80, 4, 12), "123 Fourth Avenue"),
      new Contact("Joe", new Date(85, 2, 22), "22 Lance Ln"),
      new Contact("George", new Date(46, 6, 6), "1600 Pennsylvania Avenue"));

  /**
   * The key provider that allows us to identify Contacts even if a field
   * changes. We identify contacts by their unique ID.
   */
  private static final ProvidesKey<Contact> KEY_PROVIDER = new ProvidesKey<CellTableFieldUpdaterExampleComplex.Contact>() {
    @Override
    public Object getKey(Contact item) {
      return item.id;
    }
  };

  @Override
  public void onModuleLoad() {
    // Create a CellTable with a key provider.
    final CellTable<Contact> table = new CellTable<Contact>(KEY_PROVIDER);
    table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.ENABLED);

    // Add a text input column to edit the name.
    final TextInputCell nameCell = new TextInputCell();
    Column<Contact, String> nameColumn = new Column<Contact, String>(nameCell) {
      @Override
      public String getValue(Contact object) {
        return object.name;
      }
    };
    table.addColumn(nameColumn, "Name");

    // Add a field updater to be notified when the user enters a new name.
    nameColumn.setFieldUpdater(new FieldUpdater<Contact, String>() {
      @Override
      public void update(int index, Contact object, String value) {
        // Validate the data.
        if (value.length() < 3) {
          Window.alert("Names must be at least three characters long.");

          /*
           * Clear the view data. The view data contains the pending change and
           * allows the table to render with the pending value until the data is
           * committed. If the data is committed into the object, the view data
           * is automatically cleared out. If the data is not committed because
           * it is invalid, you must delete.
           */
          nameCell.clearViewData(KEY_PROVIDER.getKey(object));

          // Redraw the table.
          table.redraw();
          return;
        }

        // Inform the user of the change.
        Window.alert("You changed the name of " + object.name + " to " + value);

        // Push the changes into the Contact. At this point, you could send an
        // asynchronous request to the server to update the database.
        object.name = value;

        // Redraw the table with the new data.
        table.redraw();
      }
    });

    // Add a date column to show the birthday.
    Column<Contact, Date> dateColumn = new Column<Contact, Date>(
        new DatePickerCell()) {
      @Override
      public Date getValue(Contact object) {
        return object.birthday;
      }
    };
    table.addColumn(dateColumn, "Birthday");

    // Add a field updater to be notified when the user enters a new birthday.
    dateColumn.setFieldUpdater(new FieldUpdater<Contact, Date>() {
      @Override
      public void update(int index, Contact object, Date value) {
        Window.alert("You changed the birthday of "
            + object.name
            + " to "
            + DateTimeFormat.getFormat(PredefinedFormat.DATE_LONG).format(value));

        // Push the changes into the Contact.
        object.birthday = value;

        // Redraw the table with the new data.
        table.redraw();
      }
    });
    // Add a text column to show the address.
    TextColumn<Contact> addressColumn = new TextColumn<Contact>() {
      @Override
      public String getValue(Contact object) {
        return object.address;
      }
    };
    table.addColumn(addressColumn, "Address");

    // Set the total row count. This isn't strictly necessary, but it affects
    // paging calculations, so its good habit to keep the row count up to date.
    table.setRowCount(CONTACTS.size(), true);

    // Push the data into the widget.
    table.setRowData(0, CONTACTS);

    // Add it to the root panel.
    RootPanel.get().add(table);
  }
}