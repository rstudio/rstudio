/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.HasBeforeSelectionHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.layout.client.Layout.AnimationCallback;
import com.google.gwt.safehtml.shared.SafeHtml;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A panel that stacks its children vertically, displaying only one at a time,
 * with a header for each child which the user can click to display.
 *
 * <p>
 * This widget will <em>only</em> work in standards mode, which requires that
 * the HTML page in which it is run have an explicit &lt;!DOCTYPE&gt;
 * declaration.
 * </p>
 *
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-StackLayoutPanel <dd> the panel itself
 * <dt>.gwt-StackLayoutPanel .gwt-StackLayoutPanelHeader <dd> applied to each
 * header widget
 * <dt>.gwt-StackLayoutPanel .gwt-StackLayoutPanelHeader-hovering <dd> applied to each
 * header widget on mouse hover
 * <dt>.gwt-StackLayoutPanel .gwt-StackLayoutPanelContent <dd> applied to each
 * child widget
 * </dl>
 *
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.StackLayoutPanelExample}
 * </p>
 *
 * <h3>Use in UiBinder Templates</h3>
 * <p>
 * A StackLayoutPanel element in a
 * {@link com.google.gwt.uibinder.client.UiBinder UiBinder} template may have a
 * <code>unit</code> attribute with a
 * {@link com.google.gwt.dom.client.Style.Unit Style.Unit} value (it defaults to
 * PX).
 * <p>
 * The children of a StackLayoutPanel element are laid out in &lt;g:stack>
 * elements. Each stack can have one widget child and one of two types of header
 * elements. A &lt;g:header> element can hold html, or a &lt;g:customHeader>
 * element can hold a widget. (Note that the tags of the header elements are not
 * capitalized. This is meant to signal that the head is not a runtime object,
 * and so cannot have a <code>ui:field</code> attribute.)
 * <p>
 * For example:
 *
 * <pre>
 * &lt;g:StackLayoutPanel unit='PX'>
 *  &lt;g:stack>
 *    &lt;g:header size='3'>&lt;b>HTML&lt;/b> header&lt;/g:header>
 *    &lt;g:Label>able&lt;/g:Label>
 *  &lt;/g:stack>
 *  &lt;g:stack>
 *    &lt;g:customHeader size='3'>
 *      &lt;g:Label>Custom header&lt;/g:Label>
 *    &lt;/g:customHeader>
 *    &lt;g:Label>baker&lt;/g:Label>
 *  &lt;/g:stack>
 * &lt;/g:StackLayoutPanel>
 * </pre>
 */
public class StackLayoutPanel extends ResizeComposite implements HasWidgets,
    ProvidesResize, IndexedPanel.ForIsWidget, AnimatedLayout,
    HasBeforeSelectionHandlers<Integer>, HasSelectionHandlers<Integer> {

  private class Header extends Composite implements HasClickHandlers {
    public Header(Widget child) {
      initWidget(child);
    }

    public HandlerRegistration addClickHandler(ClickHandler handler) {
      return this.addDomHandler(handler, ClickEvent.getType());
    }

    public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
      return this.addDomHandler(handler, MouseOutEvent.getType());
    }

    public HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
      return this.addDomHandler(handler, MouseOverEvent.getType());
    }
  }

  private static class LayoutData {
    public double headerSize;
    public Header header;
    public Widget widget;

    public LayoutData(Widget widget, Header header, double headerSize) {
      this.widget = widget;
      this.header = header;
      this.headerSize = headerSize;
    }
  }

  private static final String WIDGET_STYLE = "gwt-StackLayoutPanel";
  private static final String CONTENT_STYLE = "gwt-StackLayoutPanelContent";
  private static final String HEADER_STYLE = "gwt-StackLayoutPanelHeader";
  private static final String HEADER_STYLE_HOVERING = "gwt-StackLayoutPanelHeader-hovering";

  private static final int ANIMATION_TIME = 250;

  private int animationDuration = ANIMATION_TIME;
  private LayoutPanel layoutPanel;
  private final Unit unit;
  private final ArrayList<LayoutData> layoutData = new ArrayList<LayoutData>();
  private int selectedIndex = -1;

  /**
   * Creates an empty stack panel.
   *
   * @param unit the unit to be used for layout
   */
  public StackLayoutPanel(Unit unit) {
    this.unit = unit;
    initWidget(layoutPanel = new LayoutPanel());
    setStyleName(WIDGET_STYLE);
  }

  public void add(Widget w) {
    assert false : "Single-argument add() is not supported for this widget";
  }

  /**
   * Adds a child widget to this stack, along with a widget representing the
   * stack header.
   *
   * @param widget the child widget to be added
   * @param header the html to be shown on its header
   * @param headerSize the size of the header widget
   */
  public void add(final Widget widget, SafeHtml header, double headerSize) {
    add(widget, header.asString(), true, headerSize);
  }
  
  /**
   * Adds a child widget to this stack, along with a widget representing the
   * stack header.
   * 
   * @param widget the child widget to be added
   * @param header the text to be shown on its header
   * @param asHtml <code>true</code> to treat the specified text as HTML
   * @param headerSize the size of the header widget
   */
  public void add(final Widget widget, String header, boolean asHtml, double headerSize) {
    insert(widget, header, asHtml, headerSize, getWidgetCount());
  }
  
  /**
   * Overloaded version for IsWidget.
   * 
   * @see #add(Widget,String,boolean,double)
   */
  public void add(final IsWidget widget, String header, boolean asHtml, double headerSize) {
    this.add(widget.asWidget(), header, asHtml, headerSize);
  }

  /**
   * Adds a child widget to this stack, along with a widget representing the
   * stack header.
   *
   * @param widget the child widget to be added
   * @param header the text to be shown on its header
   * @param headerSize the size of the header widget
   */
  public void add(final Widget widget, String header, double headerSize) {
    insert(widget, header, headerSize, getWidgetCount());
  }

  /**
   * Adds a child widget to this stack, along with a widget representing the
   * stack header.
   *
   * @param widget the child widget to be added
   * @param header the header widget
   * @param headerSize the size of the header widget
   */
  public void add(final Widget widget, Widget header, double headerSize) {
    insert(widget, header, headerSize, getWidgetCount());
  }

  /**
   * Overloaded version for IsWidget.
   * 
   * @see #add(Widget,Widget,double)
   */
  public void add(final IsWidget widget, IsWidget header, double headerSize) {
    this.add(widget.asWidget(), header.asWidget(), headerSize);
  }
  
  public HandlerRegistration addBeforeSelectionHandler(
      BeforeSelectionHandler<Integer> handler) {
    return addHandler(handler, BeforeSelectionEvent.getType());
  }

  public HandlerRegistration addSelectionHandler(
      SelectionHandler<Integer> handler) {
    return addHandler(handler, SelectionEvent.getType());
  }

  public void animate(int duration) {
    animate(duration, null);
  }

  public void animate(int duration, AnimationCallback callback) {
    // Don't try to animate zero widgets.
    if (layoutData.size() == 0) {
      if (callback != null) {
        callback.onAnimationComplete();
      }
      return;
    }

    double top = 0, bottom = 0;
    int i = 0;
    for (; i < layoutData.size(); ++i) {
      LayoutData data = layoutData.get(i);
      layoutPanel.setWidgetTopHeight(data.header, top, unit, data.headerSize,
          unit);

      top += data.headerSize;

      layoutPanel.setWidgetTopHeight(data.widget, top, unit, 0, unit);

      if (i == selectedIndex) {
        break;
      }
    }

    for (int j = layoutData.size() - 1; j > i; --j) {
      LayoutData data = layoutData.get(j);
      layoutPanel.setWidgetBottomHeight(data.header, bottom, unit,
          data.headerSize, unit);
      layoutPanel.setWidgetBottomHeight(data.widget, bottom, unit, 0, unit);
      bottom += data.headerSize;
    }

    LayoutData data = layoutData.get(selectedIndex);
    layoutPanel.setWidgetTopBottom(data.widget, top, unit, bottom, unit);

    layoutPanel.animate(duration, callback);
  }

  public void clear() {
    layoutPanel.clear();
    layoutData.clear();
    selectedIndex = -1;
  }

  public void forceLayout() {
    layoutPanel.forceLayout();
  }

  /**
   * Get the duration of the animated transition between children.
   * 
   * @return the duration in milliseconds
   */
  public int getAnimationDuration() {
    return animationDuration;
  }

  /**
   * Gets the widget in the stack header at the given index.
   *
   * @param index the index of the stack header to be retrieved
   * @return the header widget
   */
  public Widget getHeaderWidget(int index) {
    checkIndex(index);
    return layoutData.get(index).header.getWidget();
  }

  /**
   * Gets the widget in the stack header associated with the given child widget.
   *
   * @param child the child whose stack header is to be retrieved
   * @return the header widget
   */
  public Widget getHeaderWidget(Widget child) {
    checkChild(child);
    return getHeaderWidget(getWidgetIndex(child));
  }

  /**
   * Gets the currently-selected index.
   *
   * @return the selected index, or <code>-1</code> if none is selected
   */
  public int getVisibleIndex() {
    return selectedIndex;
  }

  /**
   * Gets the currently-selected widget.
   *
   * @return the selected widget, or <code>null</code> if none exist
   */
  public Widget getVisibleWidget() {
    if (selectedIndex == -1) {
      return null;
    }
    return getWidget(selectedIndex);
  }

  public Widget getWidget(int index) {
    return layoutPanel.getWidget(index * 2 + 1);
  }

  public int getWidgetCount() {
    return layoutPanel.getWidgetCount() / 2;
  }

  public int getWidgetIndex(IsWidget child) {
    return getWidgetIndex(asWidgetOrNull(child));
  }

  public int getWidgetIndex(Widget child) {
    int index = layoutPanel.getWidgetIndex(child);
    if (index == -1) {
      return index;
    }
    return (index - 1) / 2;
  }

  /**
   * Inserts a widget into the panel. If the Widget is already attached, it will
   * be moved to the requested index.
   *
   * @param child the widget to be added
   * @param html the safe html to be shown on its header
   * @param headerSize the size of the header widget
   * @param beforeIndex the index before which it will be inserted
   */
  public void insert(Widget child, SafeHtml html, double headerSize, 
      int beforeIndex) {
    insert(child, html.asString(), true, headerSize, beforeIndex);
  }
  
  /**
   * Inserts a widget into the panel. If the Widget is already attached, it will
   * be moved to the requested index.
   * 
   * @param child the widget to be added
   * @param text the text to be shown on its header
   * @param asHtml <code>true</code> to treat the specified text as HTML
   * @param headerSize the size of the header widget
   * @param beforeIndex the index before which it will be inserted
   */
  public void insert(Widget child, String text, boolean asHtml,
      double headerSize, int beforeIndex) {
    HTML contents = new HTML();
    if (asHtml) {
      contents.setHTML(text);
    } else {
      contents.setText(text);
    }
    insert(child, contents, headerSize, beforeIndex);
  }

  /**
   * Inserts a widget into the panel. If the Widget is already attached, it will
   * be moved to the requested index.
   *
   * @param child the widget to be added
   * @param text the text to be shown on its header
   * @param headerSize the size of the header widget
   * @param beforeIndex the index before which it will be inserted
   */
  public void insert(Widget child, String text, double headerSize, int beforeIndex) {
    insert(child, text, false, headerSize, beforeIndex);
  }

  /**
   * Inserts a widget into the panel. If the Widget is already attached, it will
   * be moved to the requested index.
   *
   * @param child the widget to be added
   * @param header the widget to be placed in the associated header
   * @param headerSize the size of the header widget
   * @param beforeIndex the index before which it will be inserted
   */
  public void insert(Widget child, Widget header, double headerSize,
      int beforeIndex) {
    insert(child, new Header(header), headerSize, beforeIndex);
  }

  public Iterator<Widget> iterator() {
    return new Iterator<Widget>() {
      int i = 0, last = -1;

      public boolean hasNext() {
        return i < layoutData.size();
      }

      public Widget next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return layoutData.get(last = i++).widget;
      }

      public void remove() {
        if (last < 0) {
          throw new IllegalStateException();
        }

        StackLayoutPanel.this.remove(layoutData.get(last).widget);
        i = last;
        last = -1;
      }
    };
  }

  @Override
  public void onResize() {
    layoutPanel.onResize();
  }

  public boolean remove(int index) {
    return remove(getWidget(index));
  }

  public boolean remove(Widget child) {
    if (child.getParent() != layoutPanel) {
      return false;
    }

    // Find the layoutData associated with this widget and remove it.
    for (int i = 0; i < layoutData.size(); ++i) {
      LayoutData data = layoutData.get(i);
      if (data.widget == child) {
        layoutPanel.remove(data.header);
        layoutPanel.remove(data.widget);

        data.header.removeStyleName(HEADER_STYLE);
        data.widget.removeStyleName(CONTENT_STYLE);

        layoutData.remove(i);

        if (selectedIndex == i) {
          selectedIndex = -1;
          if (layoutData.size() > 0) {
            showWidget(layoutData.get(0).widget);
          }
        } else {
          if (i <= selectedIndex) {
            selectedIndex--;
          }
          animate(animationDuration);
        }
        return true;
      }
    }

    return false;
  }

  /**
   * Set the duration of the animated transition between children.
   * 
   * @param duration the duration in milliseconds.
   */
  public void setAnimationDuration(int duration) {
    this.animationDuration = duration;
  }

  /**
   * Sets a stack header's HTML contents.
   *
   * Use care when setting an object's HTML; it is an easy way to expose
   * script-based security problems. Consider using
   * {@link #setHeaderHTML(int, SafeHtml)} or 
   * {@link #setHeaderText(int, String)} whenever possible.
   *
   * @param index the index of the header whose HTML is to be set
   * @param html the header's new HTML contents
   */
  public void setHeaderHTML(int index, String html) {
    checkIndex(index);
    LayoutData data = layoutData.get(index);

    Widget headerWidget = data.header.getWidget();
    assert headerWidget instanceof HasHTML : "Header widget does not implement HasHTML";
    ((HasHTML) headerWidget).setHTML(html);
  }

  /**
   * Sets a stack header's HTML contents.
   * 
   * @param index the index of the header whose HTML is to be set
   * @param html the header's new HTML contents
   */
  public void setHeaderHTML(int index, SafeHtml html) {
    setHeaderHTML(index, html.asString());
  }
  
  /**
   * Sets a stack header's text contents.
   *
   * @param index the index of the header whose text is to be set
   * @param text the object's new text
   */
  public void setHeaderText(int index, String text) {
    checkIndex(index);
    LayoutData data = layoutData.get(index);

    Widget headerWidget = data.header.getWidget();
    assert headerWidget instanceof HasText : "Header widget does not implement HasText";
    ((HasText) headerWidget).setText(text);
  }

  /**
   * Shows the widget at the specified index and fires events.
   *
   * @param index the index of the child widget to be shown.
   */
  public void showWidget(int index) {
    showWidget(index, true);
  }

  /**
   * Shows the widget at the specified index.
   *
   * @param index the index of the child widget to be shown.
   * @param fireEvents true to fire events, false not to
   */
  public void showWidget(int index, boolean fireEvents) {
    checkIndex(index);
    showWidget(index, animationDuration, fireEvents);
  }

  /**
   * Shows the specified widget and fires events.
   *
   * @param child the child widget to be shown.
   */
  public void showWidget(Widget child) {
    showWidget(getWidgetIndex(child));
  }

  /**
   * Shows the specified widget.
   *
   * @param child the child widget to be shown.
   * @param fireEvents true to fire events, false not to
   */
  public void showWidget(Widget child, boolean fireEvents) {
    showWidget(getWidgetIndex(child), animationDuration, fireEvents);
  }

  @Override
  protected void onLoad() {
    // When the widget becomes attached, update its layout.
    animate(0);
  }

  private void checkChild(Widget child) {
    assert layoutPanel.getChildren().contains(child);
  }

  private void checkIndex(int index) {
    assert (index >= 0) && (index < getWidgetCount()) : "Index out of bounds";
  }

  private void insert(final Widget child, final Header header, double headerSize,
      int beforeIndex) {
    assert (beforeIndex >= 0) && (beforeIndex <= getWidgetCount()) : "beforeIndex out of bounds";

    // Check to see if the StackPanel already contains the Widget. If so,
    // remove it and see if we need to shift the position to the left.
    int idx = getWidgetIndex(child);
    if (idx != -1) {
      remove(child);
      if (idx < beforeIndex) {
        beforeIndex--;
      }
    }

    int widgetIndex = beforeIndex * 2;
    layoutPanel.insert(child, widgetIndex);
    layoutPanel.insert(header, widgetIndex);

    layoutPanel.setWidgetLeftRight(header, 0, Unit.PX, 0, Unit.PX);
    layoutPanel.setWidgetLeftRight(child, 0, Unit.PX, 0, Unit.PX);

    LayoutData data = new LayoutData(child, header, headerSize);
    layoutData.add(beforeIndex, data);

    header.addStyleName(HEADER_STYLE);
    child.addStyleName(CONTENT_STYLE);

    header.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        showWidget(child);
      }
    });

    header.addMouseOutHandler(new MouseOutHandler() {
      public void onMouseOut(MouseOutEvent event) {
        header.removeStyleName(HEADER_STYLE_HOVERING);
      }
    });

    header.addMouseOverHandler(new MouseOverHandler() {
      public void onMouseOver(MouseOverEvent event) {
        header.addStyleName(HEADER_STYLE_HOVERING);
      }
    });

    if (selectedIndex == -1) {
      // If there's no visible widget, display the first one. The layout will
      // be updated onLoad().
      showWidget(0);
    } else if (beforeIndex <= selectedIndex) {
      // If we inserted an item before the selected index, increment it.
      selectedIndex++;
    }

    // If the widget is already attached, we must call animate() to update the
    // layout (if it's not yet attached, then onLoad() will do this).
    if (isAttached()) {
      animate(animationDuration);
    }
  }

  private void showWidget(int index, final int duration, boolean fireEvents) {
    checkIndex(index);
    if (index == selectedIndex) {
      return;
    }

    // Fire the before selection event, giving the recipients a chance to
    // cancel the selection.
    if (fireEvents) {
      BeforeSelectionEvent<Integer> event = BeforeSelectionEvent.fire(this, index);
      if ((event != null) && event.isCanceled()) {
        return;
      }
    }

    selectedIndex = index;

    if (isAttached()) {
      animate(duration);
    }

    // Fire the selection event.
    if (fireEvents) {
      SelectionEvent.fire(this, index);
    }
  }
}
