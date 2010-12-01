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
package com.google.gwt.examples.cell;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Example of creating an editable {@link Cell}. This example creates a cell
 * that displays a Contact with a checkbox that indicates whether the contact is
 * a favorite or not.
 */
public class EditableCellExample implements EntryPoint {

  /**
   * A simple data type that represents a contact.
   */
  private static class Contact {
    private final String address;
    private final String name;

    public Contact(String name, String address) {
      this.name = name;
      this.address = address;
    }
  }

  /**
   * A custom {@link Cell} used to render a {@link Contact}. We extend
   * {@link AbstractCell} because it provides reasonable implementations of
   * methods that work for most use cases.
   */
  private class ContactCell extends AbstractCell<Contact> {

    public ContactCell() {
      // Our cell responds to change events and keydown events.
      super("change", "keydown");
    }

    @Override
    public void onBrowserEvent(Context context, Element parent, Contact value,
        NativeEvent event, ValueUpdater<Contact> valueUpdater) {
      // Check that the value is not null.
      if (value == null) {
        return;
      }

      // Call the super handler, which handlers the enter key.
      super.onBrowserEvent(context, parent, value, event, valueUpdater);

      // Handle click events.
      if ("change".equals(event.getType())) {
        updateFavorites(parent, value);
        showCurrentFavorites();
      }
    }

    @Override
    public void render(Context context, Contact value, SafeHtmlBuilder sb) {
      /*
       * Always do a null check on the value. Cell widgets can pass null to
       * cells if the underlying data contains a null, or if the data arrives
       * out of order.
       */
      if (value == null) {
        return;
      }

      // Add a checkbox. If the contact is a favorite, the box will be checked.
      sb.appendHtmlConstant("<table><tr><td valign=\"top\">");
      if (favorites.contains(value)) {
        sb.appendHtmlConstant("<input type=\"checkbox\" checked=checked/>");
      } else {
        sb.appendHtmlConstant("<input type=\"checkbox\" />");
      }
      sb.appendHtmlConstant("</td><td>");

      // Display the name in big letters.
      sb.appendHtmlConstant("<div style=\"size:200%;font-weight:bold;\">");
      sb.appendEscaped(value.name);
      sb.appendHtmlConstant("</div>");

      // Display the address in normal text.
      sb.appendHtmlConstant("<div style=\"padding-left:10px;\">");
      sb.appendEscaped(value.address);
      sb.appendHtmlConstant("</div>");

      sb.appendHtmlConstant("</td></tr></table>");
    }

    /**
     * By convention, cells that respond to user events should handle the enter
     * key. This provides a consistent user experience when users use keyboard
     * navigation in the widget. Our cell will toggle the checkbox on Enter.
     */
    @Override
    protected void onEnterKeyDown(Context context, Element parent,
        Contact value, NativeEvent event, ValueUpdater<Contact> valueUpdater) {
      // Toggle the checkbox.
      InputElement input = getInputElement(parent);
      input.setChecked(!input.isChecked());

      // Update the favorites based on the new state.
      updateFavorites(parent, value);

      // Show the new list of favorites.
      showCurrentFavorites();
    }

    /**
     * Get the checkbox input element from the parent element that wraps our
     * cell.
     * 
     * @param parent the parent element
     * @return the checkbox
     */
    private InputElement getInputElement(Element parent) {
      // We need to navigate down to our input element.
      TableElement table = parent.getFirstChildElement().cast();
      TableRowElement tr = table.getRows().getItem(0);
      TableCellElement td = tr.getCells().getItem(0);
      InputElement input = td.getFirstChildElement().cast();
      return input;
    }

    /**
     * Update the favorites list based on the state of the input element.
     */
    private void updateFavorites(Element parent, Contact value) {
      // Get the input element.
      InputElement input = getInputElement(parent);

      // Update the favorites based on the checked state.
      if (input.isChecked()) {
        favorites.add(value);
      } else {
        favorites.remove(value);
      }
    }
  }

  /**
   * The list of data to display.
   */
  private static final List<Contact> CONTACTS = Arrays.asList(new Contact(
      "John", "123 Fourth Avenue"), new Contact("Joe", "22 Lance Ln"),
      new Contact("Michael", "1283 Berry Blvd"), new Contact("Sarah",
          "100 Hundred St."), new Contact("George", "1600 Pennsylvania Avenue"));

  /**
   * Our list of favorite contacts.
   */
  private final List<Contact> favorites = new ArrayList<EditableCellExample.Contact>();

  public void onModuleLoad() {
    // Create a cell to render each value.
    ContactCell contactCell = new ContactCell();

    // Use the cell in a CellList.
    CellList<Contact> cellList = new CellList<Contact>(contactCell);

    // Push the data into the widget.
    cellList.setRowData(0, CONTACTS);

    // Add it to the root panel.
    RootPanel.get().add(cellList);
  }

  /**
   * Show the list of favorites.
   */
  private void showCurrentFavorites() {
    if (favorites.size() > 0) {
      String text = "You favorite contacts are ";
      boolean first = true;
      for (Contact contact : favorites) {
        if (!first) {
          text += ", ";
        } else {
          first = false;
        }
        text += contact.name;
      }
      Window.alert(text);
    } else {
      Window.alert("You have not selected any favorites.");
    }
  }
}