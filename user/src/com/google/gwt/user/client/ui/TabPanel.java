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
package com.google.gwt.user.client.ui;

import java.util.Iterator;

/**
 * A panel that represents a tabbed set of pages, each of which contains another
 * widget. Its child widgets are shown as the user selects the various tabs
 * associated with them. The tabs can contain arbitrary HTML.
 * 
 * <p>
 * <img class='gallery' src='TabPanel.png'/>
 * </p>
 * 
 * <p>
 * Note that this widget is not a panel per se, but rather a
 * {@link com.google.gwt.user.client.ui.Composite} that aggregates a
 * {@link com.google.gwt.user.client.ui.TabBar} and a
 * {@link com.google.gwt.user.client.ui.DeckPanel}. It does, however, implement
 * {@link com.google.gwt.user.client.ui.HasWidgets}.
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-TabPanel { the tab panel itself }</li>
 * <li>.gwt-TabPanelBottom { the bottom section of the tab panel (the deck
 * containing the widget) }</li>
 * </ul>
 * 
 * <p>
 * <h3>Example</h3> {@example com.google.gwt.examples.TabPanelExample}
 * </p>
 */
public class TabPanel extends Composite implements TabListener,
    SourcesTabEvents, HasWidgets, IndexedPanel {

  private WidgetCollection children = new WidgetCollection(this);
  private DeckPanel deck = new DeckPanel();
  private TabBar tabBar = new TabBar();
  private TabListenerCollection tabListeners;

  /**
   * Creates an empty tab panel.
   */
  public TabPanel() {
    VerticalPanel panel = new VerticalPanel();
    panel.add(tabBar);
    panel.add(deck);

    panel.setCellHeight(deck, "100%");
    tabBar.setWidth("100%");

    tabBar.addTabListener(this);
    initWidget(panel);
    setStyleName("gwt-TabPanel");
    deck.setStyleName("gwt-TabPanelBottom");
  }

  public void add(Widget w) {
    throw new UnsupportedOperationException(
      "A tabText parameter must be specified with add().");
  }

  /**
   * Adds a widget to the tab panel.
   * 
   * @param w the widget to be added
   * @param tabText the text to be shown on its tab
   */
  public void add(Widget w, String tabText) {
    insert(w, tabText, getWidgetCount());
  }

  /**
   * Adds a widget to the tab panel.
   * 
   * @param w the widget to be added
   * @param tabText the text to be shown on its tab
   * @param asHTML <code>true</code> to treat the specified text as HTML
   */
  public void add(Widget w, String tabText, boolean asHTML) {
    insert(w, tabText, asHTML, getWidgetCount());
  }

  public void addTabListener(TabListener listener) {
    if (tabListeners == null) {
      tabListeners = new TabListenerCollection();
    }
    tabListeners.add(listener);
  }

  public void clear() {
    while (getWidgetCount() > 0) {
      remove(getWidget(0));
    }
  }

  /**
   * Gets the deck panel within this tab panel.
   * 
   * @return the deck panel
   */
  public DeckPanel getDeckPanel() {
    return deck;
  }

  /**
   * Gets the tab bar within this tab panel.
   * 
   * @return the tab bar
   */
  public TabBar getTabBar() {
    return tabBar;
  }

  public Widget getWidget(int index) {
    return children.get(index);
  }

  public int getWidgetCount() {
    return children.size();
  }

  public int getWidgetIndex(Widget widget) {
    return children.indexOf(widget);
  }

  /**
   * Inserts a widget into the tab panel.
   * 
   * @param widget the widget to be inserted
   * @param tabText the text to be shown on its tab
   * @param asHTML <code>true</code> to treat the specified text as HTML
   * @param beforeIndex the index before which it will be inserted
   */
  public void insert(Widget widget, String tabText, boolean asHTML,
      int beforeIndex) {
    children.insert(widget, beforeIndex);
    tabBar.insertTab(tabText, asHTML, beforeIndex);
    deck.insert(widget, beforeIndex);
  }

  /**
   * Inserts a widget into the tab panel.
   * 
   * @param widget the widget to be inserted
   * @param tabText the text to be shown on its tab
   * @param beforeIndex the index before which it will be inserted
   */
  public void insert(Widget widget, String tabText, int beforeIndex) {
    insert(widget, tabText, false, beforeIndex);
  }

  public Iterator iterator() {
    return children.iterator();
  }

  public boolean onBeforeTabSelected(SourcesTabEvents sender, int tabIndex) {
    if (tabListeners != null) {
      return tabListeners.fireBeforeTabSelected(this, tabIndex);
    }
    return true;
  }

  public void onTabSelected(SourcesTabEvents sender, int tabIndex) {
    deck.showWidget(tabIndex);
    if (tabListeners != null) {
      tabListeners.fireTabSelected(this, tabIndex);
    }
  }

  public boolean remove(int index) {
    return remove(getWidget(index));
  }

  /**
   * Removes the given widget, and its associated tab.
   * 
   * @param widget the widget to be removed
   */
  public boolean remove(Widget widget) {
    int index = getWidgetIndex(widget);
    if (index == -1) {
      return false;
    }

    children.remove(widget);
    tabBar.removeTab(index);
    deck.remove(widget);
    return true;
  }

  public void removeTabListener(TabListener listener) {
    if (tabListeners != null) {
      tabListeners.remove(listener);
    }
  }

  /**
   * Programmatically selects the specified tab.
   * 
   * @param index the index of the tab to be selected
   */
  public void selectTab(int index) {
    tabBar.selectTab(index);
  }
}
