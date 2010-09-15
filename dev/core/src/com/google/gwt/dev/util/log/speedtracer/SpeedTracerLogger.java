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
package com.google.gwt.dev.util.log.speedtracer;

import com.google.gwt.dev.json.JsonArray;
import com.google.gwt.dev.json.JsonObject;
import com.google.gwt.dev.util.collect.Lists;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Logs performance metrics for internal development purposes. The output is
 * formatted so it can be opened directly in the SpeedTracer Chrome extension.
 * This class formats events using SpeedTracer's custom event feature. The html
 * file output can be viewed by using Chrome to open the file on a Chrome
 * browser that has the SpeedTracer extension installed.
 *
 * <p>
 * Enable logging by setting the system property {@code gwt.speedtracerlog} to
 * the output file path.
 * </p>
 *
 */
public final class SpeedTracerLogger {

  /**
   * Represents a node in a tree of SpeedTracer events.
   */
  public class Event {
    protected final EventType type;
    List<Event> children = Lists.create();
    List<String> data;
    long durationNanos;
    final long startTimeNanos;

    Event() {
      data = Lists.create();
      this.startTimeNanos = normalizedTimeNanos();
      this.type = null;
    }

    Event(Event parent, EventType type, String... data) {
      if (parent != null) {
        parent.children = Lists.add(parent.children, this);
      }
      this.type = type;
      assert (data.length % 2 == 0);
      this.data = Lists.create(data);
      this.startTimeNanos = normalizedTimeNanos();
    }

    /**
     * @param data key/value pairs to add to JSON object.
     */
    public void addData(String... data) {
      if (data != null) {
        assert (data.length % 2 == 0);
        this.data = Lists.addAll(this.data, data);
      }
    }

    /**
     * Signals the end of the current event.
     */
    public void end(String... data) {
      endImpl(this, data);
    }

    @Override
    public String toString() {
      return type.getName();
    }

    protected double convertToMilliseconds(long nanos) {
      return nanos / 1000000.0d;
    }

    JsonObject toJson() {
      JsonObject json = JsonObject.create();
      json.put("type", -2);
      json.put("typeName", type.getName());
      json.put("color", type.getColor());
      double startMs = convertToMilliseconds(startTimeNanos);
      json.put("time", startMs);
      double durationMs = convertToMilliseconds(durationNanos);
      json.put("duration", durationMs);

      JsonObject jsonData = JsonObject.create();
      for (int i = 0; i < data.size(); i += 2) {
        jsonData.put(data.get(i), data.get(i + 1));
      }
      json.put("data", jsonData);

      JsonArray jsonChildren = JsonArray.create();
      for (Event child : children) {
        jsonChildren.add(child.toJson());
      }
      json.put("children", jsonChildren);

      return json;
    }
  }

  /**
   * Enumerated types for logging events implement this interface.
   */
  public interface EventType {
    String getColor();

    String getName();
  }

  static enum Format {
    /**
     * Standard SpeedTracer log that includes JSON wrapped in HTML that will
     * launch a SpeedTracer monitor session.
     */
    HTML,

    /**
     * Only the JSON data without any HTML wrappers.
     */
    RAW
  }

  /**
   * A dummy implementation to do nothing if logging has not been turned on.
   */
  private class DummyEvent extends Event {
    @Override
    public void addData(String... data) {
      // do nothing
    }

    @Override
    public void end(String... data) {
      // do nothing
    }

    @Override
    public String toString() {
      return "Dummy";
    }
  }

  /**
   * Initializes the singleton on demand.
   */
  private static class LazySpeedTracerLoggerHolder {
    public static SpeedTracerLogger singleton = new SpeedTracerLogger();
  }

  /**
   * Thread that converts log requests to JSON in the background.
   */
  private class LogWriterThread extends Thread {
    private final String fileName;
    private final BlockingQueue<Event> threadEventQueue;
    private final Writer writer;

    public LogWriterThread(
        Writer writer, String fileName, final BlockingQueue<Event> eventQueue) {
      super();
      this.writer = writer;
      this.fileName = fileName;
      this.threadEventQueue = eventQueue;
    }

    @Override
    public void run() {
      try {
        boolean shuttingDown = false;
        while (!threadEventQueue.isEmpty() || !shuttingDown) {
          // Blocks until an event is queued
          Event event = threadEventQueue.take();
          if (event == shutDownSentinel) {
            shuttingDown = true;
          } else if (event == flushSentinel) {
            writer.flush();
            flushLatch.countDown();
          } else {
            JsonObject json = event.toJson();
            json.write(writer);
            writer.write('\n');
          }
        }
        // All queued events have been written.
        if (outputFormat.equals(Format.HTML)) {
          writer.write("</div></body></html>\n");
        }
        writer.close();
      } catch (InterruptedException ignored) {
      } catch (IOException e) {
        System.err.println("Unable to write to gwt.speedtracerlog '"
            + (fileName == null ? "" : fileName) + "'");
        e.printStackTrace();
      } finally {
        shutDownLatch.countDown();
      }
    }
  }

  /**
   * Records a LOG_MESSAGE type of SpeedTracer event.
   */
  private class MarkTimelineEvent extends Event {
    public MarkTimelineEvent(Event parent) {
      super();
      if (parent != null) {
        parent.children = Lists.add(parent.children, this);
      }
    }

    @Override
    JsonObject toJson() {
      JsonObject json = JsonObject.create();
      json.put("type", 11);
      double startMs = convertToMilliseconds(startTimeNanos);
      json.put("time", startMs);
      json.put("duration", 0.0);
      JsonObject jsonData = JsonObject.create();
      for (int i = 0; i < data.size(); i += 2) {
        jsonData.put(data.get(i), data.get(i + 1));
      }
      json.put("data", jsonData);
      return json;
    }
  }

  /**
   * Annotate the current event on the top of the stack with more information.
   * The method expects key, value pairs, so there must be an even number of
   * parameters.
   *
   * @param data JSON property, value pair to add to current event.
   */
  public static void addData(String... data) {
    SpeedTracerLogger.get().addDataImpl(data);
  }

  /**
   * Create a new global instance. Force the zero time to be recorded and the
   * log to be opened if the default logging is turned on with the <code>
   * -Dgwt.speedtracerlog</code> VM property.
   *
   * This method is only intended to be called once.
   */
  public static void init() {
    get();
  }

  /**
   * Adds a LOG_MESSAGE SpeedTracer event to the log. This represents a single
   * point in time and has a special representation in the SpeedTracer UI.
   */
  public static void markTimeline(String message) {
    SpeedTracerLogger.get().markTimelineImpl(message);
  }

  /**
   * Signals that a new event has started. You must end each event for each
   * corresponding call to {@code start}. You may nest timing calls.
   *
   * @param type the type of event
   * @data a set of key-value pairs (each key is followed by its value) that
   *       contain additional information about the event
   * @return an Event object to be closed by the caller
   */
  public static Event start(EventType type, String... data) {
    return SpeedTracerLogger.get().startImpl(type, data);
  }

  /**
   * For accessing the logger as a singleton, you can retrieve the global
   * instance. It is prudent, but not necessary to first initialize the
   * singleton with a call to {@link #init()} to set the base time.
   *
   * @return the current global {@link SpeedTracerLogger} instance.
   */
  private static SpeedTracerLogger get() {
    return LazySpeedTracerLoggerHolder.singleton;
  }

  private final DummyEvent dummyEvent = new DummyEvent();

  private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<
      Event>();

  private final BlockingQueue<Event> eventsToWrite;

  private CountDownLatch flushLatch;

  private final Event flushSentinel = new Event();

  private final Format outputFormat;

  private final ThreadLocal<Stack<Event>> pendingEvents = new ThreadLocal<
      Stack<Event>>() {
    @Override
    protected Stack<Event> initialValue() {
      return new Stack<Event>();
    }
  };
  private final CountDownLatch shutDownLatch = new CountDownLatch(1);

  private final Event shutDownSentinel = new Event();

  private final long zeroTimeNanos = System.nanoTime();
  private final long zeroTimeMilliseconds = System.currentTimeMillis();

  /**
   * Constructor intended for unit testing.
   *
   * @param writer alternative {@link Writer} to send speed tracer output.
   */
  SpeedTracerLogger(Writer writer, Format format) {
    outputFormat = format;
    eventsToWrite = openLogWriter(writer, "");
  }

  private SpeedTracerLogger() {
    // Allow a system property to override the default output format
    final String formatString = System.getProperty("gwt.speedtracerformat");
    Format format = Format.HTML;
    if (formatString != null) {
      for (Format value : Format.values()) {
        if (value.name().toLowerCase().equals(formatString.toLowerCase())) {
          format = value;
          break;
        }
      }
    }
    outputFormat = format;
    eventsToWrite = openDefaultLogWriter();
  }

  public void addDataImpl(String... data) {
    Stack<Event> threadPendingEvents = pendingEvents.get();
    if (threadPendingEvents.isEmpty()) {
      throw new IllegalStateException(
          "Tried to add data to an event that never started!");
    }

    Event currentEvent = threadPendingEvents.peek();
    currentEvent.addData(data);
  }

  public void markTimelineImpl(String message) {
    Stack<Event> threadPendingEvents = pendingEvents.get();
    Event parent = null;
    if (!threadPendingEvents.isEmpty()) {
      parent = threadPendingEvents.peek();
    }
    Event newEvent = new MarkTimelineEvent(parent);
    threadPendingEvents.push(newEvent);
    newEvent.end("message", message);
  }

  void endImpl(Event event, String... data) {
    if (eventsToWrite == null) {
      return;
    }

    if (data.length % 2 == 1) {
      throw new IllegalArgumentException("Unmatched data argument");
    }

    long endTimeNanos = normalizedTimeNanos();
    Stack<Event> threadPendingEvents = pendingEvents.get();
    if (threadPendingEvents.isEmpty()) {
      throw new IllegalStateException(
          "Tried to end an event that never started!");
    }
    Event currentEvent = threadPendingEvents.pop();

    assert (endTimeNanos >= currentEvent.startTimeNanos);
    currentEvent.durationNanos = endTimeNanos - currentEvent.startTimeNanos;

    while (currentEvent != event && !threadPendingEvents.isEmpty()) {
      // Missed a closing end for one or more frames! Try to sync back up.
      currentEvent.addData("Missed",
          "This event was closed without an explicit call to Event.end()");
      currentEvent = threadPendingEvents.pop();
      assert (endTimeNanos >= currentEvent.startTimeNanos);
      currentEvent.durationNanos = endTimeNanos - currentEvent.startTimeNanos;
    }

    if (threadPendingEvents.isEmpty() && currentEvent != event) {
      currentEvent.addData(
          "Missed", "Fell off the end of the threadPending events");
    }

    currentEvent.addData(data);
    if (threadPendingEvents.isEmpty()) {
      eventsToWrite.add(currentEvent);
    }
  }

  /**
   * Notifies the background thread to finish processing all data in the queue.
   */
  void flush() {
    try {
      // Wait for the other thread to drain the queue.
      flushLatch = new CountDownLatch(1);
      eventQueue.add(flushSentinel);
      flushLatch.await();
    } catch (InterruptedException e) {
      // Ignored
    }
  }

  Event startImpl(EventType type, String... data) {
    if (eventsToWrite == null) {
      return dummyEvent;
    }

    if (data.length % 2 == 1) {
      throw new IllegalArgumentException("Unmatched data argument");
    }

    Stack<Event> threadPendingEvents = pendingEvents.get();
    Event parent = null;
    if (!threadPendingEvents.isEmpty()) {
      parent = threadPendingEvents.peek();
    }

    Event newEvent = new Event(parent, type, data);
    // Add a field to the top level event in order to  track the base time
    // so we can re-normalize the data
    if (threadPendingEvents.size() == 0) {
      newEvent.addData("baseTime", "" + zeroTimeMilliseconds);
    }
    threadPendingEvents.push(newEvent);
    return newEvent;
  }

  private long normalizedTimeNanos() {
    return System.nanoTime() - zeroTimeNanos;
  }

  private BlockingQueue<Event> openDefaultLogWriter() {
    final String logFile = System.getProperty("gwt.speedtracerlog");

    Writer writer = null;
    if (logFile != null) {

      try {
        writer = new BufferedWriter(new FileWriter(logFile));
        return openLogWriter(writer, logFile);
      } catch (IOException e) {
        System.err.println(
            "Unable to open gwt.speedtracerlog '" + logFile + "'");
        e.printStackTrace();
      }
    }
    return null;
  }

  private BlockingQueue<Event> openLogWriter(
      final Writer writer, final String fileName) {
    try {
      if (outputFormat.equals(Format.HTML)) {
        writer.write(
                "<HTML isdump=\"true\"><body>"
                + "<style>body {font-family:Helvetica; margin-left:15px;}</style>"
                + "<h2>Performance dump from GWT</h2>"
                + "<div>This file contains data that can be viewed with the "
                + "<a href=\"http://code.google.com/speedtracer\">SpeedTracer</a> "
                + "extension under the <a href=\"http://chrome.google.com/\">"
                + "Chrome</a> browser.</div><p><span id=\"info\">"
                + "(You must install the SpeedTracer extension to open this file)</span></p>"
                + "<div style=\"display: none\" id=\"traceData\" version=\"0.17\">\n");
      }
    } catch (IOException e) {
      System.err.println("Unable to write to gwt.speedtracerlog '"
          + (fileName == null ? "" : fileName) + "'");
      e.printStackTrace();
      return null;
    }

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        try {
          // Wait for the other thread to drain the queue.
          eventQueue.add(shutDownSentinel);
          shutDownLatch.await();
        } catch (InterruptedException e) {
          // Ignored
        }
      }
    });

    // Background thread to write SpeedTracer events to log
    Thread logWriterWorker = new LogWriterThread(writer, fileName, eventQueue);

    // Lower than normal priority.
    logWriterWorker.setPriority(
        (Thread.MIN_PRIORITY + Thread.NORM_PRIORITY) / 2);

    /*
     * This thread must be daemon, otherwise shutdown hooks would never begin to
     * run, and an app wouldn't finish.
     */
    logWriterWorker.setDaemon(true);
    logWriterWorker.setName("SpeedTracerLogger writer");
    logWriterWorker.start();
    return eventQueue;
  }
}
