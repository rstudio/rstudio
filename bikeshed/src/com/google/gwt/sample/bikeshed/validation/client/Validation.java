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
package com.google.gwt.sample.bikeshed.validation.client;

import com.google.gwt.bikeshed.cells.client.FieldUpdater;
import com.google.gwt.bikeshed.cells.client.TextCell;
import com.google.gwt.bikeshed.list.client.Column;
import com.google.gwt.bikeshed.list.client.PagingTableListView;
import com.google.gwt.bikeshed.list.shared.ListListModel;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.RootPanel;

import java.util.List;

/**
 * Validation demo.
 */
public class Validation implements EntryPoint {

  static class Address {
    static int genkey = 0;
    int key;
    String state;
    String zip;
    boolean zipInvalid;

    public Address(Address address) {
      this.key = address.key;
      this.state = address.state;
      this.zip = address.zip;
    }

    public Address(String state, String zip) {
      this.key = genkey++;
      this.state = state;
      this.zip = zip;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof Address)) {
        return false;
      }
      return ((Address) other).key == key;
    }

    @Override
    public int hashCode() {
      return key;
    }
  }

  public static boolean zipInvalid(int zip) {
    return zip % 3 == 0;
  }

  public void onModuleLoad() {
    ListListModel<Address> listModel = new ListListModel<Address>();
    final List<Address> list = listModel.getList();
    for (int i = 10; i < 50; i++) {
      if (zipInvalid(30000 + i)) {
        continue;
      }

      String zip = "300" + i;
      list.add(new Address("GA", zip));
    }

    PagingTableListView<Address> table = new PagingTableListView<Address>(
        listModel, 10);
    Column<Address, String, Void> stateColumn = new Column<Address, String, Void>(
        new TextCell()) {
      @Override
      protected String getValue(Address object) {
        return object.state;
      }
    };

    Column<Address, String, ValidatableField<String>> zipColumn =
      new Column<Address, String, ValidatableField<String>>(
        new ValidatableInputCell()) {
      @Override
      protected String getValue(Address object) {
        return object.zip;
      }
    };
    zipColumn.setFieldUpdater(new FieldUpdater<Address, String, ValidatableField<String>>() {
      public void update(final int index, final Address object,
          final String value, final ValidatableField<String> viewData) {
        // Perform validation after a 2-second delay
        new Timer() {
          @Override
          public void run() {
            String pendingValue = viewData.getValue();

            int zip = Integer.parseInt(pendingValue);
            boolean zipInvalid = Validation.zipInvalid(zip);

            final Address newValue = new Address(object);
            newValue.zip = pendingValue == null ? value : pendingValue;
            newValue.zipInvalid = zipInvalid;

            viewData.setInvalid(zipInvalid);
            if (!zipInvalid) {
              viewData.setValue(null);
            }

            list.set(index, newValue);
          }
        }.schedule(2000);
      }
    });

    Column<Address, String, Void> messageColumn = new Column<Address, String, Void>(
        new TextCell()) {
      @Override
      protected String getValue(Address object) {
        return object.zipInvalid ? "Please fix the zip code" : "";
      }
    };

    table.addColumn(stateColumn);
    table.addColumn(zipColumn);
    table.addColumn(messageColumn);

    RootPanel.get().add(table);
  }
}
