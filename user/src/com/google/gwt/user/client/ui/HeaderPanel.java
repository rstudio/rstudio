/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.FiniteWidgetIterator.WidgetProvider;

import java.util.Iterator;

/**
 * A panel that includes a header (top), footer (bottom), and content (middle)
 * area. The header and footer areas resize naturally. The content area is
 * allocated all of the remaining space between the header and footer area.
 */
public class HeaderPanel extends Panel implements RequiresResize {

  /**
   * The widget provider for this panel.
   * 
   * <p>
   * Widgets are returned in the following order:
   * <ol>
   * <li>Header widget</li>
   * <li>Content widget</li>
   * <li>Footer widget</li>
   * </ol>
   */
  private class WidgetProviderImpl implements WidgetProvider {

    public Widget get(int index) {
      switch (index) {
        case 0:
          return header;
        case 1:
          return content;
        case 2:
          return footer;
      }
      throw new ArrayIndexOutOfBoundsException(index);
    }
  }

  private Widget content;
  private final Element contentContainer;
  private Widget footer;
  private final Element footerContainer;
  private final ResizeLayoutPanel.Impl footerImpl = GWT.create(ResizeLayoutPanel.Impl.class);
  private Widget header;
  private final Element headerContainer;
  private final ResizeLayoutPanel.Impl headerImpl = GWT.create(ResizeLayoutPanel.Impl.class);
  private final ScheduledCommand layoutCmd = new ScheduledCommand() {
    public void execute() {
      layoutScheduled = false;
      forceLayout();
    }
  };
  private boolean layoutScheduled = false;

  public HeaderPanel() {
    // Create the outer element
    Element elem = Document.get().createDivElement().cast();
    elem.getStyle().setPosition(Position.RELATIVE);
    elem.getStyle().setOverflow(Overflow.HIDDEN);
    setElement(elem);

    // Create a delegate to handle resize from the header and footer.
    ResizeLayoutPanel.Impl.Delegate resizeDelegate = new ResizeLayoutPanel.Impl.Delegate() {
      public void onResize() {
        scheduledLayout();
      }
    };

    // Create the header container.
    headerContainer = createContainer();
    headerContainer.getStyle().setTop(0.0, Unit.PX);
    headerImpl.init(headerContainer, resizeDelegate);
    elem.appendChild(headerContainer);

    // Create the footer container.
    footerContainer = createContainer();
    footerContainer.getStyle().setBottom(0.0, Unit.PX);
    footerImpl.init(footerContainer, resizeDelegate);
    elem.appendChild(footerContainer);

    // Create the content container.
    contentContainer = createContainer();
    contentContainer.getStyle().setOverflow(Overflow.HIDDEN);
    contentContainer.getStyle().setTop(0.0, Unit.PX);
    contentContainer.getStyle().setHeight(0.0, Unit.PX);
    elem.appendChild(contentContainer);
  }

  /**
   * Adds a widget to this panel.
   * 
   * @param w the child widget to be added
   */
  @Override
  public void add(Widget w) {
    // Add widgets in the order that they appear.
    if (header == null) {
      setHeaderWidget(w);
    } else if (content == null) {
      setContentWidget(w);
    } else if (footer == null) {
      setFooterWidget(w);
    } else {
      throw new IllegalStateException(
          "HeaderPanel already contains header, content, and footer widgets.");
    }
  }

  /**
   * Get the content widget that appears between the header and footer.
   * 
   * @return the content {@link Widget}
   */
  public Widget getContentWidget() {
    return content;
  }

  /**
   * Get the footer widget at the bottom of the panel.
   * 
   * @return the footer {@link Widget}
   */
  public Widget getFooterWidget() {
    return footer;
  }

  /**
   * Get the header widget at the top of the panel.
   * 
   * @return the header {@link Widget}
   */
  public Widget getHeaderWidget() {
    return header;
  }

  public Iterator<Widget> iterator() {
    return new FiniteWidgetIterator(new WidgetProviderImpl(), 3);
  }

  @Override
  public void onAttach() {
    super.onAttach();
    headerImpl.onAttach();
    footerImpl.onAttach();
    scheduledLayout();
  }

  @Override
  public void onDetach() {
    super.onDetach();
    headerImpl.onDetach();
    footerImpl.onDetach();
  }

  public void onResize() {
    // Handle the outer element resizing.
    scheduledLayout();
  }

  @Override
  public boolean remove(Widget w) {
    // Validate.
    if (w.getParent() != this) {
      return false;
    }
    // Orphan.
    try {
      orphan(w);
    } finally {
      // Physical detach.
      w.getElement().removeFromParent();

      // Logical detach.
      if (w == content) {
        content = null;
        contentContainer.getStyle().setDisplay(Display.NONE);
      } else if (w == header) {
        header = null;
        headerContainer.getStyle().setDisplay(Display.NONE);
      } else if (w == footer) {
        footer = null;
        footerContainer.getStyle().setDisplay(Display.NONE);
      }
    }
    return true;
  }

  /**
   * Set the widget in the content portion between the header and footer.
   * 
   * @param w the widget to use as the content
   */
  public void setContentWidget(Widget w) {
    contentContainer.getStyle().clearDisplay();
    add(w, content, contentContainer);

    // Logical attach.
    content = w;
    scheduledLayout();
  }

  /**
   * Set the widget in the footer portion at the bottom of the panel.
   * 
   * @param w the widget to use as the footer
   */
  public void setFooterWidget(Widget w) {
    footerContainer.getStyle().clearDisplay();
    add(w, footer, footerContainer);

    // Logical attach.
    footer = w;
    scheduledLayout();
  }

  /**
   * Set the widget in the header portion at the top of the panel.
   * 
   * @param w the widget to use as the header
   */
  public void setHeaderWidget(Widget w) {
    headerContainer.getStyle().clearDisplay();
    add(w, header, headerContainer);

    // Logical attach.
    header = w;
    scheduledLayout();
  }

  /**
   * Add a widget to the panel in the specified container. Note that this method
   * does not do the logical attach.
   * 
   * @param w the widget to add
   * @param toReplace the widget to replace
   * @param container the container in which to place the widget
   */
  private void add(Widget w, Widget toReplace, Element container) {
    // Validate.
    if (w == toReplace) {
      return;
    }

    // Detach new child.
    if (w != null) {
      w.removeFromParent();
    }

    // Remove old child.
    if (toReplace != null) {
      remove(toReplace);
    }

    if (w != null) {
      // Physical attach.
      container.appendChild(w.getElement());

      adopt(w);
    }
  }

  private Element createContainer() {
    Element container = Document.get().createDivElement().cast();
    container.getStyle().setPosition(Position.ABSOLUTE);
    container.getStyle().setDisplay(Display.NONE);
    container.getStyle().setLeft(0.0, Unit.PX);
    container.getStyle().setWidth(100.0, Unit.PCT);
    return container;
  }

  /**
   * Update the layout.
   */
  private void forceLayout() {
    // No sense in doing layout if we aren't attached or have no content.
    if (!isAttached() || content == null) {
      return;
    }

    // Resize the content area to fit between the header and footer.
    int remainingHeight = getElement().getClientHeight();
    if (header != null) {
      int height = Math.max(0, headerContainer.getOffsetHeight());
      remainingHeight -= height;
      contentContainer.getStyle().setTop(height, Unit.PX);
    } else {
      contentContainer.getStyle().setTop(0.0, Unit.PX);
    }
    if (footer != null) {
      remainingHeight -= footerContainer.getOffsetHeight();
    }
    contentContainer.getStyle().setHeight(Math.max(0, remainingHeight), Unit.PX);

    // Provide resize to child.
    if (content instanceof RequiresResize) {
      ((RequiresResize) content).onResize();
    }
  }

  /**
   * Schedule layout to adjust the height of the content area.
   */
  private void scheduledLayout() {
    if (isAttached() && !layoutScheduled) {
      layoutScheduled = true;
      Scheduler.get().scheduleDeferred(layoutCmd);
    }
  }
}
