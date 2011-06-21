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
package com.google.gwt.sample.mobilewebapp.client.ui;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.FillStrokeStyle;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.PartialSupport;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RequiresResize;

import java.util.ArrayList;
import java.util.List;

/**
 * A pie chart representation of data.
 */
@PartialSupport
public class PieChart extends Composite implements RequiresResize {

  /**
   * Information about a slice of pie.
   */
  private static class Slice {
    private final double weight;
    private final FillStrokeStyle fill;

    public Slice(double weight, FillStrokeStyle fill) {
      this.weight = weight;
      this.fill = fill;
    }
  }

  /**
   * The number of radians in a circle.
   */
  private static final double RADIANS_IN_CIRCLE = 2 * Math.PI;

  /**
   * Return a new {@link Canvas} if supported, and null otherwise.
   * 
   * @return a new {@link Canvas} if supported, and null otherwise
   */
  public static PieChart createIfSupported() {
    return isSupported() ? new PieChart() : null;
  }

  /**
   * Runtime check for whether the canvas element is supported in this browser.
   * 
   * @return whether the canvas element is supported
   */
  public static boolean isSupported() {
    return Canvas.isSupported();
  }

  private final Canvas canvas;
  private final List<Slice> slices = new ArrayList<Slice>();

  /**
   * Create using factory methods.
   */
  private PieChart() {
    canvas = Canvas.createIfSupported();
    canvas.setCoordinateSpaceHeight(300);
    canvas.setCoordinateSpaceWidth(300);
    initWidget(canvas);
  }

  /**
   * Add a slice to the chart.
   * 
   * @param weight the weight of the slice
   * @param fill the fill color
   */
  public void addSlice(double weight, FillStrokeStyle fill) {
    slices.add(new Slice(weight, fill));
  }

  /**
   * Clear all slices.
   */
  public void clearSlices() {
    slices.clear();
  }

  public void onResize() {
    redraw();
  }

  /**
   * Redraw the pie chart.
   */
  public void redraw() {
    if (!isAttached()) {
      return;
    }

    // Get the dimensions of the chart.
    int width = canvas.getCoordinateSpaceWidth();
    int height = canvas.getCoordinateSpaceHeight();
    double radius = Math.min(width, height) / 2.0;
    double cx = width / 2.0;
    double cy = height / 2.0;

    // Clear the context.
    Context2d context = canvas.getContext2d();
    context.clearRect(0, 0, width, height);

    // Get the total weight of all slices.
    double totalWeight = 0;
    for (Slice slice : slices) {
      totalWeight += slice.weight;
    }

    // Draw the slices.
    double startAngle = -0.5 * Math.PI;
    for (Slice slice : slices) {
      double weight = slice.weight / totalWeight;
      double endAngle = startAngle + (weight * RADIANS_IN_CIRCLE);
      context.setFillStyle(slice.fill);
      context.beginPath();
      context.moveTo(cx, cy);
      context.arc(cx, cy, radius, startAngle, endAngle);
      context.fill();
      startAngle = endAngle;
    }
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      public void execute() {
        redraw();
      }
    });
  }
}
