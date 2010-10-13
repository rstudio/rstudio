/*
 * Copyright 2010 Google Inc.
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

package com.google.gwt.logging.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasAllMouseHandlers;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A simple popup to show log messages, which can be resized, minimized, and
 * dragged to a different location.
 */
public class LoggingPopup extends PopupPanel {
  
  /**
   * Handles the logic to track click-drag movements with the mouse.
   */
  private abstract class MouseDragHandler implements MouseMoveHandler, 
  MouseUpHandler, MouseDownHandler {
    protected boolean dragging = false;
    protected Widget dragHandle;
    protected int dragStartX;
    protected int dragStartY;

    public MouseDragHandler(Widget dragHandle) {
      this.dragHandle = dragHandle;
      HasAllMouseHandlers hamh = (HasAllMouseHandlers) dragHandle;
      hamh.addMouseDownHandler(this);
      hamh.addMouseUpHandler(this);
      hamh.addMouseMoveHandler(this);
    }

    public abstract void handleDrag(int absX, int absY);

    public void onMouseDown(MouseDownEvent event) {
      dragging = true;
      DOM.setCapture(dragHandle.getElement());
      dragStartX = event.getClientX();
      dragStartY = event.getClientY();
      DOM.eventPreventDefault(DOM.eventGetCurrentEvent());
    }
 
    public void onMouseMove(MouseMoveEvent event) {
      if (dragging) {
        handleDrag(event.getClientX() - dragStartX,
            event.getClientY() - dragStartY);
        dragStartX = event.getClientX();
        dragStartY = event.getClientY();
      }
    }
    
    public void onMouseUp(MouseUpEvent event) {
      dragging = false;
      DOM.releaseCapture(dragHandle.getElement());
    }
  }
  
  private static class ScrollPanelWithMinSize extends ScrollPanel {
    private int minScrollPanelHeight = 100;
    private int minScrollPanelWidth = 100;
    private int scrollPanelHeight;
    private int scrollPanelWidth;
 
    public void incrementPixelSize(int width, int height) {
      setPixelSize(scrollPanelWidth + width, scrollPanelHeight + height);
    }

    @Override
    public void setPixelSize(int width, int height) {
      super.setPixelSize(scrollPanelWidth = Math.max(width, minScrollPanelWidth),
          scrollPanelHeight = Math.max(height, minScrollPanelHeight));
    }
  }
  
  private class WindowMoveHandler extends MouseDragHandler {
    public WindowMoveHandler(Widget dragHandle) {
      super(dragHandle);
    }

    @Override
    public void handleDrag(int absX, int absY) {
      Widget moveTarget = LoggingPopup.this;
      RootPanel.get().setWidgetPosition(moveTarget,
          moveTarget.getAbsoluteLeft() + absX,
          moveTarget.getAbsoluteTop() + absY);      
    }
  }
  
  private class WindowResizeHandler extends MouseDragHandler {
    public WindowResizeHandler(Widget dragHandle) {
      super(dragHandle);
    }

    @Override
    public void handleDrag(int absX, int absY) {
      scrollPanel.incrementPixelSize(absX, absY);
    }
  }

  private final HTML resizeIcon;
  private final ScrollPanelWithMinSize scrollPanel;
  private VerticalPanel logArea;
  
  public LoggingPopup() {
    // Since we don't want to pull UiBinder, or style sheets into the core GWT
    // library, styling for this window is done pretty manually.
    super(false, false);
    VerticalPanel mainPanel = new VerticalPanel();
    mainPanel.setBorderWidth(1);
    mainPanel.getElement().getStyle().setBackgroundColor("white");
    
    final HTML titleBar = new HTML("<center><b>Logging</b></center>");
    mainPanel.add(titleBar);
    new WindowMoveHandler(titleBar);

    scrollPanel = new ScrollPanelWithMinSize();
    mainPanel.add(scrollPanel);
    logArea = new VerticalPanel();
    scrollPanel.setWidget(logArea);
    scrollPanel.setPixelSize(300, 200);
    
    HorizontalPanel bottomBar = new HorizontalPanel();
    mainPanel.add(bottomBar);
    bottomBar.setWidth("100%");
    bottomBar.setVerticalAlignment(HasVerticalAlignment.ALIGN_BOTTOM);

    final Button maxmin = new Button("Minimize");
    bottomBar.add(maxmin);
    maxmin.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        if (maxmin.getText().equals("Minimize")) {
          maxmin.setText("Maximize");
          scrollPanel.setVisible(false);
          resizeIcon.setVisible(false);
        } else {
          scrollPanel.setVisible(true);
          resizeIcon.setVisible(true);
          maxmin.setText("Minimize");
        }
      }
    });

    resizeIcon =
      new HTML("<div style='font-size:200%; line-height:75%'>\u21F2</div>");
    resizeIcon.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
    bottomBar.add(resizeIcon);
    new WindowResizeHandler(resizeIcon);
    
    super.setWidget(mainPanel);
    show();
  }
  
  @Override
  public void add(Widget w) {
    logArea.add(w);
    scrollPanel.setScrollPosition(scrollPanel.getElement().getScrollHeight());
  }
  
  @Override
  public void setWidget(Widget w) {
    logArea.clear();
    add(w);
  }
}
