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
package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
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
          break;
        case Event.ONMOUSEUP:
          if (maybeClose && isCloseBoxEvent(event)) {
            maybeClose = false;
            hide();
            return;
          }
          break;
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
    // Create a few extra dialog boxes
    final DialogBox dialog0 = showCustomDialog(true, true, false, "Dialog 0",
        "I cannot be dragged or closed until Dialog 2 is closed", 0, 100);
    final DialogBox dialog1 = showCustomDialog(true, false, false, "Dialog 1",
        "I cannot be dragged or closed until Dialog 2 is closed", 0, 200);
    final DialogBox dialog2 = showCustomDialog(false, false, true, "Dialog 2",
        "I can be dragged", 0, 300);
    final DialogBox dialog3 = showCustomDialog(
        true,
        false,
        false,
        "Dialog 3",
        "I should auto close as soon as you click outside of me and Dialog 4 or greater",
        0, 400);
    final DialogBox dialog4 = showCustomDialog(false, false, false, "Dialog 4",
        "I can be dragged", 0, 500);

    final VisibleDialogBox dialog = showVisibleDialog();

    SimplePanel panel = new SimplePanel() {
      @Override
      protected void onUnload() {
        if (dialog.isAttached()) {
          dialog.hide();
          dialog0.hide();
          dialog1.hide();
          dialog2.hide();
          dialog3.hide();
          dialog4.hide();
        }
      }
    };
    return panel;
  }

  @Override
  public String getInstructions() {
    return "Confirm color change on mouse over caption, that the "
        + "custom close box works, and that each mouse event fires.  "
        + "Verify that the text in each DialogBox is true.";
  }

  @Override
  public String getSummary() {
    return "Legacy mouse event callbacks fire";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

  private DialogBox showCustomDialog(boolean autoHide,
      boolean previewAllEvents, boolean modal, String caption, String message,
      int left, int top) {
    final DialogBox dialog = new DialogBox(autoHide, modal);
    dialog.setPreviewingAllNativeEvents(previewAllEvents);

    // Set the caption
    caption += " (autoHide=" + dialog.isAutoHideEnabled();
    caption += ", previewAllEvents=" + dialog.isPreviewingAllNativeEvents();
    caption += ", modal=" + dialog.isModal() + ")";
    dialog.setText(caption);

    // Set the content
    Label content = new Label(message);
    if (autoHide || previewAllEvents) {
      dialog.setWidget(content);
    } else {
      VerticalPanel vPanel = new VerticalPanel();
      vPanel.add(content);
      vPanel.add(new Button("Close", new ClickHandler() {
        public void onClick(ClickEvent event) {
          dialog.hide();
        }
      }));
      dialog.setWidget(vPanel);
    }
    dialog.setPopupPosition(left, top);
    dialog.show();
    return dialog;
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
