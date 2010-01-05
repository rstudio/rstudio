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
package com.google.gwt.reference.microbenchmark.client;

import com.google.gwt.core.client.Duration;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import java.util.Date;

/**
 * Compares various widget creation strategies.
 */
public class WidgetCreation implements Microbenchmark {
  static abstract class Maker {
    final String name;

    Maker(String name) {
      this.name = name;
    }

    abstract Widget make();
  }

  private static final String COOKIE = "gwt_microb_widgetCreation";

  private static final int DEFAULT_INSTANCES = 100;

  public static native void log(String msg) /*-{
    var logger = $wnd.console;
    if(logger && logger.markTimeline) {
      logger.markTimeline(msg); 
    }
  }-*/;

  final Grid grid;

  final TextBox number;
  final Maker[] makers = {
      new Maker("SimplePanel") {
        public Widget make() {
          return new SimplePanel();
        }
      }, new Maker("FlowPanel") {
        public Widget make() {
          return new FlowPanel();
        }
      }, new Maker("HTMLPanel") {
        public Widget make() {
          return new HTMLPanel("");
        }
      }, new EmptyBinder.Maker(), new TestDom.Maker(),
      new TestCursorDomCrawl.Maker(), new TestRealisticDomCrawl.Maker(),
      new TestDomBinder.Maker(), new TestFlows.Maker(), new TestWidget.Maker(),
      new TestWidgetBinder.Maker()};

  final private FlowPanel root;

  public WidgetCreation() {
    int instances = DEFAULT_INSTANCES;
    try {
      instances = Integer.parseInt(Cookies.getCookie(COOKIE));
    } catch (NumberFormatException ignored) {
    }

    number = new TextBox();
    number.setVisibleLength(7);
    number.setValue("" + instances);
    number.addBlurHandler(new BlurHandler() {
      public void onBlur(BlurEvent event) {
        saveInstances();
      }
    });

    Window.addWindowClosingHandler(new ClosingHandler() {
      public void onWindowClosing(ClosingEvent event) {
        saveInstances();
      }
    });

    grid = new Grid(makers.length + 1, 3);
    grid.setText(0, 0, "50%");
    grid.setText(0, 1, "m");

    int row = 1;
    for (Maker m : makers) {
      grid.setText(row, 0, "0");
      grid.setText(row, 1, "0");
      grid.setText(row, 2, m.name);
      row++;
    }

    root = new FlowPanel();
    HTMLPanel l = new HTMLPanel(
        "<b>Time for creating, attaching and detaching "
            + "<span id='number'></span> instances, in MS<br><br></b>");
    l.addAndReplaceElement(number, "number");
    root.add(l);
    root.add(grid);
  }

  public String getName() {
    return "Widget Creation Survey";
  }

  public Widget getWidget() {
    return root;
  }

  public void run() {
    RootPanel r = RootPanel.get();

    Widget[] widgets = new Widget[getInstances()];

    grid.resizeColumns(grid.getColumnCount() + 1);

    int row = 1;
    for (Maker maker : makers) {
      log(maker.name);
      double start = Duration.currentTimeMillis();

      for (int i = 0; i < getInstances(); ++i) {
        widgets[i] = maker.make();
        r.add(widgets[i]);
      }

      /*
       * Force a layout by finding the body's offsetTop. We avoid doing
       * setTimeout(0), which would allow paint to happen, to keep the test
       * synchronous and because different browsers round that zero to different
       * minimums. Layout should be the bulk of the time.
       */
      Document.get().getBody().getOffsetTop();

      double thisTime = Duration.currentTimeMillis() - start;
      record(row, thisTime);

      // Clean up to keep the dom a reasonable size.
      
      for (int i = 0; i < getInstances(); ++i) {
        r.remove(widgets[i]);
      }
      row++;
    }
  }

  private String format(double median) {
    return NumberFormat.getFormat("0").format(median);
  }

  private int getInstances() {
    try {
      int instances = Integer.parseInt(number.getValue());
      return instances;
    } catch (NumberFormatException ignored) {
      return 0;
    }
  }

  private void record(int row, double thisTime) {
    final int columns = grid.getColumnCount();
    grid.setText(row, columns - 1, format(thisTime));

    double max = 0, min = 0, mean = 0;

    for (int column = 3; column < columns; column++) {
      double value = Double.parseDouble(grid.getText(row, column));
      mean += value;
      max = Math.max(max, value);
      if (min == 0) {
        min = max;
      } else {
        min = Math.min(min, value);
      }
    }

    double range = max - min;
    double halfRange = range / 2;
    double median = min + halfRange;
    grid.setText(row, 0, format(median));

    mean = mean / (columns - 3);
    grid.setText(row, 1, format(mean));
  }

  @SuppressWarnings("deprecation")
  private void saveInstances() {
    String value = number.getValue();
    Date expires = new Date();
    expires.setYear(expires.getYear() + 3);
    Cookies.setCookie(COOKIE, value, expires);
  }
}
