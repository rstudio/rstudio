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
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Example of creating a custom {@link Cell} that responds to events. This
 * example creates a cell that displays a Contact in a custom format.
 */
public class InteractionCellExample implements EntryPoint {

  /**
   * A simple data type that represents a contact.
   */
  private static class Contact {
    private final String address;
    private final Date birthday;
    private final String name;

    public Contact(String name, Date birthday, String address) {
      this.name = name;
      this.birthday = birthday;
      this.address = address;
    }
  }

  /**
   * A custom {@link Cell} used to render a {@link Contact}. We extend
   * {@link AbstractCell} because it provides reasonable implementations of
   * methods that work for most use cases.
   */
  private static class ContactCell extends AbstractCell<Contact> {

    /**
     * The {@link DateTimeFormat} used to format the birthday. Since our render
     * method is called for every value in the list, we want to move heavy
     * weight work, such as creating a formatter, out of it to speed up
     * rendering as much as possible.
     */
    private DateTimeFormat dateFormat = DateTimeFormat.getFormat(PredefinedFormat.DATE_LONG);

    public ContactCell() {
      /*
       * Let the parent class know that our cell responds to click events and
       * keydown events.
       */
      super("click", "keydown");
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

      // On click, perform the same action that we perform on enter.
      if ("click".equals(event.getType())) {
        this.onEnterKeyDown(context, parent, value, event, valueUpdater);
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

      // Display the name in big letters.
      sb.appendHtmlConstant("<div style=\"size:200%;font-weight:bold;\">");
      sb.appendEscaped(value.name);
      sb.appendHtmlConstant("</div>");

      // Display the address in normal text.
      sb.appendHtmlConstant("<div style=\"padding-left:10px;\">");
      sb.appendEscaped(value.address);
      sb.appendHtmlConstant("</div>");

      // Format that birthday and display it in light gray.
      sb.appendHtmlConstant("<div style=\"padding-left:10px;color:#aaa;\">");
      sb.append(SafeHtmlUtils.fromTrustedString("Born: "));
      sb.appendEscaped(dateFormat.format(value.birthday));
      sb.appendHtmlConstant("</div>");
    }

    /**
     * By convention, cells that respond to user events should handle the enter
     * key. This provides a consistent user experience when users use keyboard
     * navigation in the widget.
     */
    @Override
    protected void onEnterKeyDown(Context context, Element parent,
        Contact value, NativeEvent event, ValueUpdater<Contact> valueUpdater) {
      Window.alert("You clicked " + value.name);
    }
  }

  /**
   * The list of data to display.
   */
  private static final List<Contact> CONTACTS = Arrays.asList(new Contact(
      "John", new Date(80, 4, 12), "123 Fourth Avenue"), new Contact("Joe",
      new Date(85, 2, 22), "22 Lance Ln"), new Contact("Michael", new Date(80,
      1, 2), "1283 Berry Blvd"), new Contact("Sarah", new Date(67, 10, 28),
      "100 Hundred St."), new Contact("George", new Date(46, 6, 6),
      "1600 Pennsylvania Avenue"));

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
}