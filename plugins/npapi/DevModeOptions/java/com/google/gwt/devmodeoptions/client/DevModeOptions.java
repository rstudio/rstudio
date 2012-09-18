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
package com.google.gwt.devmodeoptions.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * The options page for configuring the set of hosts permitted to use the GWT
 * Developer Plugin.
 */
public class DevModeOptions implements EntryPoint {

  interface Binder extends UiBinder<Widget, DevModeOptions> {
  }

  private static final DevModeOptionsResources bundle = GWT.create(DevModeOptionsResources.class);

  @UiField
  Button addBtn;

  @UiField
  Label errorMessage;

  @UiField
  TextBox hostname;

  @UiField
  TextBox codeserver;

  JsArray<HostEntry> hosts;

  @UiField
  RadioButton includeNo;

  @UiField
  RadioButton includeYes;

  @UiField
  FlexTable savedHosts;

  public void onModuleLoad() {
    StyleInjector.inject(bundle.css().getText(), true);
    RootLayoutPanel.get().add(
        GWT.<Binder> create(Binder.class).createAndBindUi(this));

    hosts = HostEntryStorage.get().getHostEntries();

    addBtn.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        addHost(HostEntry.create(hostname.getText() + "/"
            + getCodeServer(codeserver),
            includeYes.getValue()));
      }
    });

    hostname.setFocus(true);
    String host = Location.getParameter("host");
    if (host != null) {
      hostname.setText(host);
    }

    String code = Location.getParameter("codeserver");
    if (code != null) {
      codeserver.setText(code);
    } else {
      //default for users entering through options
      codeserver.setText("localhost");
    }

    hostname.addKeyPressHandler(new KeyPressHandler() {
      public void onKeyPress(KeyPressEvent event) {
        if (event.getCharCode() == KeyCodes.KEY_ENTER) {
        addHost(HostEntry.create(hostname.getText() + "/"
            + getCodeServer(codeserver),
            includeYes.getValue()));
        }
      }
    });

    codeserver.addKeyPressHandler(new KeyPressHandler() {
      public void onKeyPress(KeyPressEvent event) {
        if (event.getCharCode() == KeyCodes.KEY_ENTER) {
        addHost(HostEntry.create(hostname.getText() + "/"
            + getCodeServer(codeserver),
            includeYes.getValue()));
        }
      }
    });

    savedHosts.setText(0, 0, "Web server");
    savedHosts.setText(0, 1, "Code server");
    savedHosts.setText(0, 2, "Include/Exclude");
    savedHosts.setText(0, 3, "Remove");
    savedHosts.getCellFormatter().addStyleName(0, 0,
        bundle.css().savedHostsHeading());
    savedHosts.getCellFormatter().addStyleName(0, 0, bundle.css().textCol());

    savedHosts.getCellFormatter().addStyleName(0, 1,
        bundle.css().savedHostsHeading());
    savedHosts.getCellFormatter().addStyleName(0, 1, bundle.css().textCol());

    savedHosts.getCellFormatter().addStyleName(0, 2,
        bundle.css().savedHostsHeading());

    savedHosts.getCellFormatter().addStyleName(0, 3,
        bundle.css().savedHostsHeading());

    for (int i = 0; i < hosts.length(); i++) {
      displayHost(hosts.get(i));
    }
  }

  private void addHost(final HostEntry newHost) {
    if (newHost.getUrl().length() == 0) {
      return;
    }

    boolean alreadyExists = false;
    for (int i = 0; i < hosts.length() && !alreadyExists; i++) {
      if (hosts.get(i).isEqual(newHost)) {
        alreadyExists = true;
      }
    }

    if (alreadyExists) {
      error("Cannot add duplicate host entry for " + newHost.getUrl());
      return;
    } else {
      hosts.push(newHost);
      clearError();
    }
    HostEntryStorage.get().saveEntries(hosts);

    displayHost(newHost);

    codeserver.setText("");
    hostname.setText("");
    hostname.setFocus(true);
  }

  private void clearError() {
    errorMessage.setText("");
  }

  private void displayHost(final HostEntry newHost) {
    int numRows = savedHosts.getRowCount();
    int col = 0;
    
    String[] names = newHost.getUrl().split("/");
   
    savedHosts.insertRow(numRows);
    savedHosts.setText(numRows, col++, names[0]);
    savedHosts.setText(numRows, col++, names.length > 1 ? names[1] 
        : "localhost");
    savedHosts.setText(numRows, col++, newHost.include() ? "Include"
        : "Exclude");
    if (newHost.include()) {
      savedHosts.getCellFormatter().addStyleName(numRows, 0,
          bundle.css().include());
      savedHosts.getCellFormatter().addStyleName(numRows, 1,
          bundle.css().include());
      savedHosts.getCellFormatter().addStyleName(numRows, 2,
          bundle.css().include());
    } else {
      savedHosts.getCellFormatter().addStyleName(numRows, 0,
          bundle.css().exclude());
      savedHosts.getCellFormatter().addStyleName(numRows, 1,
          bundle.css().exclude());
      savedHosts.getCellFormatter().addStyleName(numRows, 2,
          bundle.css().exclude());
    }

    Button removeHostButton = new Button("x");
    removeHostButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        removeHost(newHost);
      }
    });
    savedHosts.setWidget(numRows, col, removeHostButton);
  }

  private void error(String text) {
    errorMessage.setText(text);
  }

  private void removeHost(HostEntry host) {
    JsArray<HostEntry> newHosts = JavaScriptObject.createArray().cast();
    for (int index = 0; index < hosts.length(); index++) {
      if (hosts.get(index).isEqual(host)) {
        savedHosts.removeRow(index + 1);
      } else {
        newHosts.push(hosts.get(index));
      }
    }

    hosts = newHosts;
    HostEntryStorage.get().saveEntries(hosts);
  }

  private String getCodeServer(TextBox box) {
    return (box.getText().length() > 0) ? box.getText() : "localhost";
  }

}
