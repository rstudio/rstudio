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
package com.google.gwt.sample.mail.client;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Composite that represents a collection of <code>Task</code> items.
 */
public class Tasks extends Composite {

  public Tasks() {
    VerticalPanel list = new VerticalPanel();
    list.add(new CheckBox("Get groceries"));
    list.add(new CheckBox("Walk the dog"));
    list.add(new CheckBox("Start Web 2.0 company"));
    list.add(new CheckBox("Write cool app in GWT"));
    list.add(new CheckBox("Get funding"));
    list.add(new CheckBox("Take a vacation"));
    initWidget(list);
    setStyleName("mail-Tasks");
  }
}
