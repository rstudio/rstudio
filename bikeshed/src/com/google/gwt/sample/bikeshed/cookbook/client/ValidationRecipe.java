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
package com.google.gwt.sample.bikeshed.cookbook.client;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListViewAdapter;

import java.util.List;

/**
 * Validation demo.
 */
public class ValidationRecipe extends Recipe {

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
    return (zip < 0) || (zip % 3 == 0);
  }

  public ValidationRecipe() {
    super("Validation");
  }

  @Override
  protected Widget createWidget() {
    ListViewAdapter<Address> adapter = new ListViewAdapter<Address>();
    final List<Address> list = adapter.getList();
    for (int i = 10; i < 50; i++) {
      if (zipInvalid(30000 + i)) {
        continue;
      }

      String zip = "300" + i;
      list.add(new Address("GA", zip));
    }

    CellTable<Address> table = new CellTable<Address>(10);
    adapter.addView(table);
    TextColumn<Address> stateColumn = new TextColumn<Address>() {
      @Override
      public String getValue(Address object) {
        return object.state;
      }
    };

    final Column<Address, String> zipColumn =
      new Column<Address, String>(new ValidatableInputCell()) {
      @Override
      public String getValue(Address object) {
        return object.zip;
      }
    };
    zipColumn.setFieldUpdater(new FieldUpdater<Address, String>() {
      public void update(final int index, final Address address,
          final String value) {
        // Perform validation after a 2-second delay
        new Timer() {
          @Override
          public void run() {
            // Determine whether we have a valid zip code.
            int zip;
            try {
              zip = Integer.parseInt(value);
            } catch (NumberFormatException e) {
              zip = -1;
            }
            boolean zipInvalid = ValidationRecipe.zipInvalid(zip);

            // Update the value.
            final Address newAddress = new Address(address);
            newAddress.zip = value;
            newAddress.zipInvalid = zipInvalid;

            if (zipInvalid) {
              ValidatableInputCell.invalidate(zipColumn, newAddress,
                  newAddress.zip);
            } else {
              ValidatableInputCell.validate(zipColumn, newAddress);
            }

            list.set(index, newAddress);
          }
        }.schedule(2000);
      }
    });

    TextColumn<Address> messageColumn = new TextColumn<Address>() {
      @Override
      public String getValue(Address object) {
        return object.zipInvalid ? "Please fix the zip code" : "";
      }
    };

    table.addColumn(stateColumn);
    table.addColumn(zipColumn);
    table.addColumn(messageColumn);

    return table;
  }
}
