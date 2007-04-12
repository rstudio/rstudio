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
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Demonstrates {@link com.google.gwt.user.client.ui.ListBox}.
 */
public class Lists extends Sink implements ChangeListener {

  private static final String[][] stringLists = new String[][] {
      new String[] {"foo0", "bar0", "baz0", "toto0", "tintin0"},
      new String[] {"foo1", "bar1", "baz1", "toto1", "tintin1"},
      new String[] {"foo2", "bar2", "baz2", "toto2", "tintin2"},
      new String[] {"foo3", "bar3", "baz3", "toto3", "tintin3"},
      new String[] {"foo4", "bar4", "baz4", "toto4", "tintin4"},};

  private static final String[] words = new String[] {
      "1337", "apple", "about", "ant", "bruce", "banana", "bobv", "canada",
      "coconut", "compiler", "donut", "deferred binding", "dessert topping",
      "eclair", "ecc", "frog attack", "floor wax", "fitz", "google", "gosh",
      "gwt", "hollis", "haskell", "hammer", "in the flinks", "internets",
      "ipso facto", "jat", "jgw", "java", "jens", "knorton", "kaitlyn",
      "kangaroo", "la grange", "lars", "love", "morrildl", "max", "maddie",
      "mloofle", "mmendez", "nail", "narnia", "null", "optimizations",
      "obfuscation", "original", "ping pong", "polymorphic", "pleather",
      "quotidian", "quality", "qu'est-ce que c'est", "ready state", "ruby",
      "rdayal", "subversion", "superclass", "scottb", "tobyr", "the dans",
      "~ tilde", "undefined", "unit tests", "under 100ms", "vtbl", "vidalia",
      "vector graphics", "w3c", "web experience", "work around", "w00t!",
      "xml", "xargs", "xeno", "yacc", "yank (the vi command)", "zealot", "zoe",
      "zebra"};

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
  private MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
  private SuggestBox suggestBox = new SuggestBox(oracle);

  public Lists() {
    combo.setVisibleItemCount(1);
    combo.addChangeListener(this);
    list.setVisibleItemCount(10);
    list.setMultipleSelect(true);

    for (int i = 0; i < stringLists.length; ++i) {
      combo.addItem("List " + i);
    }
    combo.setSelectedIndex(0);
    fillList(0);

    list.addChangeListener(this);

    for (int i = 0; i < words.length; ++i) {
      oracle.add(words[i]);
    }

    VerticalPanel suggestPanel = new VerticalPanel();
    suggestPanel.add(new Label("Suggest box:"));
    suggestPanel.add(suggestBox);

    HorizontalPanel horz = new HorizontalPanel();
    horz.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);
    horz.setSpacing(8);
    horz.add(combo);
    horz.add(list);
    horz.add(suggestPanel);

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

  private void fillList(int idx) {
    // Set the contents of the list box to reflect the combo selection.
    list.clear();
    String[] strings = stringLists[idx];
    for (int i = 0; i < strings.length; ++i) {
      list.addItem(strings[i]);
    }

    echoSelection();
  }
}
