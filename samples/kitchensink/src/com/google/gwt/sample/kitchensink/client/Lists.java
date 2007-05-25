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
package com.google.gwt.sample.kitchensink.client;

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.TreeListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Demonstrates {@link com.google.gwt.user.client.ui.ListBox}.
 */
public class Lists extends Sink implements ChangeListener, TreeListener {

  private static class PendingItem extends TreeItem {
    public PendingItem() {
      super("Please wait...");
    }
  }

  private static class Proto {
    public Proto[] children;
    public TreeItem item;
    public String text;

    public Proto(String text) {
      this.text = text;
    }

    public Proto(String text, Proto[] children) {
      this(text);
      this.children = children;
    }
  }

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

  private static Proto[] fProto = new Proto[]{
    new Proto("Beethoven", new Proto[]{
      new Proto("Concertos", new Proto[]{
        new Proto("No. 1 - C"), new Proto("No. 2 - B-Flat Major"),
        new Proto("No. 3 - C Minor"), new Proto("No. 4 - G Major"),
        new Proto("No. 5 - E-Flat Major"),}),
      new Proto("Quartets", new Proto[]{
        new Proto("Six String Quartets"), new Proto("Three String Quartets"),
        new Proto("Grosse Fugue for String Quartets"),}),
      new Proto("Sonatas", new Proto[]{
        new Proto("Sonata in A Minor"), new Proto("Sonata in F Major"),}),
      new Proto("Symphonies", new Proto[]{
        new Proto("No. 1 - C Major"), new Proto("No. 2 - D Major"),
        new Proto("No. 3 - E-Flat Major"), new Proto("No. 4 - B-Flat Major"),
        new Proto("No. 5 - C Minor"), new Proto("No. 6 - F Major"),
        new Proto("No. 7 - A Major"), new Proto("No. 8 - F Major"),
        new Proto("No. 9 - D Minor"),}),}),
    new Proto("Brahms", new Proto[]{
      new Proto("Concertos", new Proto[]{
        new Proto("Violin Concerto"), new Proto("Double Concerto - A Minor"),
        new Proto("Piano Concerto No. 1 - D Minor"),
        new Proto("Piano Concerto No. 2 - B-Flat Major"),}),
      new Proto("Quartets", new Proto[]{
        new Proto("Piano Quartet No. 1 - G Minor"),
        new Proto("Piano Quartet No. 2 - A Major"),
        new Proto("Piano Quartet No. 3 - C Minor"),
        new Proto("String Quartet No. 3 - B-Flat Minor"),}),
      new Proto("Sonatas", new Proto[]{
        new Proto("Two Sonatas for Clarinet - F Minor"),
        new Proto("Two Sonatas for Clarinet - E-Flat Major"),}),
      new Proto("Symphonies", new Proto[]{
        new Proto("No. 1 - C Minor"), new Proto("No. 2 - D Minor"),
        new Proto("No. 3 - F Major"), new Proto("No. 4 - E Minor"),}),}),
    new Proto("Mozart", new Proto[]{new Proto("Concertos", new Proto[]{
      new Proto("Piano Concerto No. 12"), new Proto("Piano Concerto No. 17"),
      new Proto("Clarinet Concerto"), new Proto("Violin Concerto No. 5"),
      new Proto("Violin Concerto No. 4"),}),}),};

  public static SinkInfo init(final Sink.Images images) {
    return new SinkInfo("Lists",
        "<h2>Lists and Trees</h2>" +
        "<p>GWT provides a number of ways to display lists and trees. This " +
        "includes the browser's built-in list and drop-down boxes, as well as " +
        "the more advanced suggestion combo-box and trees.</p><p>Try typing " +
        "some text in the SuggestBox below to see what happens!</p>") {

      public Sink createInstance() {
        return new Lists(images);
      }
    };
  }
  private ListBox combo = new ListBox();
  private ListBox list = new ListBox();

  private MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();

  private SuggestBox suggestBox = new SuggestBox(oracle);

  private Tree tree;

  public Lists(Sink.Images images) {
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
    initWidget(panel);

    tree = new Tree(images);
    for (int i = 0; i < fProto.length; ++i) {
      createItem(fProto[i]);
      tree.addItem(fProto[i].item);
    }

    tree.addTreeListener(this);
    tree.setWidth("20em");
    horz.add(tree);
  }

  public void onChange(Widget sender) {
    if (sender == combo) {
      fillList(combo.getSelectedIndex());
    } else if (sender == list) {
    }
  }

  public void onShow() {
  }

  public void onTreeItemSelected(TreeItem item) {
  }

  public void onTreeItemStateChanged(TreeItem item) {
    TreeItem child = item.getChild(0);
    if (child instanceof PendingItem) {
      item.removeItem(child);

      Proto proto = (Proto) item.getUserObject();
      for (int i = 0; i < proto.children.length; ++i) {
        createItem(proto.children[i]);
        item.addItem(proto.children[i].item);
      }
    }
  }

  private void createItem(Proto proto) {
    proto.item = new TreeItem(proto.text);
    proto.item.setUserObject(proto);
    if (proto.children != null) {
      proto.item.addItem(new PendingItem());
    }
  }

  private void fillList(int idx) {
    // Set the contents of the list box to reflect the combo selection.
    list.clear();
    String[] strings = stringLists[idx];
    for (int i = 0; i < strings.length; ++i) {
      list.addItem(strings[i]);
    }
  }
}
