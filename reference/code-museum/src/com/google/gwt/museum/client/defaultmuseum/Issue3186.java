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

import com.google.gwt.dom.client.Element;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.MouseListenerCollection;
import com.google.gwt.user.client.ui.Widget;

import java.util.HashMap;
import java.util.Map;

/**
 * Ensure the ListenerWrapper for mouse still works like the old listeners did.
 */
@SuppressWarnings("deprecation")
public class Issue3186 extends AbstractIssue {

  enum VisibleEvents {
    mouseDown, mouseEnter, mouseLeave, mouseMove, mouseUp
  }

  private final class TestWidget extends FocusPanel {
    private class Control implements MouseListener {
      int controlX;
      int controlY;
      int controlMouseEnter;
      int controlMouseLeave;

      public void onMouseDown(Widget sender, int x, int y) {
        this.controlX = x;
        this.controlY = y;
      }

      public void onMouseEnter(Widget sender) {
        ++controlMouseEnter;
      }

      public void onMouseLeave(Widget sender) {
        ++controlMouseLeave;
      }

      public void onMouseMove(Widget sender, int x, int y) {
        this.controlX = x;
        this.controlY = y;
      }

      public void onMouseUp(Widget sender, int x, int y) {
        this.controlX = x;
        this.controlY = y;
      }
    }

    private class Current implements MouseListener {
      private int mouseEnterCount;
      private int mouseLeaveCount;

      public void onMouseDown(Widget sender, int x, int y) {
        check(x, y, VisibleEvents.mouseDown);
      }

      public void onMouseEnter(Widget sender) {
        ++mouseEnterCount;
        if (mouseEnterCount != control.controlMouseEnter) {
          fail("recieved:" + mouseEnterCount + " events, expected:"
              + control.controlMouseEnter, VisibleEvents.mouseEnter);
        } else {
          pass(VisibleEvents.mouseEnter);
        }
        sender.getElement().getStyle().setProperty("background", "yellow");
      }

      public void onMouseLeave(Widget sender) {
        ++mouseLeaveCount;
        if (mouseLeaveCount != control.controlMouseLeave) {
          fail("recieved:" + mouseLeaveCount + " events, expected:"
              + control.controlMouseLeave, VisibleEvents.mouseLeave);
        } else {
          pass(VisibleEvents.mouseLeave);
        }

        sender.getElement().getStyle().setProperty("background", "");
      }

      public void onMouseMove(Widget sender, int x, int y) {
        check(x, y, VisibleEvents.mouseMove);
      }

      public void onMouseUp(Widget sender, int x, int y) {
        check(x, y, VisibleEvents.mouseUp);
      }

      private void check(int x, int y, VisibleEvents event) {
        String errorReport = getErrorReport(x, y);
        if (errorReport == null) {
          eventToElement.get(event).setInnerHTML(
              "<span style='color:green'>pass</span>");
        } else {
          fail(errorReport, event);
        }
      }

      private String getErrorReport(int x, int y) {
        String errorReport = null;
        if (x != control.controlX) {
          errorReport = "wanted x: " + control.controlX + " actual x" + x;
        } else if (y != control.controlY) {
          errorReport += "wanted y: " + control.controlY + " actual y" + y;
        }
        return errorReport;
      }
    }

    private FlexTable layout = null;
    private MouseListenerCollection collection = new MouseListenerCollection();

    private Control control = new Control();
    private Current current = new Current();
    private final Map<VisibleEvents, Element> eventToElement = new HashMap<VisibleEvents, Element>();

    public TestWidget() {
      layout = new FlexTable();
      layout.setCellPadding(3);
      layout.setBorderWidth(2);

      layout.setHTML(0, 0, "<b>MouseEvents</b>");
      layout.setHTML(0, 1, "<b>Status</b>");

      for (VisibleEvents e : VisibleEvents.values()) {
        eventToElement.put(e, addResultRow(e.name()));
      }
      add(layout);
      this.addMouseListener(current);
      collection.add(control);
    }

    public void fail(String errorReport, VisibleEvents event) {
      eventToElement.get(event).setInnerHTML(
          "<span style='color:red'>" + errorReport + "</span>");
    }

    @Override
    public void onBrowserEvent(Event event) {
      collection.fireMouseEvent(this, event);
      super.onBrowserEvent(event);
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
  }

  @Override
  public Widget createIssue() {
    AbsolutePanel p = new AbsolutePanel();
    p.setHeight("500px");
    p.setWidth("500px");
    final TestWidget dialog = showTestWidget();
    p.add(dialog, 100, 100);
    return p;
  }

  @Override
  public String getInstructions() {
    return "move your mouse around ";
  }

  @Override
  public String getSummary() {
    return "mouse listeners work the same";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

  private TestWidget showTestWidget() {
    final TestWidget dialog = new TestWidget();
    return dialog;
  }

}
