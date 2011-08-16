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
package com.google.gwt.reference.microbenchmark.client;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * An implementation of {@link Microbenchmark} that surveys multiple timed
 * tests.
 */
public class MicrobenchmarkSurvey implements Microbenchmark {

  /**
   * A single runnable test that makes up the survey.
   */
  static abstract class NanoTest {

    private final String name;

    /**
     * Construct a new {@link NanoTest}.
     * 
     * @param name the display name
     */
    public NanoTest(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    /**
     * Get the widget to display in a popup when the user clicks on the test
     * name.
     * 
     * @return the popup widget, or null not to show one
     */
    public Widget getPopup() {
      return null;
    }

    /**
     * Run the test.
     */
    public abstract void runTest();

    /**
     * Setup the test before starting the timer. Override this method to prepare
     * the test before it starts running.
     */
    public void setup() {
      // No-op by default.
    }

    /**
     * Tear down the test after stopping the timer. Override this method to
     * cleanup the test after it completes.
     */
    public void teardown() {
      // No-op by default.
    }
  }

  /**
   * A nano test that makes a widget and attaches it to the {@link RootPanel}.
   */
  static abstract class WidgetMaker extends NanoTest {

    private final RootPanel root = RootPanel.get();
    private Widget popupWidget;
    private Widget w;

    public WidgetMaker(String name) {
      super(name);
    }

    @Override
    public Widget getPopup() {
      if (popupWidget == null) {
        popupWidget = make();
      }
      return popupWidget;
    }

    @Override
    public void runTest() {
      w = make();
      root.add(w);

      /*
       * Force a layout by finding the body's offsetTop and height. We avoid
       * doing setTimeout(0), which would allow paint to happen, to keep the
       * test synchronous and because different browsers round that zero to
       * different minimums. Layout should be the bulk of the time.
       */
      Document.get().getBody().getOffsetTop();
      Document.get().getBody().getOffsetHeight();
      w.getOffsetHeight();
    }

    @Override
    public void teardown() {
      // Clean up to keep the dom. Attached widgets will affect later tests.
      root.remove(w);
    }

    /**
     * Make the widget to test.
     * 
     * @return the widget
     */
    protected abstract Widget make();
  }

  /**
   * A nano test that updates an existing widget that is already attached to the
   * {@link RootPanel}.
   * 
   * @param <W> the widget type
   */
  static abstract class WidgetUpdater<W extends Widget> extends MicrobenchmarkSurvey.NanoTest {

    private final RootPanel root = RootPanel.get();
    private W w;

    public WidgetUpdater(String name) {
      super(name);
    }

    @Override
    public Widget getPopup() {
      return ensureWidget();
    }

    @Override
    public void setup() {
      root.add(ensureWidget());
    }

    @Override
    public void runTest() {
      updateWidget(w);
    }

    @Override
    public void teardown() {
      root.remove(w);
    }

    /**
     * Make the widget to test.
     * 
     * @return the widget
     */
    protected abstract W make();

    /**
     * Update the widget.
     * 
     * @param w the widget to update
     */
    protected abstract void updateWidget(W w);

    private W ensureWidget() {
      if (w == null) {
        w = make();
      }
      return w;
    }
  }

  interface Binder extends UiBinder<Widget, MicrobenchmarkSurvey> {
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  private static final String COOKIE = "gwt_microb_survey";

  private static final int DEFAULT_INSTANCES = 100;

  public static native void log(String msg) /*-{
    var logger = $wnd.console;
    if (logger) {
      logger.log(msg);
      if (logger.markTimeline) {
        logger.markTimeline(msg);
      }
    }
  }-*/;

  @UiField(provided = true)
  Grid grid;
  @UiField
  CheckBox includeLargeWidget;
  @UiField
  TextBox number;
  @UiField
  Widget root;
  final String name;
  private final List<NanoTest> nanos;

  /**
   * Construct a new {@link MicrobenchmarkSurvey} micro benchmark.
   * 
   * @param name the name of the benchmark
   * @param nanos the {@link NanoTest}s that make up the survey
   */
  public MicrobenchmarkSurvey(String name, List<NanoTest> nanos) {
    this.name = name;
    this.nanos = Collections.unmodifiableList(nanos);

    int instances = DEFAULT_INSTANCES;
    try {
      instances = Integer.parseInt(Cookies.getCookie(COOKIE));
    } catch (NumberFormatException ignored) {
    }

    // Initialize the grid.
    grid = new Grid(nanos.size() + 2, 3);
    grid.setText(0, 0, "median");
    grid.setText(0, 1, "mean");

    int row = 1;
    for (final NanoTest nano : nanos) {
      grid.setText(row, 0, "0");
      grid.setText(row, 1, "0");
      InlineLabel a = new InlineLabel();
      a.setText(nano.getName());
      a.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          Widget toDisplay = nano.getPopup();
          if (toDisplay != null) {
            PopupPanel popup = new PopupPanel(true, true);
            ScrollPanel container = new ScrollPanel(toDisplay);
            container.setPixelSize(500, 500);
            popup.setWidget(container);
            popup.center();
          }
        }
      });
      // TODO: popup.
      grid.setWidget(row, 2, a);
      row++;
    }

    // Create the widget.
    root = BINDER.createAndBindUi(this);
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
  }

  public String getName() {
    return name;
  }

  public Widget getWidget() {
    return root;
  }

  public void run() {
    RootPanel root = RootPanel.get();

    // Add a large widget to the root to reflect a typical application.
    FlowPanel largeWidget = null;
    if (includeLargeWidget.getValue()) {
      largeWidget = new FlowPanel();
      TestWidgetBinder.Maker widgetMaker = new TestWidgetBinder.Maker();
      for (int i = 0; i < 100; i++) {
        largeWidget.add(widgetMaker.make());
      }
      root.add(largeWidget);
    }

    int nanosCount = nanos.size();
    double[] times = new double[nanosCount];

    int column = grid.getColumnCount();
    grid.resizeColumns(column + 1);
    grid.setText(0, column, "Run " + (column - 3));

    final int instances = getInstances();
    boolean forward = false;
    for (int i = 0; i < instances; ++i) {
      forward = !forward;
      for (int m = 0; m < nanosCount; m++) {
        /*
         * Alternate the order that we invoke the makers to cancel out the
         * performance impact of adding elements to the DOM, which would cause
         * later tests to run more slowly than earlier tests.
         */
        NanoTest nano = nanos.get(forward ? m : (nanosCount - 1 - m));
        nano.setup();

        // Execute the test.
        log(i + ": " + nano.name);
        double start = Duration.currentTimeMillis();
        nano.runTest();

        // Record the end time.
        double thisTime = Duration.currentTimeMillis() - start;
        times[m] += thisTime;

        // Cleanup after the test.
        nano.teardown();
      }
    }

    // Record the times.
    double allTimes = 0;
    for (int m = 0; m < nanosCount; ++m) {
      record(m + 1, times[m]);
      allTimes += times[m];
    }
    grid.setText(grid.getRowCount() - 1, grid.getColumnCount() - 1, Util.format(allTimes));

    // Cleanup the dom.
    if (largeWidget != null) {
      root.remove(largeWidget);
    }
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
    grid.setText(row, columns - 1, Util.format(thisTime));

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
    grid.setText(row, 0, Util.format(Util.roundToTens(median)));

    mean = mean / (columns - 3);
    grid.setText(row, 1, Util.format(Util.roundToTens(mean)));
  }

  @SuppressWarnings("deprecation")
  private void saveInstances() {
    String value = number.getValue();
    Date expires = new Date();
    expires.setYear(expires.getYear() + 3);
    Cookies.setCookie(COOKIE, value, expires);
  }
}
