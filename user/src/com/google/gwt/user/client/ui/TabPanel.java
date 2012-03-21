/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.aria.client.Roles;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.HasBeforeSelectionHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;

import java.util.Iterator;

/**
 * A panel that represents a tabbed set of pages, each of which contains another
 * widget. Its child widgets are shown as the user selects the various tabs
 * associated with them. The tabs can contain arbitrary HTML.
 *
 * <p>
 * This widget will <em>only</em> work in quirks mode. If your application is in
 * Standards Mode, use {@link TabLayoutPanel} instead.
 * </p>
 *
 * <p>
 * <img class='gallery' src='doc-files/TabPanel.png'/>
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
 * <li>.gwt-TabPanelBottom { the bottom section of the tab panel
 * (the deck containing the widget) }</li>
 * </ul>
 *
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.TabPanelExample}
 * </p>
 *
 * @see TabLayoutPanel
 */

// Cannot do anything about tab panel implementing TabListener until next
// release
@SuppressWarnings("deprecation")
public class TabPanel extends Composite implements TabListener,
    SourcesTabEvents, HasWidgets, HasAnimation, IndexedPanel.ForIsWidget,
    HasBeforeSelectionHandlers<Integer>, HasSelectionHandlers<Integer> {
  /**
   * This extension of DeckPanel overrides the public mutator methods to prevent
   * external callers from adding to the state of the DeckPanel.
   * <p>
   * Removal of Widgets is supported so that WidgetCollection.WidgetIterator
   * operates as expected.
   * </p>
   * <p>
   * We ensure that the DeckPanel cannot become of of sync with its associated
   * TabBar by delegating all mutations to the TabBar to this implementation of
   * DeckPanel.
   * </p>
   */
  private static class TabbedDeckPanel extends DeckPanel {
    private final UnmodifiableTabBar tabBar;

    public TabbedDeckPanel(UnmodifiableTabBar tabBar) {
      this.tabBar = tabBar;
    }

    @Override
    public void add(Widget w) {
      throw new UnsupportedOperationException(
          "Use TabPanel.add() to alter the DeckPanel");
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException(
          "Use TabPanel.clear() to alter the DeckPanel");
    }

    @Override
    public void insert(Widget w, int beforeIndex) {
      throw new UnsupportedOperationException(
          "Use TabPanel.insert() to alter the DeckPanel");
    }

    @Override
    public boolean remove(Widget w) {
      // Removal of items from the TabBar is delegated to the DeckPanel
      // to ensure consistency
      int idx = getWidgetIndex(w);
      if (idx != -1) {
        tabBar.removeTabProtected(idx);
        return super.remove(w);
      }

      return false;
    }

    protected void insertProtected(Widget w, String tabText, boolean asHTML,
        int beforeIndex) {

      // Check to see if the TabPanel already contains the Widget. If so,
      // remove it and see if we need to shift the position to the left.
      int idx = getWidgetIndex(w);
      if (idx != -1) {
        remove(w);
        if (idx < beforeIndex) {
          beforeIndex--;
        }
      }

      tabBar.insertTabProtected(tabText, asHTML, beforeIndex);
      super.insert(w, beforeIndex);
    }

    protected void insertProtected(Widget w, Widget tabWidget, int beforeIndex) {

      // Check to see if the TabPanel already contains the Widget. If so,
      // remove it and see if we need to shift the position to the left.
      int idx = getWidgetIndex(w);
      if (idx != -1) {
        remove(w);
        if (idx < beforeIndex) {
          beforeIndex--;
        }
      }

      tabBar.insertTabProtected(tabWidget, beforeIndex);
      super.insert(w, beforeIndex);
    }
  }

  /**
   * This extension of TabPanel overrides the public mutator methods to prevent
   * external callers from modifying the state of the TabBar.
   */
  private class UnmodifiableTabBar extends TabBar {
    @Override
    public void insertTab(String text, boolean asHTML, int beforeIndex) {
      throw new UnsupportedOperationException(
          "Use TabPanel.insert() to alter the TabBar");
    }

    @Override
    public void insertTab(Widget widget, int beforeIndex) {
      throw new UnsupportedOperationException(
          "Use TabPanel.insert() to alter the TabBar");
    }

    public void insertTabProtected(String text, boolean asHTML, int beforeIndex) {
      super.insertTab(text, asHTML, beforeIndex);
    }

    public void insertTabProtected(Widget widget, int beforeIndex) {
      super.insertTab(widget, beforeIndex);
    }

    @Override
    public void removeTab(int index) {
      // It's possible for removeTab() to function correctly, but it's
      // preferable to have only TabbedDeckPanel.remove() be operable,
      // especially since TabBar does not export an Iterator over its values.
      throw new UnsupportedOperationException(
          "Use TabPanel.remove() to alter the TabBar");
    }

    public void removeTabProtected(int index) {
      super.removeTab(index);
    }

    @Override
    protected SimplePanel createTabTextWrapper() {
      return TabPanel.this.createTabTextWrapper();
    }
  }

  private final UnmodifiableTabBar tabBar = new UnmodifiableTabBar();
  private final TabbedDeckPanel deck = new TabbedDeckPanel(tabBar);

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
    // Add a11y role "tabpanel"
    Roles.getTabpanelRole().set(deck.getElement());
  }

  /**
   * Convenience overload to allow {@link IsWidget} to be used directly.
   */
  public void add(IsWidget w, IsWidget tabWidget) {
    add(asWidgetOrNull(w), asWidgetOrNull(tabWidget));
  }

  /**
   * Convenience overload to allow {@link IsWidget} to be used directly.
   */
  public void add(IsWidget w, String tabText) {
    add(asWidgetOrNull(w), tabText);
  }

  /**
   * Convenience overload to allow {@link IsWidget} to be used directly.
   */
  public void add(IsWidget w, String tabText, boolean asHTML) {
    add(asWidgetOrNull(w), tabText, asHTML);
  }

  @Override
  public void add(Widget w) {
    throw new UnsupportedOperationException(
        "A tabText parameter must be specified with add().");
  }

  /**
   * Adds a widget to the tab panel. If the Widget is already attached to the
   * TabPanel, it will be moved to the right-most index.
   *
   * @param w the widget to be added
   * @param tabText the text to be shown on its tab
   */
  public void add(Widget w, String tabText) {
    insert(w, tabText, getWidgetCount());
  }

  /**
   * Adds a widget to the tab panel. If the Widget is already attached to the
   * TabPanel, it will be moved to the right-most index.
   *
   * @param w the widget to be added
   * @param tabText the text to be shown on its tab
   * @param asHTML <code>true</code> to treat the specified text as HTML
   */
  public void add(Widget w, String tabText, boolean asHTML) {
    insert(w, tabText, asHTML, getWidgetCount());
  }

  /**
   * Adds a widget to the tab panel. If the Widget is already attached to the
   * TabPanel, it will be moved to the right-most index.
   *
   * @param w the widget to be added
   * @param tabWidget the widget to be shown in the tab
   */
  public void add(Widget w, Widget tabWidget) {
    insert(w, tabWidget, getWidgetCount());
  }

  @Override
  public HandlerRegistration addBeforeSelectionHandler(
      BeforeSelectionHandler<Integer> handler) {
    return addHandler(handler, BeforeSelectionEvent.getType());
  }

  @Override
  public HandlerRegistration addSelectionHandler(
      SelectionHandler<Integer> handler) {
    return addHandler(handler, SelectionEvent.getType());
  }

  /**
   * @deprecated Use {@link #addBeforeSelectionHandler} and {@link
   * #addSelectionHandler} instead
   */
  @Override
  @Deprecated
  public void addTabListener(TabListener listener) {
    ListenerWrapper.WrappedTabListener.add(this, listener);
  }

  @Override
  public void clear() {
    while (getWidgetCount() > 0) {
      remove(getWidget(0));
    }
  }

  /**
   * Gets the deck panel within this tab panel. Adding or removing Widgets from
   * the DeckPanel is not supported and will throw
   * UnsupportedOperationExceptions.
   *
   * @return the deck panel
   */
  public DeckPanel getDeckPanel() {
    return deck;
  }

  /**
   * Gets the tab bar within this tab panel. Adding or removing tabs from from
   * the TabBar is not supported and will throw UnsupportedOperationExceptions.
   *
   * @return the tab bar
   */
  public TabBar getTabBar() {
    return tabBar;
  }

  @Override
  public Widget getWidget(int index) {
    return deck.getWidget(index);
  }

  @Override
  public int getWidgetCount() {
    return deck.getWidgetCount();
  }

  /**
   * Convenience overload to allow {@link IsWidget} to be used directly.
   */
  @Override
  public int getWidgetIndex(IsWidget child) {
    return getWidgetIndex(asWidgetOrNull(child));
  }

  @Override
  public int getWidgetIndex(Widget widget) {
    return deck.getWidgetIndex(widget);
  }

  /**
   * Convenience overload to allow {@link IsWidget} to be used directly.
   */
  public void insert(IsWidget widget, IsWidget tabWidget, int beforeIndex) {
    insert(asWidgetOrNull(widget), asWidgetOrNull(tabWidget), beforeIndex);
  }

  /**
   * Convenience overload to allow {@link IsWidget} to be used directly.
   */
  public void insert(IsWidget widget, String tabText, boolean asHTML,
      int beforeIndex) {
    insert(asWidgetOrNull(widget), tabText, asHTML, beforeIndex);
  }

  /**
   * Convenience overload to allow {@link IsWidget} to be used directly.
   */
  public void insert(IsWidget widget, String tabText, int beforeIndex) {
    insert(asWidgetOrNull(widget), tabText, beforeIndex);
  }

  /**
   * Inserts a widget into the tab panel. If the Widget is already attached to
   * the TabPanel, it will be moved to the requested index.
   *
   * @param widget the widget to be inserted
   * @param tabText the text to be shown on its tab
   * @param asHTML <code>true</code> to treat the specified text as HTML
   * @param beforeIndex the index before which it will be inserted
   */
  public void insert(Widget widget, String tabText, boolean asHTML,
      int beforeIndex) {
    // Delegate updates to the TabBar to our DeckPanel implementation
    deck.insertProtected(widget, tabText, asHTML, beforeIndex);
  }

  /**
   * Inserts a widget into the tab panel. If the Widget is already attached to
   * the TabPanel, it will be moved to the requested index.
   *
   * @param widget the widget to be inserted
   * @param tabText the text to be shown on its tab
   * @param beforeIndex the index before which it will be inserted
   */
  public void insert(Widget widget, String tabText, int beforeIndex) {
    insert(widget, tabText, false, beforeIndex);
  }

  /**
   * Inserts a widget into the tab panel. If the Widget is already attached to
   * the TabPanel, it will be moved to the requested index.
   *
   * @param widget the widget to be inserted.
   * @param tabWidget the widget to be shown on its tab.
   * @param beforeIndex the index before which it will be inserted.
   */
  public void insert(Widget widget, Widget tabWidget, int beforeIndex) {
    // Delegate updates to the TabBar to our DeckPanel implementation
    deck.insertProtected(widget, tabWidget, beforeIndex);
  }

  @Override
  public boolean isAnimationEnabled() {
    return deck.isAnimationEnabled();
  }

  @Override
  public Iterator<Widget> iterator() {
    // The Iterator returned by DeckPanel supports removal and will invoke
    // TabbedDeckPanel.remove(), which is an active function.
    return deck.iterator();
  }

  /**
   * @deprecated Use {@link BeforeSelectionHandler#onBeforeSelection} instead
   */
  @Override
  @Deprecated
  public boolean onBeforeTabSelected(SourcesTabEvents sender, int tabIndex) {
    BeforeSelectionEvent<Integer> event = BeforeSelectionEvent.fire(this, tabIndex);
    return event == null || !event.isCanceled();
  }

  /**
   * @deprecated Use {@link SelectionHandler#onSelection} instead
   */
  @Override
  @Deprecated
  public void onTabSelected(SourcesTabEvents sender, int tabIndex) {
    deck.showWidget(tabIndex);
    SelectionEvent.fire(this, tabIndex);
  }

  @Override
  public boolean remove(int index) {
    // Delegate updates to the TabBar to our DeckPanel implementation
    return deck.remove(index);
  }

  /**
   * Removes the given widget, and its associated tab.
   *
   * @param widget the widget to be removed
   */
  @Override
  public boolean remove(Widget widget) {
    // Delegate updates to the TabBar to our DeckPanel implementation
    return deck.remove(widget);
  }

  /**
   * @deprecated Use the {@link HandlerRegistration#removeHandler}
   * method on the object returned by and add*Handler method instead
   */
  @Override
  @Deprecated
  public void removeTabListener(TabListener listener) {
    ListenerWrapper.WrappedTabListener.remove(this, listener);
  }

  /**
   * Programmatically selects the specified tab and fires events.
   *
   * @param index the index of the tab to be selected
   */
  public void selectTab(int index) {
    selectTab(index, true);
  }

  /**
   * Programmatically selects the specified tab.
   *
   * @param index the index of the tab to be selected
   * @param fireEvents true to fire events, false not to
   */
  public void selectTab(int index, boolean fireEvents) {
    tabBar.selectTab(index, fireEvents);
  }

  @Override
  public void setAnimationEnabled(boolean enable) {
    deck.setAnimationEnabled(enable);
  }

  /**
   * Create a {@link SimplePanel} that will wrap the contents in a tab.
   * Subclasses can use this method to wrap tabs in decorator panels.
   *
   * @return a {@link SimplePanel} to wrap the tab contents, or null to leave
   *         tabs unwrapped
   */
  protected SimplePanel createTabTextWrapper() {
    return null;
  }

  /**
   * <b>Affected Elements:</b>
   * <ul>
   * <li>-bar = The tab bar.</li>
   * <li>-bar-tab# = The element containing the content of the tab itself.</li>
   * <li>-bar-tab-wrapper# = The cell containing the tab at the index.</li>
   * <li>-bottom = The panel beneath the tab bar.</li>
   * </ul>
   *
   * @see UIObject#onEnsureDebugId(String)
   */
  @Override
  protected void onEnsureDebugId(String baseID) {
    super.onEnsureDebugId(baseID);
    tabBar.ensureDebugId(baseID + "-bar");
    deck.ensureDebugId(baseID + "-bottom");
  }
}
