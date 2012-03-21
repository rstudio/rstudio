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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasAllKeyHandlers;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.HasBeforeSelectionHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
/**
 * A horizontal bar of folder-style tabs, most commonly used as part of a
 * {@link com.google.gwt.user.client.ui.TabPanel}.
 * <p>
 * <img class='gallery' src='doc-files/TabBar.png'/>
 * </p>
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-TabBar { the tab bar itself }</li>
 * <li>.gwt-TabBar .gwt-TabBarFirst { the left edge of the bar }</li>
 * <li>.gwt-TabBar .gwt-TabBarFirst-wrapper { table cell around the left edge }
 * </li>
 * <li>.gwt-TabBar .gwt-TabBarRest { the right edge of the bar }</li>
 * <li>.gwt-TabBar .gwt-TabBarRest-wrapper { table cell around the right edge }
 * </li>
 * <li>.gwt-TabBar .gwt-TabBarItem { unselected tabs }</li>
 * <li>.gwt-TabBar .gwt-TabBarItem-wrapper { table cell around tab }</li>
 * <li>.gwt-TabBar .gwt-TabBarItem-selected { additional style for selected
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.TabBarExample}
 * </p>
 */
@SuppressWarnings("deprecation")
public class TabBar extends Composite implements SourcesTabEvents,
    HasBeforeSelectionHandlers<Integer>, HasSelectionHandlers<Integer>,
    ClickListener, KeyboardListener {

  /**
   * Set of characteristic interfaces supported by {@link TabBar} tabs.
   *
   * Note that this set might expand over time, so implement this interface at
   * your own risk.
   */
  public interface Tab extends HasAllKeyHandlers, HasClickHandlers, HasWordWrap {
    /**
     * Check if the underlying widget implements {@link HasWordWrap}.
     *
     * @return true if the widget implements {@link HasWordWrap}
     */
    boolean hasWordWrap();
  }

  /**
   * <code>ClickDelegatePanel</code> decorates any widget with the minimal
   * amount of machinery to receive clicks for delegation to the parent.
   * {@link SourcesClickEvents} is not implemented due to the fact that only a
   * single observer is needed.
   */
  private class ClickDelegatePanel extends Composite implements Tab {
    private SimplePanel focusablePanel;
    private boolean enabled = true;

    ClickDelegatePanel(Widget child) {

      focusablePanel = new SimplePanel(FocusPanel.impl.createFocusable());
      focusablePanel.setWidget(child);
      SimplePanel wrapperWidget = createTabTextWrapper();
      if (wrapperWidget == null) {
        initWidget(focusablePanel);
      } else {
        wrapperWidget.setWidget(focusablePanel);
        initWidget(wrapperWidget);
      }

      sinkEvents(Event.ONCLICK | Event.ONKEYDOWN);
    }

    @Override
    public HandlerRegistration addClickHandler(ClickHandler handler) {
      return addHandler(handler, ClickEvent.getType());
    }

    @Override
    public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
      return addHandler(handler, KeyDownEvent.getType());
    }

    @Override
    public HandlerRegistration addKeyPressHandler(KeyPressHandler handler) {
      return addDomHandler(handler, KeyPressEvent.getType());
    }

    @Override
    public HandlerRegistration addKeyUpHandler(KeyUpHandler handler) {
      return addDomHandler(handler, KeyUpEvent.getType());
    }

    public SimplePanel getFocusablePanel() {
      return focusablePanel;
    }

    @Override
    public boolean getWordWrap() {
      if (hasWordWrap()) {
        return ((HasWordWrap) focusablePanel.getWidget()).getWordWrap();
      }
      throw new UnsupportedOperationException(
          "Widget does not implement HasWordWrap");
    }

    @Override
    public boolean hasWordWrap() {
      return focusablePanel.getWidget() instanceof HasWordWrap;
    }

    public boolean isEnabled() {
      return enabled;
    }

    @Override
    public void onBrowserEvent(Event event) {
      if (!enabled) {
        return;
      }

      // No need for call to super.
      switch (DOM.eventGetType(event)) {
        case Event.ONCLICK:
          TabBar.this.selectTabByTabWidget(this);
          TabBar.this.onClick(this);
          break;

        case Event.ONKEYDOWN:
          if (((char) DOM.eventGetKeyCode(event)) == KeyCodes.KEY_ENTER) {
            TabBar.this.selectTabByTabWidget(this);
          }
          TabBar.this.onKeyDown(this, (char) event.getKeyCode(),
              KeyboardListenerCollection.getKeyboardModifiers(event));
          break;
      }
      super.onBrowserEvent(event);
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    @Override
    public void setWordWrap(boolean wrap) {
      if (hasWordWrap()) {
        ((HasWordWrap) focusablePanel.getWidget()).setWordWrap(wrap);
      } else {
        throw new UnsupportedOperationException(
            "Widget does not implement HasWordWrap");
      }
    }
  }

  private static final String STYLENAME_DEFAULT = "gwt-TabBarItem";
  private HorizontalPanel panel = new HorizontalPanel();
  private Widget selectedTab;

  /**
   * Creates an empty tab bar.
   */
  public TabBar() {
    initWidget(panel);
    sinkEvents(Event.ONCLICK);
    setStyleName("gwt-TabBar");

    // Add a11y role "tablist"
    Roles.getTablistRole().set(panel.getElement());

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
    setStyleName(first.getElement().getParentElement(),
        "gwt-TabBarFirst-wrapper");
    setStyleName(rest.getElement().getParentElement(), "gwt-TabBarRest-wrapper");
  }

  @Override
  public HandlerRegistration addBeforeSelectionHandler(BeforeSelectionHandler<Integer> handler) {
    return addHandler(handler, BeforeSelectionEvent.getType());
  }

  @Override
  public HandlerRegistration addSelectionHandler(SelectionHandler<Integer> handler) {
    return addHandler(handler, SelectionEvent.getType());
  }

  /**
   * Adds a new tab with the specified text.
   *
   * @param html the new tab's html
   */
  public void addTab(SafeHtml html) {
    addTab(html.asString(), true);
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
   * @param widget the new tab's widget
   */
  public void addTab(Widget widget) {
    insertTab(widget, getTabCount());
  }

  /**
   * @deprecated Use {@link #addBeforeSelectionHandler(BeforeSelectionHandler)}
   * and {@link #addSelectionHandler(SelectionHandler)} instead
   */
  @Override
  @Deprecated
  public void addTabListener(TabListener listener) {
    ListenerWrapper.WrappedTabListener.add(this, listener);
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
   * Gets the given tab.
   *
   * This method is final because the Tab interface will expand. Therefore
   * it is highly likely that subclasses which implemented this method would end up
   * breaking.
   *
   * @param index the tab's index
   * @return the tab wrapper
   */
  public final Tab getTab(int index) {
    if (index >= getTabCount()) {
      return null;
    }
    ClickDelegatePanel p = (ClickDelegatePanel) panel.getWidget(index + 1);
    return p;
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
    ClickDelegatePanel delPanel = (ClickDelegatePanel) panel.getWidget(index + 1);
    SimplePanel focusablePanel = delPanel.getFocusablePanel();
    Widget widget = focusablePanel.getWidget();
    if (widget instanceof HTML) {
      return ((HTML) widget).getHTML();
    } else if (widget instanceof Label) {
      return ((Label) widget).getText();
    } else {
      // This will be a focusable panel holding a user-supplied widget.
      return focusablePanel.getElement().getParentElement().getInnerHTML();
    }
  }

  /**
   * Inserts a new tab at the specified index.
   *
   * @param html the new tab's html
   * @param beforeIndex the index before which this tab will be inserted
   */
  public void insertTab(SafeHtml html, int beforeIndex) {
    insertTab(html.asString(), true, beforeIndex);
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
    insertTabWidget(item, beforeIndex);
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
   * @param widget widget to be used in the new tab
   * @param beforeIndex the index before which this tab will be inserted
   */
  public void insertTab(Widget widget, int beforeIndex) {
    insertTabWidget(widget, beforeIndex);
  }

  /**
   * Check if a tab is enabled or disabled. If disabled, the user cannot select
   * the tab.
   *
   * @param index the index of the tab
   * @return true if the tab is enabled, false if disabled
   */
  public boolean isTabEnabled(int index) {
    assert (index >= 0) && (index < getTabCount()) : "Tab index out of bounds";
    ClickDelegatePanel delPanel = (ClickDelegatePanel) panel.getWidget(index + 1);
    return delPanel.isEnabled();
  }

  /**
   * @deprecated add a {@link BeforeSelectionHandler} instead. Alternatively, if
   * you need to access to the individual tabs, add a click handler to each
   * {@link Tab} element instead.
   */
  @Override
  @Deprecated
  public void onClick(Widget sender) {
  }

  /**
   * @deprecated add a key down handler to the individual {@link Tab} objects
   *  instead.
   */
  @Override
  @Deprecated
  public void onKeyDown(Widget sender, char keyCode, int modifiers) {
  }

  /**
   * @deprecated this method has been doing nothing for the entire last release,
   * if what you wanted to do was to listen to key press events on tabs, add the
   * key press handler to the individual tab wrappers instead.
   */
  @Override
  @Deprecated
  public void onKeyPress(Widget sender, char keyCode, int modifiers) {
  }

  /**
   * @deprecated this method has been doing nothing for the entire last release,
   * if what you wanted to do was to listen to key up events on tabs, add the
   * key up handler to the individual tab wrappers instead.
   *
   */
  @Override
  @Deprecated
  public void onKeyUp(Widget sender, char keyCode, int modifiers) {
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

  /**
   * @deprecated Instead use the {@link HandlerRegistration#removeHandler}
   * call on the object returned by an add*Handler method
   */
  @Override
  @Deprecated
  public void removeTabListener(TabListener listener) {
    ListenerWrapper.WrappedTabListener.remove(this, listener);
  }

  /**
   * Programmatically selects the specified tab and fires events. Use index -1
   * to specify that no tab should be selected.
   *
   * @param index the index of the tab to be selected
   * @return <code>true</code> if successful, <code>false</code> if the change
   * is denied by the {@link BeforeSelectionHandler}.
   */
  public boolean selectTab(int index) {
    return selectTab(index, true);
  }

  /**
   * Programmatically selects the specified tab. Use index -1 to specify that no
   * tab should be selected.
   *
   * @param index the index of the tab to be selected
   * @param fireEvents true to fire events, false not to
   * @return <code>true</code> if successful, <code>false</code> if the change
   * is denied by the {@link BeforeSelectionHandler}.
   */
  public boolean selectTab(int index, boolean fireEvents) {
    checkTabIndex(index);

    if (fireEvents) {
      BeforeSelectionEvent<?> event = BeforeSelectionEvent.fire(this, index);
      if (event != null && event.isCanceled()) {
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
    if (fireEvents) {
      SelectionEvent.fire(this, index);
    }
    return true;
  }

  /**
   * Enable or disable a tab. When disabled, users cannot select the tab.
   *
   * @param index the index of the tab to enable or disable
   * @param enabled true to enable, false to disable
   */
  public void setTabEnabled(int index, boolean enabled) {
    assert (index >= 0) && (index < getTabCount()) : "Tab index out of bounds";

    // Style the wrapper
    ClickDelegatePanel delPanel = (ClickDelegatePanel) panel.getWidget(index + 1);
    delPanel.setEnabled(enabled);
    setStyleName(delPanel.getElement(), "gwt-TabBarItem-disabled", !enabled);
    setStyleName(delPanel.getElement().getParentElement(),
        "gwt-TabBarItem-wrapper-disabled", !enabled);
  }

  /**
   * Sets a tab's contents via HTML.
   *
   * Use care when setting an object's HTML; it is an easy way to expose
   * script-based security problems. Consider using
   * {@link #setTabText(int, String)} or {@link #setTabHTML(int, SafeHtml)}
   * whenever possible.
   *
   * @param index the index of the tab whose HTML is to be set
   * @param html the tab new HTML
   */
  public void setTabHTML(int index, String html) {
    assert (index >= 0) && (index < getTabCount()) : "Tab index out of bounds";

    ClickDelegatePanel delPanel = (ClickDelegatePanel) panel.getWidget(index + 1);
    SimplePanel focusablePanel = delPanel.getFocusablePanel();
    focusablePanel.setWidget(new HTML(html, false));
  }

  /**
   * Sets a tab's contents via safe html.
   *
   * @param index the index of the tab whose HTML is to be set
   * @param html the tab new HTML
   */
  public void setTabHTML(int index, SafeHtml html) {
    setTabHTML(index, html.asString());
  }

  /**
   * Sets a tab's text contents.
   *
   * @param index the index of the tab whose text is to be set
   * @param text the object's new text
   */
  public void setTabText(int index, String text) {
    assert (index >= 0) && (index < getTabCount()) : "Tab index out of bounds";

    ClickDelegatePanel delPanel = (ClickDelegatePanel) panel.getWidget(index + 1);
    SimplePanel focusablePanel = delPanel.getFocusablePanel();

    // It is not safe to check if the current widget is an instanceof Label and
    // reuse it here because HTML is an instanceof Label. Leaving an HTML would
    // throw off the results of getTabHTML(int).
    focusablePanel.setWidget(new Label(text, false));
  }

  /**
   * Create a {@link SimplePanel} that will wrap the contents in a tab.
   * Subclasses can use this method to wrap tabs in decorator panels.
   *
   * @return a {@link SimplePanel} to wrap the tab contents, or null to leave
   * tabs unwrapped
   */
  protected SimplePanel createTabTextWrapper() {
    return null;
  }

  /**
   * Inserts a new tab at the specified index.
   *
   * @param widget widget to be used in the new tab
   * @param beforeIndex the index before which this tab will be inserted
   */
  protected void insertTabWidget(Widget widget, int beforeIndex) {
    checkInsertBeforeTabIndex(beforeIndex);

    ClickDelegatePanel delWidget = new ClickDelegatePanel(widget);
    delWidget.setStyleName(STYLENAME_DEFAULT);

    // Add a11y role "tab"
    SimplePanel focusablePanel = delWidget.getFocusablePanel();
    Roles.getTabRole().set(focusablePanel.getElement());

    panel.insert(delWidget, beforeIndex + 1);

    setStyleName(DOM.getParent(delWidget.getElement()), STYLENAME_DEFAULT
        + "-wrapper", true);
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
      ClickDelegatePanel delPanel = (ClickDelegatePanel) panel.getWidget(i + 1);
      SimplePanel focusablePanel = delPanel.getFocusablePanel();
      ensureDebugId(focusablePanel.getContainerElement(), baseID, "tab" + i);
      ensureDebugId(DOM.getParent(delPanel.getElement()), baseID, "tab-wrapper"
          + i);
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

  /**
   * Selects the tab corresponding to the widget for the tab. To be clear the
   * widget for the tab is not the widget INSIDE of the tab; it is the widget
   * used to represent the tab itself.
   *
   * @param tabWidget The widget for the tab to be selected
   * @return true if the tab corresponding to the widget for the tab could
   * located and selected, false otherwise
   */
  private boolean selectTabByTabWidget(Widget tabWidget) {
    int numTabs = panel.getWidgetCount() - 1;

    for (int i = 1; i < numTabs; ++i) {
      if (panel.getWidget(i) == tabWidget) {
        return selectTab(i - 1);
      }
    }

    return false;
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
