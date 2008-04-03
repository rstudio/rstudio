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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.animation.WidgetAnimation;

/**
 * A panel that stacks its children vertically, displaying only one at a time,
 * with a header for each child which the user can click to display.
 * <p>
 * <img class='gallery' src='StackPanel.png'/>
 * </p>
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-StackPanel { the panel itself }</li>
 * <li>.gwt-StackPanel .gwt-StackPanelItem { unselected items }</li>
 * <li>.gwt-StackPanel .gwt-StackPanelItem-selected { selected items }</li>
 * <li>.gwt-StackPanel .gwt-StackPanelContent { the wrapper around the contents
 * of the item }</li>
 * </ul>
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.StackPanelExample}
 * </p>
 */
public class StackPanel extends ComplexPanel implements HasAnimation {
  /**
   * An {@link WidgetAnimation} used to switch between the visible stack.
   */
  public static class StackAnimation extends WidgetAnimation {
    /**
     * The {@link StackPanel} being affected.
     */
    private StackPanel curPanel = null;

    /**
     * The index of the stack that is being hidden.
     */
    private int oldIndex = -1;

    /**
     * The index of the stack that is being displayed.
     */
    private int newIndex = -1;

    /**
     * The container that is being displayed.
     */
    private Element newTD = null;

    @Override
    public void onCancel() {
      onComplete();
    }

    @Override
    public void onComplete() {
      if (curPanel != null) {
        curPanel.setStackContentVisible(oldIndex, false);
        curPanel.setStackContentVisible(newIndex, true);
        clearOpacity(newTD);
        curPanel = null;
      }
    }

    @Override
    public void onInstantaneousRun() {
      curPanel.setStackContentVisible(oldIndex, false);
      curPanel.setStackContentVisible(newIndex, true);
      curPanel = null;
    }

    @Override
    public void onStart() {
      onUpdate(0.0);
      curPanel.setStackContentVisible(oldIndex, false);
      curPanel.setStackContentVisible(newIndex, true);
    }

    @Override
    public void onUpdate(double progress) {
      setOpacity(newTD, progress);
    }

    /**
     * Open or close the content.
     * 
     * @param panel the panel being affected
     * @param oldIndex the index being closed
     * @param newIndex the index being opened
     */
    public void showStack(StackPanel panel, int oldIndex, int newIndex) {
      // Immediately complete previous open
      cancel();

      // Do not animate if there is no stack currently visible
      if (oldIndex < 0) {
        panel.setStackContentVisible(newIndex, true);
        return;
      }

      // Get all of the affected Elements
      curPanel = panel;
      this.oldIndex = oldIndex;
      this.newIndex = newIndex;
      newTD = DOM.getFirstChild(DOM.getChild(curPanel.body, (2 * newIndex) + 1));

      // Run the animation
      if (panel.isAnimationEnabled) {
        run(250);
      } else {
        onInstantaneousRun();
      }
    }

    /**
     * Clear the opacity of an {@link Element}.
     * 
     * @param elem the {@link Element}
     */
    protected void clearOpacity(Element elem) {
      DOM.setStyleAttribute(elem, "opacity", "");
    }

    /**
     * Set the opacity of an {@link Element}.
     * 
     * @param elem the {@link Element}
     * @param opacity the opacity between 0.0 and 1.0, inclusively
     */
    protected void setOpacity(Element elem, double opacity) {
      DOM.setStyleAttribute(elem, "opacity", opacity + "");
    }
  }

  /**
   * IE version of {@link StackAnimation} that sets opacity using filters.
   */
  public static class StackAnimationIE extends StackAnimation {
    @Override
    protected void clearOpacity(Element elem) {
      DOM.setStyleAttribute(elem, "filter", "");
    }

    @Override
    protected void setOpacity(Element elem, double opacity) {
      int opac = (int) (100 * opacity);
      DOM.setStyleAttribute(elem, "filter", "alpha(opacity=" + opac + ")");
    }
  }

  private static final String DEFAULT_STYLENAME = "gwt-StackPanel";
  private static final String DEFAULT_ITEM_STYLENAME = DEFAULT_STYLENAME
      + "Item";

  /**
   * The {@link WidgetAnimation} used to switch stacks.
   */
  private static StackAnimation stackAnimation;

  private Element body;
  private boolean isAnimationEnabled = true;
  private int visibleStack = -1;

  /**
   * Creates an empty stack panel.
   */
  public StackPanel() {
    Element table = DOM.createTable();
    setElement(table);

    body = DOM.createTBody();
    DOM.appendChild(table, body);
    DOM.setElementPropertyInt(table, "cellSpacing", 0);
    DOM.setElementPropertyInt(table, "cellPadding", 0);

    DOM.sinkEvents(table, Event.ONCLICK);
    setStyleName(DEFAULT_STYLENAME);
  }

  /**
   * Adds a new child with the given widget.
   * 
   * @param w the widget to be added
   */
  @Override
  public void add(Widget w) {
    insert(w, getWidgetCount());
  }

  /**
   * Adds a new child with the given widget and header.
   * 
   * @param w the widget to be added
   * @param stackText the header text associated with this widget
   */
  public void add(Widget w, String stackText) {
    add(w, stackText, false);
  }

  /**
   * Adds a new child with the given widget and header, optionally interpreting
   * the header as HTML.
   * 
   * @param w the widget to be added
   * @param stackText the header text associated with this widget
   * @param asHTML <code>true</code> to treat the specified text as HTML
   */
  public void add(Widget w, String stackText, boolean asHTML) {
    add(w);
    setStackText(getWidgetCount() - 1, stackText, asHTML);
  }

  /**
   * Gets the currently selected child index.
   * 
   * @return selected child
   */
  public int getSelectedIndex() {
    return visibleStack;
  }

  /**
   * Inserts a widget before the specified index.
   * 
   * @param w the widget to be inserted
   * @param beforeIndex the index before which it will be inserted
   * @throws IndexOutOfBoundsException if <code>beforeIndex</code> is out of
   *           range
   */
  public void insert(Widget w, int beforeIndex) {
    // header
    Element trh = DOM.createTR();
    Element tdh = DOM.createTD();
    DOM.appendChild(trh, tdh);
    DOM.appendChild(tdh, createHeaderElem());

    // body
    Element trb = DOM.createTR();
    Element tdb = DOM.createTD();
    DOM.appendChild(trb, tdb);

    // DOM indices are 2x logical indices; 2 dom elements per stack item
    beforeIndex = adjustIndex(w, beforeIndex);
    int effectiveIndex = beforeIndex * 2;

    // this ordering puts the body below the header
    DOM.insertChild(body, trb, effectiveIndex);
    DOM.insertChild(body, trh, effectiveIndex);

    // header styling
    setStyleName(tdh, DEFAULT_ITEM_STYLENAME, true);
    DOM.setElementPropertyInt(tdh, "__owner", hashCode());
    DOM.setElementProperty(tdh, "height", "1px");

    // body styling
    setStyleName(tdb, DEFAULT_STYLENAME + "Content", true);
    DOM.setElementProperty(tdb, "height", "100%");
    DOM.setElementProperty(tdb, "vAlign", "top");

    // Now that the DOM is connected, call insert (this ensures that onLoad() is
    // not fired until the child widget is attached to the DOM).
    super.insert(w, tdb, beforeIndex, false);

    // Update indices of all elements to the right.
    updateIndicesFrom(beforeIndex);

    // Correct visible stack for new location.
    if (visibleStack == -1) {
      showStack(0);
    } else {
      setStackVisible(beforeIndex, false, true);
      if (visibleStack >= beforeIndex) {
        ++visibleStack;
      }
      // Reshow the stack to apply style names
      setStackVisible(visibleStack, true, true);
    }
  }

  /**
   * @see HasAnimation#isAnimationEnabled()
   */
  public boolean isAnimationEnabled() {
    return isAnimationEnabled;
  }

  @Override
  public void onBrowserEvent(Event event) {
    if (DOM.eventGetType(event) == Event.ONCLICK) {
      Element target = DOM.eventGetTarget(event);
      int index = findDividerIndex(target);
      if (index != -1) {
        showStack(index);
      }
    }
  }

  @Override
  public boolean remove(int index) {
    return remove(getWidget(index), index);
  }

  @Override
  public boolean remove(Widget child) {
    return remove(child, getWidgetIndex(child));
  }

  /**
   * @see HasAnimation#setAnimationEnabled(boolean)
   */
  public void setAnimationEnabled(boolean enable) {
    isAnimationEnabled = enable;
  }

  /**
   * Sets the text associated with a child by its index.
   * 
   * @param index the index of the child whose text is to be set
   * @param text the text to be associated with it
   */
  public void setStackText(int index, String text) {
    setStackText(index, text, false);
  }

  /**
   * Sets the text associated with a child by its index.
   * 
   * @param index the index of the child whose text is to be set
   * @param text the text to be associated with it
   * @param asHTML <code>true</code> to treat the specified text as HTML
   */
  public void setStackText(int index, String text, boolean asHTML) {
    if (index >= getWidgetCount()) {
      return;
    }

    Element tdWrapper = DOM.getChild(DOM.getChild(body, index * 2), 0);
    Element tbody = DOM.getFirstChild(DOM.getFirstChild(tdWrapper));
    Element tr = DOM.getChild(tbody, 1);
    Element td = DOM.getChild(tr, 1);
    if (asHTML) {
      DOM.setInnerHTML(td, text);
    } else {
      DOM.setInnerText(td, text);
    }
  }

  /**
   * Shows the widget at the specified child index.
   * 
   * @param index the index of the child to be shown
   */
  public void showStack(int index) {
    if ((index >= getWidgetCount()) || (index < 0) || (index == visibleStack)) {
      return;
    }

    int oldIndex = visibleStack;
    if (visibleStack >= 0) {
      setStackVisible(visibleStack, false, false);
    }

    visibleStack = index;
    setStackVisible(visibleStack, true, false);
    if (stackAnimation == null) {
      stackAnimation = GWT.create(StackAnimation.class);
    }
    stackAnimation.showStack(this, oldIndex, index);
  }

  /**
   * <b>Affected Elements:</b>
   * <ul>
   * <li>-text# = The element around the header at the specified index.</li>
   * <li>-text-wrapper# = The element around the header at the specified index.</li>
   * <li>-content# = The element around the body at the specified index.</li>
   * </ul>
   * 
   * @see UIObject#onEnsureDebugId(String)
   */
  @Override
  protected void onEnsureDebugId(String baseID) {
    super.onEnsureDebugId(baseID);

    int numHeaders = DOM.getChildCount(body) >> 1;
    for (int i = 0; i < numHeaders; i++) {
      Element headerElem = DOM.getFirstChild(DOM.getChild(body, 2 * i));
      Element bodyElem = DOM.getFirstChild(DOM.getChild(body, 2 * i + 1));
      ensureDebugId(headerElem, baseID, "text-wrapper" + i);
      ensureDebugId(bodyElem, baseID, "content" + i);

      // Set the ID on the inner most container of the stack item
      Element table = DOM.getFirstChild(headerElem);
      Element tbody = DOM.getFirstChild(DOM.getFirstChild(headerElem));
      Element tr = DOM.getChild(tbody, 1);
      Element td = DOM.getChild(tr, 1);
      ensureDebugId(td, baseID, "text" + i);
    }
  }

  /**
   * @return a header element
   */
  private Element createHeaderElem() {
    // Create the table
    Element table = DOM.createTable();
    Element tbody = DOM.createTBody();
    DOM.appendChild(table, tbody);
    DOM.setStyleAttribute(table, "width", "100%");
    DOM.setElementPropertyInt(table, "cellSpacing", 0);
    DOM.setElementPropertyInt(table, "cellPadding", 0);

    // Add the top row
    DOM.appendChild(tbody, DecoratorPanel.createTR("top"));

    // Add the middle row
    DOM.appendChild(tbody, DecoratorPanel.createTR("middle"));

    // Return the table
    return table;
  }

  private int findDividerIndex(Element elem) {
    while (elem != getElement()) {
      String expando = DOM.getElementProperty(elem, "__index");
      if (expando != null) {
        // Make sure it belongs to me!
        int ownerHash = DOM.getElementPropertyInt(elem, "__owner");
        if (ownerHash == hashCode()) {
          // Yes, it's mine.
          return Integer.parseInt(expando);
        } else {
          // It must belong to some nested StackPanel.
          return -1;
        }
      }
      elem = DOM.getParent(elem);
    }
    return -1;
  }

  private boolean remove(Widget child, int index) {
    // Make sure to call this before disconnecting the DOM.
    boolean removed = super.remove(child);
    if (removed) {
      // Calculate which internal table elements to remove.
      int rowIndex = 2 * index;
      Element tr = DOM.getChild(body, rowIndex);
      DOM.removeChild(body, tr);
      tr = DOM.getChild(body, rowIndex);
      DOM.removeChild(body, tr);

      // Correct visible stack for new location.
      if (visibleStack == index) {
        visibleStack = -1;
      } else if (visibleStack > index) {
        --visibleStack;
      }

      // Update indices of all elements to the right.
      updateIndicesFrom(index);
    }
    return removed;
  }

  private void setStackContentVisible(int index, boolean visible) {
    Element tr = DOM.getChild(body, (index * 2) + 1);
    UIObject.setVisible(tr, visible);
    getWidget(index).setVisible(visible);
  }

  private void setStackVisible(int index, boolean visible, boolean render) {
    // Get the first table row containing the widget's selector item.
    Element tr = DOM.getChild(body, (index * 2));
    if (tr == null) {
      return;
    }

    // Style the stack selector item.
    Element td = DOM.getFirstChild(tr);
    setStyleName(td, DEFAULT_ITEM_STYLENAME + "-selected", visible);

    // Show/hide the contained widget.
    if (render) {
      setStackContentVisible(index, visible);
    }

    // Set the style of the next header
    Element trNext = DOM.getChild(body, ((index + 1) * 2));
    if (trNext != null) {
      Element tdNext = DOM.getFirstChild(trNext);
      Element divNext = DOM.getFirstChild(tdNext);
      setStyleName(tdNext, DEFAULT_ITEM_STYLENAME + "-below-selected", visible);
    }
  }

  private void updateIndicesFrom(int beforeIndex) {
    for (int i = beforeIndex, c = getWidgetCount(); i < c; ++i) {
      Element childTR = DOM.getChild(body, i * 2);
      Element childTD = DOM.getFirstChild(childTR);
      DOM.setElementPropertyInt(childTD, "__index", i);

      // Update the special style on the first element
      if (beforeIndex == 0) {
        setStyleName(childTD, DEFAULT_ITEM_STYLENAME + "-first", true);
      } else {
        setStyleName(childTD, DEFAULT_ITEM_STYLENAME + "-first", false);
      }
    }
  }

}
