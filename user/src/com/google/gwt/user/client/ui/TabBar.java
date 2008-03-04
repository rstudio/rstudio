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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;

/**
 * A horizontal bar of folder-style tabs, most commonly used as part of a
 * {@link com.google.gwt.user.client.ui.TabPanel}.
 * <p>
 * <img class='gallery' src='TabBar.png'/>
 * </p>
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-TabBar { the tab bar itself }</li>
 * <li>.gwt-TabBar .gwt-TabBarFirst { the left edge of the bar }</li>
 * <li>.gwt-TabBar .gwt-TabBarRest { the right edge of the bar }</li>
 * <li>.gwt-TabBar .gwt-TabBarItem { unselected tabs }</li>
 * <li>.gwt-TabBar .gwt-TabBarItem-wrapper { table cell around tab }</li>
 * <li>.gwt-TabBar .gwt-TabBarItem-selected { additional style for selected
 * tabs } </li>
 * <li>.gwt-TabBar .gwt-TabBarItem-wrapper-selected { table cell around
 * selected tab }</li>
 * </ul>
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.TabBarExample}
 * </p>
 */
public class TabBar extends Composite implements SourcesTabEvents,
    ClickListener {

  /**
   * <code>ClickDecoratorPanel</code> decorates any widget with the minimal
   * amount of machinery to receive clicks for delegation to the parent.
   * {@link SourcesClickEvents} is not implemented due to the fact that only a
   * single observer is needed.
   */
  private static final class ClickDecoratorPanel extends SimplePanel {
    ClickListener delegate;

    ClickDecoratorPanel(Widget child, ClickListener delegate) {
      this.delegate = delegate;
      setWidget(child);
      sinkEvents(Event.ONCLICK);
    }

    @Override
    public void onBrowserEvent(Event event) {
      // No need for call to super.
      switch (DOM.eventGetType(event)) {
        case Event.ONCLICK:
          delegate.onClick(this);
      }
    }
  }

  private static final String STYLENAME_DEFAULT = "gwt-TabBarItem";
  private HorizontalPanel panel = new HorizontalPanel();
  private Widget selectedTab;
  private TabListenerCollection tabListeners;

  /**
   * Creates an empty tab bar.
   */
  public TabBar() {
    initWidget(panel);
    sinkEvents(Event.ONCLICK);
    setStyleName("gwt-TabBar");

    panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_BOTTOM);

    HTML first = new HTML("&nbsp;", true), rest = new HTML("&nbsp;", true);
    first.setStyleName("gwt-TabBarFirst");
    rest.setStyleName("gwt-TabBarRest");
    first.setHeight("100%");
    rest.setHeight("100%");

    panel.add(first);
    panel.add(rest);
    first.setHeight("100%");
    panel.setCellHeight(first, "100%");
    panel.setCellWidth(rest, "100%");
  }

  /**
   * Adds a new tab with the specified text.
   * 
   * @param text the new tab's text
   */
  public void addTab(String text) {
    insertTab(text, getTabCount());
  }

  /**
   * Adds a new tab with the specified text.
   * 
   * @param text the new tab's text
   * @param asHTML <code>true</code> to treat the specified text as html
   */
  public void addTab(String text, boolean asHTML) {
    insertTab(text, asHTML, getTabCount());
  }

  /**
   * Adds a new tab with the specified widget.
   * 
   * @param widget the new tab's widget.
   */
  public void addTab(Widget widget) {
    insertTab(widget, getTabCount());
  }

  public void addTabListener(TabListener listener) {
    if (tabListeners == null) {
      tabListeners = new TabListenerCollection();
    }
    tabListeners.add(listener);
  }

  /**
   * Gets the tab that is currently selected.
   * 
   * @return the selected tab
   */
  public int getSelectedTab() {
    if (selectedTab == null) {
      return -1;
    }
    return panel.getWidgetIndex(selectedTab) - 1;
  }

  /**
   * Gets the number of tabs present.
   * 
   * @return the tab count
   */
  public int getTabCount() {
    return panel.getWidgetCount() - 2;
  }

  /**
   * Gets the specified tab's HTML.
   * 
   * @param index the index of the tab whose HTML is to be retrieved
   * @return the tab's HTML
   */
  public String getTabHTML(int index) {
    if (index >= getTabCount()) {
      return null;
    }
    Widget widget = panel.getWidget(index + 1);
    if (widget instanceof HTML) {
      return ((HTML) widget).getHTML();
    } else if (widget instanceof Label) {
      return ((Label) widget).getText();
    } else {
      // This will be a ClickDecorator holding a user-supplied widget.
      return DOM.getInnerHTML(widget.getElement());
    }
  }

  /**
   * Inserts a new tab at the specified index.
   * 
   * @param text the new tab's text
   * @param asHTML <code>true</code> to treat the specified text as HTML
   * @param beforeIndex the index before which this tab will be inserted
   */
  public void insertTab(String text, boolean asHTML, int beforeIndex) {
    checkInsertBeforeTabIndex(beforeIndex);

    Label item;
    if (asHTML) {
      item = new HTML(text);
    } else {
      item = new Label(text);
    }

    item.setWordWrap(false);
    item.addClickListener(this);
    item.setStyleName(STYLENAME_DEFAULT);
    panel.insert(item, beforeIndex + 1);
    setStyleName(DOM.getParent(item.getElement()), STYLENAME_DEFAULT
        + "-wrapper", true);
  }

  /**
   * Inserts a new tab at the specified index.
   * 
   * @param text the new tab's text
   * @param beforeIndex the index before which this tab will be inserted
   */
  public void insertTab(String text, int beforeIndex) {
    insertTab(text, false, beforeIndex);
  }

  /**
   * Inserts a new tab at the specified index.
   * 
   * @param widget widget to be used in the new tab.
   * @param beforeIndex the index before which this tab will be inserted.
   */
  public void insertTab(Widget widget, int beforeIndex) {
    checkInsertBeforeTabIndex(beforeIndex);

    ClickDecoratorPanel decWidget = new ClickDecoratorPanel(widget, this);
    decWidget.addStyleName(STYLENAME_DEFAULT);
    panel.insert(decWidget, beforeIndex + 1);
    setStyleName(DOM.getParent(decWidget.getElement()), STYLENAME_DEFAULT
        + "-wrapper", true);
  }

  public void onClick(Widget sender) {
    for (int i = 1; i < panel.getWidgetCount() - 1; ++i) {
      if (panel.getWidget(i) == sender) {
        selectTab(i - 1);
        return;
      }
    }
  }

  /**
   * Removes the tab at the specified index.
   * 
   * @param index the index of the tab to be removed
   */
  public void removeTab(int index) {
    checkTabIndex(index);

    // (index + 1) to account for 'first' placeholder widget.
    Widget toRemove = panel.getWidget(index + 1);
    if (toRemove == selectedTab) {
      selectedTab = null;
    }
    panel.remove(toRemove);
  }

  public void removeTabListener(TabListener listener) {
    if (tabListeners != null) {
      tabListeners.remove(listener);
    }
  }

  /**
   * Programmatically selects the specified tab. Use index -1 to specify that no
   * tab should be selected.
   * 
   * @param index the index of the tab to be selected.
   * @return <code>true</code> if successful, <code>false</code> if the
   *         change is denied by the {@link TabListener}.
   */
  public boolean selectTab(int index) {
    checkTabIndex(index);

    if (tabListeners != null) {
      if (!tabListeners.fireBeforeTabSelected(this, index)) {
        return false;
      }
    }

    // Check for -1.
    setSelectionStyle(selectedTab, false);
    if (index == -1) {
      selectedTab = null;
      return true;
    }

    selectedTab = panel.getWidget(index + 1);
    setSelectionStyle(selectedTab, true);

    if (tabListeners != null) {
      tabListeners.fireTabSelected(this, index);
    }
    return true;
  }

  /**
   * <b>Affected Elements:</b>
   * <ul>
   * <li>-tab# = The element containing the contents of the tab.</li>
   * <li>-tab-wrapper# = The cell containing the tab at the index.</li>
   * </ul>
   * 
   * @see UIObject#onEnsureDebugId(String)
   */
  @Override
  protected void onEnsureDebugId(String baseID) {
    super.onEnsureDebugId(baseID);

    int numTabs = getTabCount();
    for (int i = 0; i < numTabs; i++) {
      Element widgetElem = panel.getWidget(i + 1).getElement();
      ensureDebugId(widgetElem, baseID, "tab" + i);
      ensureDebugId(DOM.getParent(widgetElem), baseID, "tab-wrapper" + i);
    } 
  }

  private void checkInsertBeforeTabIndex(int beforeIndex) {
    if ((beforeIndex < 0) || (beforeIndex > getTabCount())) {
      throw new IndexOutOfBoundsException();
    }
  }

  private void checkTabIndex(int index) {
    if ((index < -1) || (index >= getTabCount())) {
      throw new IndexOutOfBoundsException();
    }
  }

  private void setSelectionStyle(Widget item, boolean selected) {
    if (item != null) {
      if (selected) {
        item.addStyleName("gwt-TabBarItem-selected");
        setStyleName(DOM.getParent(item.getElement()),
            "gwt-TabBarItem-wrapper-selected", true);
      } else {
        item.removeStyleName("gwt-TabBarItem-selected");
        setStyleName(DOM.getParent(item.getElement()),
            "gwt-TabBarItem-wrapper-selected", false);
      }
    }
  }
}
