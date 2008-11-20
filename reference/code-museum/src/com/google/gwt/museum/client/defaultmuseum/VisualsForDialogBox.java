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
package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.HashMap;
import java.util.Map;

/**
 * Verify that events fire in all browsers.
 */
public class VisualsForDialogBox extends AbstractIssue {

  enum VisibleEvents {
    mouseDown, mouseEnter, mouseLeave, mouseMove, mouseUp, captionMouseDown
  }

  private final class VisibleDialogBox extends DialogBox {
    private FlexTable layout = null;

    private final Map<VisibleEvents, Element> eventToElement = new HashMap<VisibleEvents, Element>();

    private boolean maybeClose;

    public VisibleDialogBox() {
      this(false);
    }

    public VisibleDialogBox(boolean autoHide) {
      this(autoHide, true);
    }

    public VisibleDialogBox(boolean autoHide, boolean modal) {
      super(autoHide, modal);
      layout = new FlexTable();
      layout.setCellPadding(3);
      layout.setBorderWidth(2);
      layout.setHTML(0, 0, "<b>VisibleEvents</b>");
      layout.setHTML(0, 1, "<b>Status</b>");

      final String style = "float:right; border: 1px solid blue; color:blue;"
          + "font-weight:bold; font-size:85%";
      setHTML("I Gots a Close Box<div id='vis-closebox' style='" + style
          + "'>&nbsp;X&nbsp;</div>");

      for (VisibleEvents e : VisibleEvents.values()) {
        eventToElement.put(e, addResultRow(e.name()));
      }
      add(layout);
    }

    @Override
    public void onBrowserEvent(Event event) {
      switch (event.getTypeInt()) {
        case Event.ONMOUSEDOWN:
          if (isCloseBoxEvent(event)) {
            maybeClose = true;
            return;
          }
        case Event.ONMOUSEUP:
          if (maybeClose && isCloseBoxEvent(event)) {
            maybeClose = false;
            hide();
            return;
          }
      }
      maybeClose = false;
      super.onBrowserEvent(event);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onMouseDown(Widget sender, int x, int y) {
      pass(VisibleEvents.mouseDown);
      super.onMouseDown(sender, x, y);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onMouseEnter(Widget sender) {
      pass(VisibleEvents.mouseEnter);
      sender.getElement().getStyle().setProperty("background", "yellow");
      super.onMouseEnter(sender);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onMouseLeave(Widget sender) {
      pass(VisibleEvents.mouseLeave);
      sender.getElement().getStyle().setProperty("background", "");
      super.onMouseLeave(sender);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onMouseMove(Widget sender, int x, int y) {
      pass(VisibleEvents.mouseMove);
      super.onMouseMove(sender, x, y);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onMouseUp(Widget sender, int x, int y) {
      pass(VisibleEvents.mouseUp);
      super.onMouseUp(sender, x, y);
    }

    public void pass(VisibleEvents event) {
      eventToElement.get(event).setInnerHTML(
          "<span style='color:green'>pass</span>");
    }

    private Element addResultRow(String eventName) {
      int row = layout.getRowCount();
      layout.setHTML(row, 0, eventName);
      layout.setHTML(row, 1, "<span style='color:red'>?</span>");
      Element cell = layout.getCellFormatter().getElement(row, 1);
      return cell;
    }

    private boolean isCloseBoxEvent(Event event) {
      return Document.get().getElementById("vis-closebox").isOrHasChild(
          event.getTarget());
    }
  }

  @Override
  public Widget createIssue() {
    final VisibleDialogBox dialog = showVisibleDialog();

    SimplePanel panel = new SimplePanel() {
      @Override
      protected void onUnload() {
        dialog.hide();
      }
    };

    return panel;
  }

  @Override
  public String getInstructions() {
    return "Confirm color change on mouse over caption, that the "
        + "custom close box works, and that each mouse event fires.";
  }

  @Override
  public String getSummary() {
    return "Legacy mouse event callbacks fire";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

  private VisibleDialogBox showVisibleDialog() {
    final VisibleDialogBox dialog = new VisibleDialogBox();
    dialog.setModal(false);
    dialog.center();
    dialog.getCaption().addMouseDownHandler(new MouseDownHandler() {
      public void onMouseDown(MouseDownEvent event) {
        dialog.pass(VisibleEvents.captionMouseDown);
      }
    });

    return dialog;
  }
}
