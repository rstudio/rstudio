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
package com.google.gwt.sample.simplexml.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;

/**
 * A very simple XML Example where we take a customer profile and display it on
 * a page.
 */
public class SimpleXML implements EntryPoint {
  private static final String XML_LABEL_STYLE = "xmlLabel";
  private static final String USER_TABLE_LABEL_STYLE = "userTableLabel";
  private static final String USER_TABLE_STYLE = "userTable";
  private static final String NOTES_STYLE = "notes";

  public void onModuleLoad() {
    RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET,
        "customerRecord.xml");

    try {
      requestBuilder.sendRequest(null, new RequestCallback() {
        public void onError(Request request, Throwable exception) {
          requestFailed(exception);
        }

        public void onResponseReceived(Request request, Response response) {
          renderXML(response.getText());
        }
      });
    } catch (RequestException ex) {
      requestFailed(ex);
    }
  }

  private FlexTable createOrderTable(FlowPanel xmlParsed, String label) {
    HTML orderTableLabel = new HTML("<h2>" + label + "</h2>");
    xmlParsed.add(orderTableLabel);
    FlexTable orderTable = new FlexTable();
    orderTable.setStyleName(USER_TABLE_STYLE);
    orderTable.setBorderWidth(3);
    orderTable.getRowFormatter().setStyleName(0, USER_TABLE_LABEL_STYLE);
    orderTable.setText(0, 0, "Order ID");
    orderTable.setText(0, 1, "Item");
    orderTable.setText(0, 2, "Ordered On");
    orderTable.setText(0, 3, "Street");
    orderTable.setText(0, 4, "City");
    orderTable.setText(0, 5, "State");
    orderTable.setText(0, 6, "Zip");
    xmlParsed.add(orderTable);
    return orderTable;
  }

  /**
   * Creates the xml representation of xmlText. xmlText is assumed to have been
   * validated for structure on the server.
   * 
   * @param xmlText xml text
   * @param xmlParsed panel to display customer record
   */
  private void customerPane(String xmlText, FlowPanel xmlParsed) {
    Document customerDom = XMLParser.parse(xmlText);
    Element customerElement = customerDom.getDocumentElement();
    // Must do this if you ever use a raw node list that you expect to be
    // all elements.
    XMLParser.removeWhitespace(customerElement);

    // Customer Name
    String nameValue = getElementTextValue(customerElement, "name");
    String title = "<h1>" + nameValue + "</h1>";
    HTML titleHTML = new HTML(title);
    xmlParsed.add(titleHTML);

    // Customer Notes
    String notesValue = getElementTextValue(customerElement, "notes");
    Label notesText = new Label();
    notesText.setStyleName(NOTES_STYLE);
    notesText.setText(notesValue);
    xmlParsed.add(notesText);

    // Pending orders UI setup
    FlexTable pendingTable = createOrderTable(xmlParsed, "Pending Orders");
    FlexTable completedTable = createOrderTable(xmlParsed, "Completed");
    completedTable.setText(0, 7, "Shipped by");

    // Fill Orders Table
    NodeList orders = customerElement.getElementsByTagName("order");
    int pendingRowPos = 0;
    int completedRowPos = 0;
    for (int i = 0; i < orders.getLength(); i++) {
      Element order = (Element) orders.item(i);
      HTMLTable table;
      int rowPos;
      if (order.getAttribute("status").equals("pending")) {
        table = pendingTable;
        rowPos = ++pendingRowPos;
      } else {
        table = completedTable;
        rowPos = ++completedRowPos;
      }
      int columnPos = 0;
      fillInOrderTableRow(customerElement, order, table, rowPos, columnPos);
    }
  }

  private void fillInOrderTableRow(Element customerElement, Element order,
      HTMLTable table, int rowPos, int columnPos) {
    // Order ID
    String orderId = order.getAttribute("id");
    table.setText(rowPos, columnPos++, orderId);

    // Item
    Element item = (Element) order.getElementsByTagName("item").item(0);
    String itemUPC = item.getAttribute("upc");
    String itemName = item.getFirstChild().getNodeValue();
    Label itemLabel = new Label(itemUPC);
    itemLabel.setTitle(itemName);
    table.setWidget(rowPos, columnPos++, itemLabel);

    // Ordered On
    String orderedOnValue = getElementTextValue(customerElement, "orderedOn");
    table.setText(rowPos, columnPos++, orderedOnValue);

    // Address
    Element address = (Element) order.getElementsByTagName("address").item(0);
    XMLParser.removeWhitespace(address);
    NodeList lst = address.getChildNodes();
    for (int j = 0; j < lst.getLength(); j++) {
      Element next = (Element) lst.item(j);
      String addressPartText = next.getFirstChild().getNodeValue();
      table.setText(rowPos, columnPos++, addressPartText);
    }

    // Shipped By (optional attribute)
    NodeList shippedByList = order.getElementsByTagName("shippingInfo");
    if (shippedByList.getLength() == 1) {
      Element shippedBy = (Element) shippedByList.item(0);
      // Depending upon the shipper, different attributes might be
      // available, so XML carries the display info
      FlexTable shippedByTable = new FlexTable();
      shippedByTable.getRowFormatter().setStyleName(0, USER_TABLE_LABEL_STYLE);
      shippedByTable.setBorderWidth(1);
      NodeList shippedByParts = shippedBy.getChildNodes();
      for (int j = 0; j < shippedByParts.getLength(); j++) {
        Node next = shippedByParts.item(j);
        Element elem = (Element) next;
        shippedByTable.setText(0, j, elem.getAttribute("title"));
        shippedByTable.setText(1, j, elem.getFirstChild().getNodeValue());
      }
      table.setWidget(rowPos, columnPos++, shippedByTable);
    }
  }

  /**
   * Utility method to return the values of elements of the form <myTag>tag
   * value</myTag>
   */
  private String getElementTextValue(Element parent, String elementTag) {
    // If the xml is not coming from a known good source, this method would
    // have to include safety checks.
    return parent.getElementsByTagName(elementTag).item(0).getFirstChild().getNodeValue();
  }

  private void renderXML(String xmlText) {
    final TabPanel tab = new TabPanel();
    final FlowPanel xmlSource = new FlowPanel();
    final FlowPanel xmlParsed = new FlowPanel();
    tab.add(xmlParsed, "Customer Pane");
    tab.add(xmlSource, "XML Source");
    tab.selectTab(0);
    RootPanel.get().add(tab);
    xmlPane(xmlText, xmlSource);
    customerPane(xmlText, xmlParsed);
  }

  private void requestFailed(Throwable exception) {
    Window.alert("Failed to send the request.  The error message was: "
        + exception.getMessage());
  }

  /**
   * Show the raw XML.
   * 
   * @param xmlText
   * @param xmlSource
   */
  private void xmlPane(String xmlText, final FlowPanel xmlSource) {
    xmlText = xmlText.replaceAll("<", "&#60;");
    xmlText = xmlText.replaceAll(">", "&#62;");
    Label xml = new HTML("<pre>" + xmlText + "</pre>", false);
    xml.setStyleName(XML_LABEL_STYLE);
    xmlSource.add(xml);
  }
}
