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

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
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
   * Enumerated types for logging events implement this interface.
   */
  public interface EventType {
    String getColor();

    String getName();
  }

  /**
   * Represents a node in a tree of SpeedTracer events.
   */
  private class Event {

    final List<Event> children = new ArrayList<Event>();

    final List<String> data = new ArrayList<String>();

    long durationNanos;

    final long startTimeNanos;

    final EventType type;

    Event(Event parent, EventType type, String[] data) {
      if (parent != null) {
        parent.children.add(this);
      }
      this.type = type;
      if (data != null) {
        for (int i = 0; i < data.length; i++) {
          this.data.add(data[i]);
        }
      }
      this.startTimeNanos = normalizedTimeNanos();
    }

    /**
     * @param data key/value pairs to add to JSON object.
     */
    public void addData(String[] data) {
      if (data != null) {
        for (int i = 0; i < data.length; i++) {
          this.data.add(data[i]);
        }
      }
    }

    @Override
    public String toString() {
      return type.getName();
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

    private double convertToMilliseconds(long nanos) {
      return nanos / 1000000.0d;
    }
  }

  /**
   * Thread that converts log requests to JSON in the background.
   */
  private class LogWriterThread extends Thread {
    private final BlockingQueue<Event> threadEventQueue;
    private final String fileName;
    private final Writer writer;

    public LogWriterThread(Writer writer, String fileName,
        final BlockingQueue<Event> eventQueue) {
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
        writer.write("</div></body></html>\n");
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

  private static SpeedTracerLogger singleton;

  /**
   * For accessing the logger as a singleton, you can retrieve the global
   * instance. You must first initialize the singleton with a call to
   * {@link #init()} or {@link #init(Writer)}.
   *
   * @return the current instance if one already exists from a previous call to
   *         {@link #init()}
   */
  public static synchronized SpeedTracerLogger get() {
    if (singleton == null) {
      init();
    }
    return singleton;
  }

  /**
   * Create a new global instance. Force the zero time to be recorded and the
   * log to be opened if the default logging is turned on with the
   * <code>-Dgwt.speedtracerlog</code> VM property.
   *
   * This method is only intended to be called once (except for unit testing).
   */
  public static synchronized void init() {
    if (singleton != null) {
      singleton.flush();
    }
    singleton = newInstance();
  }

  /**
   * Creates a new instance and assigns it to the global instance.
   *
   * This method is only intended to be called once (except for unit testing).
   *
   * @param writer The writer to use for logging messages.
   */
  public static synchronized void init(Writer writer) {
    if (singleton != null) {
      singleton.flush();
    }
    singleton = newInstance(writer);
  }

  /**
   * Allocates a new instance of the logger. The global instance returned by
   * init() or get() is not affected.
   *
   * @return a new {@link SpeedTracerLogger} instance.
   */
  public static SpeedTracerLogger newInstance() {
    return new SpeedTracerLogger();
  }

  /**
   * Allocates a new instance of the logger. The global instance returned by
   * {@link #init()} or {@link #get()} is not affected.
   *
   * @param writer The writer to use for logging messages.
   * @return a new {@link SpeedTracerLogger} instance.
   */
  public static SpeedTracerLogger newInstance(Writer writer) {
    return new SpeedTracerLogger(writer);
  }

  private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<Event>();

  private final BlockingQueue<Event> eventsToWrite;

  private CountDownLatch flushLatch;

  private final Event flushSentinel = new Event(null, null, null);

  private final ThreadLocal<Stack<Event>> pendingEvents = new ThreadLocal<Stack<Event>>() {
    @Override
    protected Stack<Event> initialValue() {
      return new Stack<Event>();
    }
  };

  private final CountDownLatch shutDownLatch = new CountDownLatch(1);

  private final Event shutDownSentinel = new Event(null, null, null);

  private final long zeroTimeNanos = System.nanoTime();

  private SpeedTracerLogger() {
    eventsToWrite = openDefaultLogWriter();
  }

  private SpeedTracerLogger(Writer writer) {
    eventsToWrite = openLogWriter(writer, "");
  }

  /**
   * Signals the end of the current event.
   */
  public void end(EventType type, String... data) {
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

    List<Event> missedEvents = Lists.create();

    // TODO(zundel): Why not just assert in this case?
    while (currentEvent.type != type && !threadPendingEvents.isEmpty()) {
      // Missed a closing end for one or more frames! Try to sync back up.
      missedEvents = Lists.add(missedEvents, currentEvent);
      currentEvent = threadPendingEvents.pop();
      currentEvent.durationNanos = endTimeNanos - currentEvent.startTimeNanos;
    }
    currentEvent.addData(data);
    if (threadPendingEvents.isEmpty()) {
      eventsToWrite.add(currentEvent);
    }
    if (missedEvents.size() > 0) {
      StringBuilder sb = new StringBuilder();
      sb.append("SpeedTracerLogger missing end() calls for the following events: ");
      for (Event event : missedEvents) {
        sb.append(event.type.getName());
      }
      throw new IllegalStateException(sb.toString());
    }
  }

  /**
   * Notifies the background thread to finish processing all data in the queue.
   */
  public void flush() {
    try {
      // Wait for the other thread to drain the queue.
      flushLatch = new CountDownLatch(1);
      eventQueue.add(flushSentinel);
      flushLatch.await();
    } catch (InterruptedException e) {
      // Ignored
    }
  }

  /**
   * Signals that a new event has started. You must call {@link #end} for each
   * corresponding call to {@code start}. You may nest timing calls.
   *
   * @param type the type of event
   * @data a set of key-value pairs (each key is followed by its value) that
   *       contain additional information about the event
   */
  public void start(EventType type, String... data) {
    if (eventsToWrite == null) {
      return;
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
    threadPendingEvents.push(newEvent);
  }

  private long normalizedTimeNanos() {
    return System.nanoTime() - zeroTimeNanos;
  }

  private BlockingQueue<Event> openDefaultLogWriter() {
    final String logFile = System.getProperty("gwt.speedtracerlog");
    Writer writer = null;
    if (logFile != null) {

      try {
        writer = new FileWriter(logFile);
        return openLogWriter(writer, logFile);
      } catch (IOException e) {
        System.err.println("Unable to open gwt.speedtracerlog '" + logFile
            + "'");
        e.printStackTrace();
      }
    }
    return null;
  }

  private BlockingQueue<Event> openLogWriter(final Writer writer,
      final String fileName) {
    try {
      writer.write("<HTML isdump=\"true\"><body>"
          + "<style>body {font-family:Helvetica; margin-left:15px;}</style>"
          + "<h2>Performance dump from GWT</h2>"
          + "<div>This file contains data that can be viewed with the "
          + "<a href=\"http://code.google.com/speedtracer\">SpeedTracer</a> "
          + "extension under the <a href=\"http://chrome.google.com/\">Chrome</a> browser.</div>"
          + "<p><span id=\"info\">(You must install the SpeedTracer extension to open this file)</span></p>"
          + "<div style=\"display: none\" id=\"traceData\" version=\"0.13\">\n");
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
    logWriterWorker.setPriority((Thread.MIN_PRIORITY + Thread.NORM_PRIORITY) / 2);

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
