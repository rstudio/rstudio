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
package com.google.gwt.dev.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Performs basic recording/logging of performance metrics for internal
 * development purposes.
 *
 * <p>
 * This class differs from {@link com.google.gwt.core.ext.TreeLogger TreeLogger}
 * by providing an interface more suitable for metrics-oriented logging.
 * </p>
 *
 * <p>
 * Performance logging can be enabled by setting the system property,
 * {@code gwt.perflog=true}.
 * </p>
 *
 */
public class PerfLogger {

  private static class Timing {

    String message;

    long startTimeNanos;

    long totalTimeNanos;

    Timing parent = null;

    List<Timing> subTimings = new ArrayList<Timing>();

    boolean messageOnly;

    Timing(Timing parent, String message) {
      this.parent = parent;
      this.message = message;
      this.startTimeNanos = System.nanoTime();
    }

    Timing() {
    }

    boolean isRoot() {
      return parent == null;
    }
  }

  /**
   * Flag for enabling performance logging.
   */
  private static boolean enabled = Boolean.parseBoolean(System.getProperty("gwt.perflog"));

  private static ThreadLocal<Timing> currentTiming = new ThreadLocal<Timing>() {
    @Override
    protected Timing initialValue() {
      return new Timing();
    }
  };

  /**
   * Ends the current timing.
   */
  public static void end() {
    if (!enabled) {
      return;
    }
    long endTimeNanos = System.nanoTime();
    Timing timing = currentTiming.get();
    if (timing.isRoot()) {
      System.out.println("Tried to end a timing that was never started!\n");
      return;
    }
    timing.totalTimeNanos = endTimeNanos - timing.startTimeNanos;

    Timing newCurrent = timing.parent;
    currentTiming.set(newCurrent);
    if (newCurrent.isRoot()) {
      printTimings();
      newCurrent.subTimings = new ArrayList<Timing>();
    }
  }

  /**
   * Logs a message without explicitly including timer information.
   *
   * @param message a not <code>null</code> message
   */
  public static void log(String message) {
    if (enabled) {
      start(message);
      currentTiming.get().messageOnly = true;
      end();
    }
  }

  /**
   * Starts a new timing corresponding to {@code message}. You must call {@link
   * #end} for each corresponding call to {@code start}. You may nest timing
   * calls.
   *
   * @param message a not <code>null</code> message.
   */
  public static void start(String message) {
    if (!enabled) {
      return;
    }
    Timing current = currentTiming.get();
    Timing newTiming = new Timing(current, message);
    current.subTimings.add(newTiming);
    currentTiming.set(newTiming);
  }

  private static String getIndentString(int level) {
    StringBuffer str = new StringBuffer(level * 2);
    for (int i = 0; i < level; ++i) {
      str.append("  ");
    }
    return str.toString();
  }

  private static void printTiming(Timing t, int depth) {
    if (!t.isRoot()) {
      StringBuffer msg = new StringBuffer(getIndentString(depth - 1));
      msg.append("[perf] ");
      msg.append(t.message);
      if (!t.messageOnly) {
        msg.append(" ");
        msg.append(t.totalTimeNanos / 1000000);
        msg.append("ms");
      }
      System.out.println(msg);
    }

    ++depth;
    for (Timing subTiming : t.subTimings) {
      printTiming(subTiming, depth);
    }
  }

  private static void printTimings() {
    printTiming(currentTiming.get(), 0);
  }
}