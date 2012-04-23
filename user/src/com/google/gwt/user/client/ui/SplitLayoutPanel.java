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

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;

/**
 * A panel that adds user-positioned splitters between each of its child
 * widgets.
 *
 * <p>
 * This panel is used in the same way as {@link DockLayoutPanel}, except that
 * its children's sizes are always specified in {@link Unit#PX} units, and each
 * pair of child widgets has a splitter between them that the user can drag.
 * </p>
 *
 * <p>
 * This widget will <em>only</em> work in standards mode, which requires that
 * the HTML page in which it is run have an explicit &lt;!DOCTYPE&gt;
 * declaration.
 * </p>
 *
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-SplitLayoutPanel { the panel itself }</li>
 * <li>.gwt-SplitLayoutPanel .gwt-SplitLayoutPanel-HDragger { horizontal dragger
 * }</li>
 * <li>.gwt-SplitLayoutPanel .gwt-SplitLayoutPanel-VDragger { vertical dragger }
 * </li>
 * </ul>
 *
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.SplitLayoutPanelExample}
 * </p>
 */
public class SplitLayoutPanel extends DockLayoutPanel {

  class HSplitter extends Splitter {
    public HSplitter(Widget target, boolean reverse) {
      super(target, reverse);
      getElement().getStyle().setPropertyPx("width", splitterSize);
      setStyleName("gwt-SplitLayoutPanel-HDragger");
    }

    @Override
    protected int getAbsolutePosition() {
      return getAbsoluteLeft();
    }

    @Override
    protected double getCenterSize() {
      return getCenterWidth();
    }

    @Override
    protected int getEventPosition(Event event) {
      return event.getClientX();
    }

    @Override
    protected int getTargetPosition() {
      return target.getAbsoluteLeft();
    }

    @Override
    protected int getTargetSize() {
      return target.getOffsetWidth();
    }
  }

  abstract class Splitter extends Widget {
    protected final Widget target;

    private int offset;
    private boolean mouseDown;
    private ScheduledCommand layoutCommand;

    private final boolean reverse;
    private int minSize;
    private int snapClosedSize = -1;
    private double centerSize, syncedCenterSize;

    private boolean toggleDisplayAllowed = false;
    private double lastClick = 0;

    public Splitter(Widget target, boolean reverse) {
      this.target = target;
      this.reverse = reverse;

      setElement(Document.get().createDivElement());
      sinkEvents(Event.ONMOUSEDOWN | Event.ONMOUSEUP | Event.ONMOUSEMOVE
          | Event.ONDBLCLICK);
    }

    @Override
    public void onBrowserEvent(Event event) {
      switch (event.getTypeInt()) {
        case Event.ONMOUSEDOWN:
          mouseDown = true;

          /*
           * Resize glassElem to take up the entire scrollable window area,
           * which is the greater of the scroll size and the client size.
           */
          int width = Math.max(Window.getClientWidth(),
              Document.get().getScrollWidth());
          int height = Math.max(Window.getClientHeight(),
              Document.get().getScrollHeight());
          glassElem.getStyle().setHeight(height, Unit.PX);
          glassElem.getStyle().setWidth(width, Unit.PX);
          Document.get().getBody().appendChild(glassElem);

          offset = getEventPosition(event) - getAbsolutePosition();
          Event.setCapture(getElement());
          event.preventDefault();
          break;

        case Event.ONMOUSEUP:
          mouseDown = false;

          glassElem.removeFromParent();
          
          // Handle double-clicks.
          // Fake them since the double-click event aren't fired.
          if (this.toggleDisplayAllowed) {
            double now = Duration.currentTimeMillis();
            if (now - this.lastClick < DOUBLE_CLICK_TIMEOUT) {
              now = 0;
              LayoutData layout = (LayoutData) target.getLayoutData();
              if (layout.size == 0) {
                // Restore the old size.
                setAssociatedWidgetSize(layout.oldSize);
              } else {
                /*
                 * Collapse to size 0. We change the size instead of hiding the
                 * widget because hiding the widget can cause issues if the
                 * widget contains a flash component.
                 */
                layout.oldSize = layout.size;
                setAssociatedWidgetSize(0);
              }
            }
            this.lastClick = now;
          }

          Event.releaseCapture(getElement());
          event.preventDefault();
          break;

        case Event.ONMOUSEMOVE:
          if (mouseDown) {
            int size;
            if (reverse) {
              size = getTargetPosition() + getTargetSize()
                  - getEventPosition(event) - offset;
            } else {
              size = getEventPosition(event) - getTargetPosition() - offset;
            }
            ((LayoutData) target.getLayoutData()).hidden = false;
            setAssociatedWidgetSize(size);
            event.preventDefault();
          }
          break;
      }
    }

    public void setMinSize(int minSize) {
      this.minSize = minSize;
      LayoutData layout = (LayoutData) target.getLayoutData();

      // Try resetting the associated widget's size, which will enforce the new
      // minSize value.
      setAssociatedWidgetSize((int) layout.size);
    }

    public void setSnapClosedSize(int snapClosedSize) {
      this.snapClosedSize = snapClosedSize;
    }

    public void setToggleDisplayAllowed(boolean allowed) {
      this.toggleDisplayAllowed = allowed;
    }

    protected abstract int getAbsolutePosition();

    protected abstract double getCenterSize();

    protected abstract int getEventPosition(Event event);

    protected abstract int getTargetPosition();

    protected abstract int getTargetSize();

    private double getMaxSize() {
      // To avoid seeing stale center size values due to deferred layout
      // updates, maintain our own copy up to date and resync when the
      // DockLayoutPanel value changes.
      double newCenterSize = getCenterSize();
      if (syncedCenterSize != newCenterSize) {
        syncedCenterSize = newCenterSize;
        centerSize = newCenterSize;
      }

      return Math.max(((LayoutData) target.getLayoutData()).size + centerSize,
                0);
    }

    private void setAssociatedWidgetSize(double size) {
      double maxSize = getMaxSize();
      if (size > maxSize) {
        size = maxSize;
      }

      if (snapClosedSize > 0 && size < snapClosedSize) {
        size = 0;
      } else if (size < minSize) {
        size = minSize;
      }

      LayoutData layout = (LayoutData) target.getLayoutData();
      if (size == layout.size) {
        return;
      }

      // Adjust our view until the deferred layout gets scheduled.
      centerSize += layout.size - size;
      layout.size = size;

      // Defer actually updating the layout, so that if we receive many
      // mouse events before layout/paint occurs, we'll only update once.
      if (layoutCommand == null) {
        layoutCommand = new ScheduledCommand() {
          @Override
          public void execute() {
            layoutCommand = null;
            forceLayout();
          }
        };
        Scheduler.get().scheduleDeferred(layoutCommand);
      }
    }
  }

  class VSplitter extends Splitter {
    public VSplitter(Widget target, boolean reverse) {
      super(target, reverse);
      getElement().getStyle().setPropertyPx("height", splitterSize);
      setStyleName("gwt-SplitLayoutPanel-VDragger");
    }

    @Override
    protected int getAbsolutePosition() {
      return getAbsoluteTop();
    }

    @Override
    protected double getCenterSize() {
      return getCenterHeight();
    }

    @Override
    protected int getEventPosition(Event event) {
      return event.getClientY();
    }

    @Override
    protected int getTargetPosition() {
      return target.getAbsoluteTop();
    }

    @Override
    protected int getTargetSize() {
      return target.getOffsetHeight();
    }
  }

  private static final int DEFAULT_SPLITTER_SIZE = 8;
  private static final int DOUBLE_CLICK_TIMEOUT = 500;

  /**
   * The element that masks the screen so we can catch mouse events over
   * iframes.
   */
  private static Element glassElem = null;

  private final int splitterSize;

  /**
   * Construct a new {@link SplitLayoutPanel} with the default splitter size of
   * 8px.
   */
  public SplitLayoutPanel() {
    this(DEFAULT_SPLITTER_SIZE);
  }

  /**
   * Construct a new {@link SplitLayoutPanel} with the specified splitter size
   * in pixels.
   *
   * @param splitterSize the size of the splitter in pixels
   */
  public SplitLayoutPanel(int splitterSize) {
    super(Unit.PX);
    this.splitterSize = splitterSize;
    setStyleName("gwt-SplitLayoutPanel");

    if (glassElem == null) {
      glassElem = Document.get().createDivElement();
      glassElem.getStyle().setPosition(Position.ABSOLUTE);
      glassElem.getStyle().setTop(0, Unit.PX);
      glassElem.getStyle().setLeft(0, Unit.PX);
      glassElem.getStyle().setMargin(0, Unit.PX);
      glassElem.getStyle().setPadding(0, Unit.PX);
      glassElem.getStyle().setBorderWidth(0, Unit.PX);

      // We need to set the background color or mouse events will go right
      // through the glassElem. If the SplitPanel contains an iframe, the
      // iframe will capture the event and the slider will stop moving.
      glassElem.getStyle().setProperty("background", "white");
      glassElem.getStyle().setOpacity(0.0);
    }
  }

  /**
   * Return the size of the splitter in pixels.
   *
   * @return the splitter size
   */
  public int getSplitterSize() {
    return splitterSize;
  }

  @Override
  public void insert(Widget child, Direction direction, double size, Widget before) {
    super.insert(child, direction, size, before);
    if (direction != Direction.CENTER) {
      insertSplitter(child, before);
    }
  }

  @Override
  public boolean remove(Widget child) {
    assert !(child instanceof Splitter) : "Splitters may not be directly removed";

    int idx = getWidgetIndex(child);
    if (super.remove(child)) {
      // Remove the associated splitter, if any.
      // Now that the widget is removed, idx is the index of the splitter.
      if (idx < getWidgetCount()) {
        // Call super.remove(), or we'll end up recursing.
        super.remove(getWidget(idx));
      }
      return true;
    }
    return false;
  }

  /**
   * Sets the minimum allowable size for the given widget.
   *
   * <p>
   * Its associated splitter cannot be dragged to a position that would make it
   * smaller than this size. This method has no effect for the
   * {@link DockLayoutPanel.Direction#CENTER} widget.
   * </p>
   *
   * @param child the child whose minimum size will be set
   * @param minSize the minimum size for this widget
   */
  public void setWidgetMinSize(Widget child, int minSize) {
    assertIsChild(child);
    Splitter splitter = getAssociatedSplitter(child);
    // The splitter is null for the center element.
    if (splitter != null) {
      splitter.setMinSize(minSize);
    }
  }

  /**
   * Sets a size below which the slider will close completely. This can be used
   * in conjunction with {@link #setWidgetMinSize} to provide a speed-bump
   * effect where the slider will stick to a preferred minimum size before
   * closing completely.
   *
   * <p>
   * This method has no effect for the {@link DockLayoutPanel.Direction#CENTER}
   * widget.
   * </p>
   *
   * @param child the child whose slider should snap closed
   * @param snapClosedSize the width below which the widget will close or
   *        -1 to disable.
   */
  public void setWidgetSnapClosedSize(Widget child, int snapClosedSize) {
    assertIsChild(child);
    Splitter splitter = getAssociatedSplitter(child);
    // The splitter is null for the center element.
    if (splitter != null) {
      splitter.setSnapClosedSize(snapClosedSize);
    }
  }

  /**
   * Sets whether or not double-clicking on the splitter should toggle the
   * display of the widget.
   *
   * @param child the child whose display toggling will be allowed or not.
   * @param allowed whether or not display toggling is allowed for this widget
   */
  public void setWidgetToggleDisplayAllowed(Widget child, boolean allowed) {
    assertIsChild(child);
    Splitter splitter = getAssociatedSplitter(child);
    // The splitter is null for the center element.
    if (splitter != null) {
      splitter.setToggleDisplayAllowed(allowed);
    }
  }

  private Splitter getAssociatedSplitter(Widget child) {
    // If a widget has a next sibling, it must be a splitter, because the only
    // widget that *isn't* followed by a splitter must be the CENTER, which has
    // no associated splitter.
    int idx = getWidgetIndex(child);
    if (idx > -1 && idx < getWidgetCount() - 1) {
      Widget splitter = getWidget(idx + 1);
      assert splitter instanceof Splitter : "Expected child widget to be splitter";
      return (Splitter) splitter;
    }
    return null;
  }

  private void insertSplitter(Widget widget, Widget before) {
    assert getChildren().size() > 0 : "Can't add a splitter before any children";

    LayoutData layout = (LayoutData) widget.getLayoutData();
    Splitter splitter = null;
    switch (getResolvedDirection(layout.direction)) {
      case WEST:
        splitter = new HSplitter(widget, false);
        break;
      case EAST:
        splitter = new HSplitter(widget, true);
        break;
      case NORTH:
        splitter = new VSplitter(widget, false);
        break;
      case SOUTH:
        splitter = new VSplitter(widget, true);
        break;
      default:
        assert false : "Unexpected direction";
    }

    super.insert(splitter, layout.direction, splitterSize, before);
  }
}
