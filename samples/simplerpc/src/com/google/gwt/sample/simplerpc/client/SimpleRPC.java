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
package com.google.gwt.sample.simplerpc.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Demonstrates a simple use of the RPC mechanism.
 */
public class SimpleRPC implements EntryPoint {

  public void onModuleLoad() {
    final Panel root = RootPanel.get();

    // Create the RPC client.
    SimpleRPCServiceAsync simpleRPCService = createSimpleRPCServiceAsync();

    // Collection of very simple RPC calls to "getString".
    callGetString(root, simpleRPCService);

    // A single simple call to "getMultipleStrings".
    callGetMultipleStrings(root, simpleRPCService);
  }

  /**
   * Creates a single call to <code>getMultipleStrings</code>.
   */
  private void callGetMultipleStrings(final Panel root,
      SimpleRPCServiceAsync simpleRPCService) {
    AsyncCallback<Map<Integer, String>> getMultipleStringsCallback = createGetMultipleStringsCallback(root);

    // Should print a table of key value pairs.
    List<Integer> indexes = new ArrayList<Integer>();
    indexes.add(Integer.valueOf(0));
    indexes.add(Integer.valueOf(2));
    simpleRPCService.getMultipleStrings(indexes, getMultipleStringsCallback);
  }

  /**
   * Calls <code>getString</code> three times, the first two should return
   * valid answers, the third should give back an error. <p/> Control flow will
   * continue after making each call. Later the 'callback' onSuccess or
   * onFailure method will be invoked when the RPC completes. There is no order
   * guarantee here, the three results could appear on the page in any order.
   */
  private void callGetString(final Panel root,
      SimpleRPCServiceAsync simpleRPCService) {
    // Create a callback to use.
    AsyncCallback<String> singleGetStringCallback = createGetStringCallback(root);

    // Should print 'Hello World'.
    simpleRPCService.getString(0, singleGetStringCallback);

    // Should print 'Bonjour monde'.
    simpleRPCService.getString(1, singleGetStringCallback);

    // Should print an IndexOutOfBoundsException.
    simpleRPCService.getString(3, singleGetStringCallback);
  }

  /**
   * Create an asynchronous callback for the <code>getMultipleStrings</code>
   * RPC call. The same callback can be used for many RPC calls or customized
   * for a single one.
   */
  private AsyncCallback<Map<Integer, String>> createGetMultipleStringsCallback(
      final Panel root) {
    return new AsyncCallback<Map<Integer, String>>() {

      public void onFailure(Throwable caught) {
        Window.alert("error: " + caught);
      }

      public void onSuccess(Map<Integer, String> result) {
        FlexTable t = new FlexTable();
        t.setBorderWidth(2);
        t.setHTML(0, 0, "<b>Map Key</b>");
        t.setHTML(0, 1, "<b>Map Value</b>");
        int index = 1;
        for (Entry<Integer, String> element : result.entrySet()) {
          Integer key = element.getKey();
          String value = element.getValue();
          t.setText(index, 0, key.toString());
          t.setText(index, 1, value);
          ++index;
        }
        root.add(new HTML("<h3>Result(on success)</h3>"));
        root.add(t);
      }
    };
  }

  /**
   * Create an asynchronous callback for the <code>getString</code> RPC call.
   * The same callback can be used for many RPC calls or customized for a single
   * one.
   */
  private AsyncCallback<String> createGetStringCallback(final Panel root) {
    return new AsyncCallback<String>() {
      public void onFailure(Throwable caught) {
        root.add(new HTML("<h3>Result (on failure) </h3>"));
        root.add(new HTML("<i>" + caught.getMessage() + "</i>"));
      }

      public void onSuccess(String result) {
        root.add(new HTML("<h3>Result (on success) </h3>" + result));
      }
    };
  }

  /**
   * Returns an configured instance of the <code>SimpleRPCService</code>
   * client proxy. <p/> Note that although you are creating the service
   * interface proper, you cast the result to the asynchronous version of the
   * interface. The cast is always safe because the generated proxy implements
   * the asynchronous interface automatically.
   */
  private SimpleRPCServiceAsync createSimpleRPCServiceAsync() {
    return GWT.<SimpleRPCServiceAsync> create(SimpleRPCService.class);
  }
}
