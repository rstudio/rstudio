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
package com.google.gwt.sample.json.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONException;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

import java.util.Set;

/**
 * Class that acts as a client to a JSON service. Currently, this client just
 * requests a text which contains a JSON encoding of a search result set from
 * yahoo. We use a text file to demonstrate how the pieces work without tripping
 * on cross-site scripting issues.
 * 
 * If you would like to make this a more dynamic example, you can associate a
 * servlet with this example and simply have it hit the yahoo service and return
 * the results.
 */
public class JSON {
  /**
   * Class for handling the response text associated with a request for a JSON
   * object.
   */
  private class JSONResponseTextHandler implements RequestCallback {
    public void onError(Request request, Throwable exception) {
      displayRequestError(exception.toString());
      resetSearchButtonCaption();
    }

    public void onResponseReceived(Request request, Response response) {
      String responseText = response.getText();
      try {
        JSONValue jsonValue = JSONParser.parse(responseText);
        displayJSONObject(jsonValue);
      } catch (JSONException e) {
        displayParseError(responseText);
      }
      resetSearchButtonCaption();
    }
  }

  /*
   * Class for handling the fetch button's click event.
   */
  private class SearchButtonHandler implements ClickHandler {
    public void onClick(ClickEvent event) {
      jsonTree.setVisible(false);
      doFetchURL();
    }
  }

  /*
   * Default URL to use to fetch JSON objects. Note that the contents of this
   * JSON result were as a result of requesting the following URL:
   * 
   * http://api.search.yahoo.com/ImageSearchService/V1/imageSearch?appid=YahooDemo
   * &query=potato&results=2&output=json
   */
  private static final String DEFAULT_SEARCH_URL = GWT.getModuleBaseURL()
      + "search-results.js";

  /*
   * Text displayed on the fetch button when we are in a default state.
   */
  private static final String SEARCH_BUTTON_DEFAULT_TEXT = "Search";

  /*
   * Text displayed on the fetch button when we are waiting for a JSON reply.
   */
  private static final String SEARCH_BUTTON_WAITING_TEXT = "Waiting for JSON Response...";

  private Tree jsonTree = new Tree();

  /*
   * RequestBuilder used to issue HTTP GET requests.
   */
  private final RequestBuilder requestBuilder = new RequestBuilder(
      RequestBuilder.GET, DEFAULT_SEARCH_URL);

  private Button searchButton = new Button();

  /**
   * Entry point for this simple application. Currently, we build the
   * application's form and wait for events.
   */
  public void onModuleLoad() {
    initializeMainForm();
  }

  /*
   * Add the object presented by the JSONValue as a children to the requested
   * TreeItem.
   */
  private void addChildren(TreeItem treeItem, JSONValue jsonValue) {
    JSONArray jsonArray;
    JSONObject jsonObject;
    JSONString jsonString;

    if ((jsonArray = jsonValue.isArray()) != null) {
      for (int i = 0; i < jsonArray.size(); ++i) {
        TreeItem child = treeItem.addItem(getChildText("["
            + Integer.toString(i) + "]"));
        addChildren(child, jsonArray.get(i));
      }
    } else if ((jsonObject = jsonValue.isObject()) != null) {
      Set<String> keys = jsonObject.keySet();
      for (String key : keys) {
        TreeItem child = treeItem.addItem(getChildText(key));
        addChildren(child, jsonObject.get(key));
      }
    } else if ((jsonString = jsonValue.isString()) != null) {
      // Use stringValue instead of toString() because we don't want escaping
      treeItem.addItem(jsonString.stringValue());
    } else {
      // JSONBoolean, JSONNumber, and JSONNull work well with toString().
      treeItem.addItem(getChildText(jsonValue.toString()));
    }
  }

  private void displayError(String errorType, String errorMessage) {
    jsonTree.removeItems();
    jsonTree.setVisible(true);
    TreeItem treeItem = jsonTree.addItem(errorType);
    treeItem.addItem(errorMessage);
    treeItem.setStyleName("JSON-JSONResponseObject");
    treeItem.setState(true);
  }

  /*
   * Update the treeview of a JSON object.
   */
  private void displayJSONObject(JSONValue jsonValue) {
    jsonTree.removeItems();
    jsonTree.setVisible(true);
    TreeItem treeItem = jsonTree.addItem("JSON Response");
    addChildren(treeItem, jsonValue);
    treeItem.setStyleName("JSON-JSONResponseObject");
    treeItem.setState(true);
  }

  private void displayParseError(String responseText) {
    displayError("Failed to parse JSON response", responseText);
  }

  private void displayRequestError(String message) {
    displayError("Request failed.", message);
  }

  private void displaySendError(String message) {
    displayError("Failed to send the request.", message);
  }

  /*
   * Fetch the requested URL.
   */
  private void doFetchURL() {
    searchButton.setText(SEARCH_BUTTON_WAITING_TEXT);
    try {
      requestBuilder.sendRequest(null, new JSONResponseTextHandler());
    } catch (RequestException ex) {
      displaySendError(ex.toString());
      resetSearchButtonCaption();
    }
  }

  /*
   * Causes the text of child elements to wrap.
   */
  private String getChildText(String text) {
    return "<span style='white-space:normal'>" + text + "</span>";
  }

  /**
   * Initialize the main form's layout and content.
   */
  private void initializeMainForm() {
    searchButton.setStyleName("JSON-SearchButton");
    searchButton.setText(SEARCH_BUTTON_DEFAULT_TEXT);
    searchButton.addClickHandler(new SearchButtonHandler());

    // Avoids showing an "empty" cell
    jsonTree.setVisible(false);

    // Find out where the host page wants the button.
    //
    RootPanel searchButtonSlot = RootPanel.get("search");
    if (searchButtonSlot == null) {
      Window.alert("Please define a container element whose id is 'search'");
      return;
    }

    // Find out where the host page wants the tree view.
    //
    RootPanel treeViewSlot = RootPanel.get("tree");
    if (treeViewSlot == null) {
      Window.alert("Please define a container element whose id is 'tree'");
      return;
    }

    // Add both widgets.
    //
    searchButtonSlot.add(searchButton);
    treeViewSlot.add(jsonTree);
  }

  private void resetSearchButtonCaption() {
    searchButton.setText(SEARCH_BUTTON_DEFAULT_TEXT);
  }
}
