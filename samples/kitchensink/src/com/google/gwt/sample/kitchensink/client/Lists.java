/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.sample.kitchensink.client;

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Demonstrates {@link com.google.gwt.user.client.ui.ListBox}.
 */
public class Lists extends Sink implements ChangeListener {

  private static final String[][] sStrings = new String[][]{
    new String[]{"foo0", "bar0", "baz0", "toto0", "tintin0"},
    new String[]{"foo1", "bar1", "baz1", "toto1", "tintin1"},
    new String[]{"foo2", "bar2", "baz2", "toto2", "tintin2"},
    new String[]{"foo3", "bar3", "baz3", "toto3", "tintin3"},
    new String[]{"foo4", "bar4", "baz4", "toto4", "tintin4"},};

  public static SinkInfo init() {
    return new SinkInfo("Lists",
      "Here is the ListBox widget in its two major forms.") {
      public Sink createInstance() {
        return new Lists();
      }
    };
  }

  private ListBox combo = new ListBox();
  private ListBox list = new ListBox();
  private Label echo = new Label();

  public Lists() {
    combo.setVisibleItemCount(1);
    combo.addChangeListener(this);
    list.setVisibleItemCount(10);
    list.setMultipleSelect(true);

    for (int i = 0; i < sStrings.length; ++i) {
      combo.addItem("List " + i);
    }
    combo.setSelectedIndex(0);
    fillList(0);

    list.addChangeListener(this);

    HorizontalPanel horz = new HorizontalPanel();
    horz.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);
    horz.setSpacing(8);
    horz.add(combo);
    horz.add(list);

    VerticalPanel panel = new VerticalPanel();
    panel.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
    panel.add(horz);
    panel.add(echo);
    initWidget(panel);

    echoSelection();
  }

  public void onChange(Widget sender) {
    if (sender == combo) {
      fillList(combo.getSelectedIndex());
    } else if (sender == list) {
      echoSelection();
    }
  }

  public void onShow() {
  }

  private void fillList(int idx) {
    // Set the contents of the list box to reflect the combo selection.
    list.clear();
    String[] strings = sStrings[idx];
    for (int i = 0; i < strings.length; ++i) {
      list.addItem(strings[i]);
    }

    echoSelection();
  }

  private void echoSelection() {
    // Determine which items are selected, and display them.
    String msg = "Selected items: ";
    for (int i = 0; i < list.getItemCount(); ++i) {
      if (list.isItemSelected(i)) {
        msg += list.getItemText(i) + " ";
      }
    }
    echo.setText(msg);
  }
}
