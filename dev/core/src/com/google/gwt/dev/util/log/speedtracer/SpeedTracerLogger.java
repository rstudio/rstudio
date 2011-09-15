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
import com.google.gwt.dev.shell.DevModeSession;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dev.util.log.dashboard.DashboardNotifierFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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
  
  private static final Logger log = Logger.getLogger(SpeedTracerLogger.class.getName());
  
  // Log file name (logging is enabled if this is non-null)
  private static final String logFile = 
    System.getProperty("gwt.speedtracerlog");
  
  // Allow a system property to override the default output format
  private static final String defaultFormatString = 
    System.getProperty("gwt.speedtracerformat");
  
  // Use cumulative multi-threaded process cpu time instead of wall time
  private static final boolean logProcessCpuTime = 
    getBooleanProperty("gwt.speedtracer.logProcessCpuTime");
  
  // Use per thread cpu time instead of wall time. If logProcessCpuTime is set,
  // then this can remain false - we only need one or the other.
  private static final boolean logThreadCpuTime =
      getBooleanProperty("gwt.speedtracer.logThreadCpuTime");
  
  // Turn on logging summarizing gc time during an event
  private static final boolean logGcTime =
    getBooleanProperty("gwt.speedtracer.logGcTime");
  
  // Turn on logging estimating overhead used for speedtracer logging.
  private static final boolean logOverheadTime =
    getBooleanProperty("gwt.speedtracer.logOverheadTime");

  // Disable logging of JSNI calls and callbacks to reduce memory usage where
  // the heap is already tight.
  private static final boolean jsniCallLoggingEnabled = 
      !getBooleanProperty("gwt.speedtracer.disableJsniLogging");

  static {
    // verify configuration
    if (logProcessCpuTime && logThreadCpuTime) {
      throw new RuntimeException("System properties are misconfigured: "
          + "Specify one or the other of 'gwt.speedtracer.logProcessCpuTime' "
          + "or 'gwt.speedtracer.logThreadCpuTime', not both.");
    }
  }
  
  /**
   * Represents a node in a tree of SpeedTracer events.
   */
  public class Event {
    protected final EventType type;
    List<Event> children;
    List<String> data;
    DevModeSession devModeSession;
    
    long elapsedDurationNanos;
    long elapsedStartTimeNanos;

    long processCpuDurationNanos;
    long processCpuStartTimeNanos;

    long threadCpuDurationNanos;
    long threadCpuStartTimeNanos;

    Event() {
      if (enabled) {
        if (logThreadCpuTime) {
          threadCpuTimeKeeper.resetTimeBase();
        }
        recordStartTime();
        this.data = Lists.create();
        this.children = Lists.create();
      } else {
        this.processCpuStartTimeNanos = 0L;
        this.threadCpuStartTimeNanos = 0L;
        this.elapsedStartTimeNanos = 0L;
        this.data = null;
        this.children = null;
      }
      this.type = null;
    }

    Event(DevModeSession session, Event parent, EventType type, String... data) {

      if (parent != null) {
        parent.children = Lists.add(parent.children, this);
      }
      this.type = type;
      assert (data.length % 2 == 0);
      recordStartTime();
      this.data = Lists.create(data);
      this.children = Lists.create();
      this.devModeSession = session;
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

    public DevModeSession getDevModeSession() {
      return devModeSession;
    }

    /**
     * Returns the event duration, in nanoseconds, for the log file. Depending
     * on system properties, this will measured in elapsed time, process CPU
     * time, or thread CPU time.
     */
    public long getDurationNanos() {
      return logProcessCpuTime ? processCpuDurationNanos : (logThreadCpuTime
          ? threadCpuDurationNanos : elapsedDurationNanos);
    }

    public long getElapsedDurationNanos() {
      return this.elapsedDurationNanos;
    }

    public long getElapsedStartTimeNanos() {
      return this.elapsedStartTimeNanos;
    }

    /**
     * Returns the event start time, normalized in nanoseconds, for the log
     * file. Depending on system properties, this will be normalized based on
     * elapsed time, process CPU time, or thread CPU time.
     */
    public long getStartTimeNanos() {
      return logProcessCpuTime ? processCpuStartTimeNanos : (logThreadCpuTime
          ? threadCpuStartTimeNanos : elapsedStartTimeNanos);
    }

    public EventType getType() {
      return type;
    }

    @Override
    public String toString() {
      return type.getName();
    }

    /**
     * Extends the durations of the current event by the durations of the
     * specified event.
     */
    void extendDuration(Event refEvent) {
      elapsedDurationNanos += refEvent.elapsedDurationNanos;
      processCpuDurationNanos += refEvent.processCpuDurationNanos;
      threadCpuDurationNanos += refEvent.threadCpuDurationNanos;
    }

    /**
     * Sets the start time of this event to start immediately after the
     * specified event ends.
     */
    void setStartsAfter(Event refEvent) {
      elapsedStartTimeNanos = refEvent.elapsedStartTimeNanos + refEvent.elapsedDurationNanos;
      processCpuStartTimeNanos =
          refEvent.processCpuStartTimeNanos + refEvent.processCpuDurationNanos;
      threadCpuStartTimeNanos = refEvent.threadCpuStartTimeNanos + refEvent.threadCpuDurationNanos;
    }
    
    JsonObject toJson() {
      JsonObject json = JsonObject.create();
      json.put("type", -2);
      json.put("typeName", type.getName());
      json.put("color", type.getColor());
      double startMs = convertToMilliseconds(getStartTimeNanos());
      json.put("time", startMs);
      double durationMs = convertToMilliseconds(getDurationNanos());
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

    /**
     * Records the duration of this event based on the current time and the
     * event's recorded start time.
     */
    void updateDuration() {
      long elapsedEndTimeNanos = elapsedTimeKeeper.normalizedTimeNanos();
      assert (elapsedEndTimeNanos >= elapsedStartTimeNanos);
      elapsedDurationNanos = elapsedEndTimeNanos - elapsedStartTimeNanos;

      // don't bother making expensive time keeping method calls unless
      // necessary
      if (logProcessCpuTime) {
        long processCpuEndTimeNanos = processCpuTimeKeeper.normalizedTimeNanos();
        assert (processCpuEndTimeNanos >= processCpuStartTimeNanos);
        processCpuDurationNanos = processCpuEndTimeNanos - processCpuStartTimeNanos;
      } else if (logThreadCpuTime) {
        long threadCpuEndTimeNanos = threadCpuTimeKeeper.normalizedTimeNanos();
        assert (threadCpuEndTimeNanos >= threadCpuStartTimeNanos);
        threadCpuDurationNanos = threadCpuEndTimeNanos - threadCpuStartTimeNanos;
      }
    }

    /**
     * Marks the start time for this event. Three different time measurements
     * are used:
     * <ol>
     * <li>Elapsed (wall-clock) time</li>
     * <li>Process CPU time</li>
     * <li>Thread CPU time</li>
     * </ol>
     */
    private void recordStartTime() {
      elapsedStartTimeNanos = elapsedTimeKeeper.normalizedTimeNanos();

      // don't bother making expensive time keeping method calls unless
      // necessary
      if (logProcessCpuTime) {
        processCpuStartTimeNanos = processCpuTimeKeeper.normalizedTimeNanos();
      } else if (logThreadCpuTime) {
        threadCpuStartTimeNanos = threadCpuTimeKeeper.normalizedTimeNanos();
      }
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
   * Provides functionality specific to garbage collection events.
   */
  private class GcEvent extends Event {
    private Event refEvent;

    /**
     * Constructs an event that represents garbage collection metrics.
     * 
     * @param refEvent the event during which the garbage collections took place
     * @param gcType the garbage collector type
     * @param collectionCount the total number of collections for this garbage
     *          collector type
     * @param durationNanos the total elapsed time spent in garbage collection
     *          during the span of {@code refEvent}
     */
    GcEvent(Event refEvent, String gcType, long collectionCount, long durationNanos) {
      super(null, null, SpeedTracerEventType.GC, "Collector Type", gcType,
          "Cumulative Collection Count", Long.toString(collectionCount));

      this.refEvent = refEvent;
      // GarbageCollectorMXBean can only provide elapsed time, so that's all we
      // record
      this.elapsedDurationNanos = durationNanos;
    }

    /**
     * Returns elapsed duration since that is the only duration we can measure
     * for garbage collection events.
     */
    @Override
    public long getDurationNanos() {
      return getElapsedDurationNanos();
    }

    /**
     * Returns a start time so that this event ends with its {@code refEvent}.
     */
    @Override
    public long getElapsedStartTimeNanos() {
      return refEvent.getElapsedStartTimeNanos() + refEvent.getElapsedDurationNanos()
          - getElapsedDurationNanos();
    }

    /**
     * Returns a start time so that this event ends with its {@code refEvent}.
     */
    @Override
    public long getStartTimeNanos() {
      return refEvent.getStartTimeNanos() + refEvent.getDurationNanos() - getDurationNanos();
    }
  }
  
  /**
   * Time keeper which uses wall time.
   */
  private class ElapsedNormalizedTimeKeeper {

    private final long zeroTimeNanos;
    private final long zeroTimeMillis;

    public ElapsedNormalizedTimeKeeper() {
      zeroTimeNanos = System.nanoTime();
      zeroTimeMillis = (long) convertToMilliseconds(zeroTimeNanos);
    }

    public long normalizedTimeNanos() {
      return System.nanoTime() - zeroTimeNanos;
    }
    
    public long zeroTimeMillis() {
      return zeroTimeMillis;
    }
  }
  
  /**
   * Time keeper which uses process cpu time.  This can be greater than wall
   * time, since it is cumulative over the multiple threads of a process.
   */
  private class ProcessNormalizedTimeKeeper {
    private final OperatingSystemMXBean osMXBean;
    private final Method getProcessCpuTimeMethod;
    private final long zeroTimeNanos;
    private final long zeroTimeMillis;
    
    public ProcessNormalizedTimeKeeper() {
      try {
        osMXBean = ManagementFactory.getOperatingSystemMXBean();
        /* 
         * Find this method by reflection, since it's part of the Sun
         * implementation for OperatingSystemMXBean, and we can't always assume
         * that com.sun.management.OperatingSystemMXBean will be available.
         */
        getProcessCpuTimeMethod = 
          osMXBean.getClass().getMethod("getProcessCpuTime");
        getProcessCpuTimeMethod.setAccessible(true);
        zeroTimeNanos = (Long) getProcessCpuTimeMethod.invoke(osMXBean);
        zeroTimeMillis = (long) convertToMilliseconds(zeroTimeNanos);
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }
    
    public long normalizedTimeNanos() {
      try {
        return (Long) getProcessCpuTimeMethod.invoke(osMXBean) - zeroTimeNanos;
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }

    public long zeroTimeMillis() {
      return zeroTimeMillis;
    }
  }

  /**
   * Time keeper which uses per thread cpu time.  It is assumed that individual
   * events logged will be single threaded, and that top-level events will call
   * {@link #resetTimeBase()} prior to logging time.  The resettable time base
   * is needed since each individual thread starts its timing at 0, regardless
   * of when the thread is created.  So we reset the time base at the beginning
   * of an event, so that we can generate a chronologically representative
   * output, although the relation to wall time is actually compressed within
   * a logged event (thread cpu time does not include wait time, etc.).
   */
  private class ThreadNormalizedTimeKeeper {

    private final ThreadMXBean threadMXBean;
    private final ThreadLocal<Long> resettableTimeBase = new ThreadLocal<Long>();
    private final long zeroTimeNanos;
    private final long zeroTimeMillis;

    public ThreadNormalizedTimeKeeper() {
      threadMXBean = ManagementFactory.getThreadMXBean();
      if (!threadMXBean.isCurrentThreadCpuTimeSupported()) {
        throw new RuntimeException("Current thread cpu time not supported");
      }
      zeroTimeNanos = System.nanoTime();
      zeroTimeMillis = (long) convertToMilliseconds(zeroTimeNanos);
    }

    public long normalizedTimeNanos() {
      return threadMXBean.getCurrentThreadCpuTime() + resettableTimeBase.get();
    }
    
    public void resetTimeBase() {
      /*
       * Since all threads start individually at time 0L, we use this to
       * offset each event's time so we can generate chronological output.
       */
      resettableTimeBase.set(System.nanoTime() 
          - zeroTimeNanos - threadMXBean.getCurrentThreadCpuTime());
    }
    
    public long zeroTimeMillis() {
      return zeroTimeMillis;
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
    private static final int FLUSH_TIMER_MSECS = 10000;
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
      long nextFlush = System.currentTimeMillis() + FLUSH_TIMER_MSECS;
      try {
        while (true) {
          Event event =
              threadEventQueue.poll(nextFlush - System.currentTimeMillis(),
                  TimeUnit.MILLISECONDS);
          if (event == null) {
            // ignore.
          } else if (event == shutDownSentinel) {
            break;
          } else if (event == flushSentinel) {
            writer.flush();
            flushLatch.countDown();
          } else {
            JsonObject json = event.toJson();
            json.write(writer);
            writer.write('\n');
          }
          if (System.currentTimeMillis() >= nextFlush) {
            writer.flush();
            nextFlush = System.currentTimeMillis() + FLUSH_TIMER_MSECS;
          }
        }
        // All queued events have been written.
        if (outputFormat.equals(Format.HTML)) {
          writer.write("</div></body></html>\n");
        }
        writer.close();
      } catch (InterruptedException ignored) {
      } catch (IOException e) {
        log.log(Level.SEVERE, "Unable to write to gwt.speedtracerlog '"
            + (fileName == null ? "" : fileName) + "'", e);
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
      double startMs = convertToMilliseconds(getStartTimeNanos());
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
   * Returns true if JSNI calls and callbacks are being logged.
   */
  public static boolean jsniCallLoggingEnabled() {
    return jsniCallLoggingEnabled;
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
   * <p>
   * Has the same effect as calling
   * {@link #start(DevModeSession, EventType, String...)
   * start(DevModeSession.getSessionForCurrentThread(), type, data)}.
   * 
   * @param type the type of event
   * @param data a set of key-value pairs (each key is followed by its value)
   *          that contain additional information about the event
   * @return an Event object to be ended by the caller
   */
  public static Event start(EventType type, String... data) {
    return SpeedTracerLogger.get().startImpl(DevModeSession.getSessionForCurrentThread(), type,
        data);
  }

  /**
   * Signals that a new event has started. You must end each event for each
   * corresponding call to {@code start}. You may nest timing calls.
   *
   * @param session the devmode session with which this event is associated or
   *      null if no devmode session is active
   * @param type the type of event
   * @param data a set of key-value pairs (each key is followed by its value) that
   *       contain additional information about the event
   * @return an Event object to be ended by the caller
   */
  public static Event start(DevModeSession session, EventType type, String... data) {
    return SpeedTracerLogger.get().startImpl(session, type, data);
  }

  private static double convertToMilliseconds(long nanos) {
    return nanos / 1000000.0d;
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
  
  private static boolean getBooleanProperty(String propName) {
    try {
      return System.getProperty(propName) != null;
    } catch (RuntimeException ruEx) {
      return false;
    }
  }

  private final boolean enabled;
  
  private final DummyEvent dummyEvent = new DummyEvent();

  private BlockingQueue<Event> eventsToWrite;

  private final boolean fileLoggingEnabled;
  
  private CountDownLatch flushLatch;

  private Event flushSentinel;

  private Format outputFormat;

  private ThreadLocal<Stack<Event>> pendingEvents;
  
  private CountDownLatch shutDownLatch;

  private Event shutDownSentinel;

  private List<GarbageCollectorMXBean> gcMXBeans;
  
  private Map<String, Long> lastGcTimes;

  private final ElapsedNormalizedTimeKeeper elapsedTimeKeeper = new ElapsedNormalizedTimeKeeper();

  private final ProcessNormalizedTimeKeeper processCpuTimeKeeper = 
              (logProcessCpuTime) ? new ProcessNormalizedTimeKeeper() : null;

  private final ThreadNormalizedTimeKeeper threadCpuTimeKeeper = 
              (logThreadCpuTime) ? new ThreadNormalizedTimeKeeper() : null;

  /**
   * Constructor intended for unit testing.
   * 
   * @param writer alternative {@link Writer} to send speed tracer output.
   */
  SpeedTracerLogger(Writer writer, Format format) {
    enabled = true;
    fileLoggingEnabled = true;
    outputFormat = format;
    eventsToWrite = openLogWriter(writer, "");
    pendingEvents = initPendingEvents();
    shutDownSentinel = new DummyEvent();
    flushSentinel = new DummyEvent();
    shutDownLatch = new CountDownLatch(1);
  }

  private SpeedTracerLogger() {
    fileLoggingEnabled = logFile != null;
    enabled = fileLoggingEnabled || DashboardNotifierFactory.areNotificationsEnabled();

    if (enabled) {
      if (fileLoggingEnabled) {
        // Allow a system property to override the default output format
        Format format = Format.HTML;
        if (defaultFormatString != null) {
          for (Format value : Format.values()) {
            if (value.name().toLowerCase().equals(defaultFormatString.toLowerCase())) {
              format = value;
              break;
            }
          }
        }
        outputFormat = format;
        eventsToWrite = openDefaultLogWriter();

        shutDownSentinel = new DummyEvent();
        flushSentinel = new DummyEvent();
        shutDownLatch = new CountDownLatch(1);
      }

      if (logGcTime) {
        gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        lastGcTimes = new ConcurrentHashMap<String, Long>();
      }

      pendingEvents = initPendingEvents();
    }
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
  
  void addGcEvents(Event refEvent) {
    // we're not sending GC events to the dashboard, so we only record them
    // to file
    if (!fileLoggingEnabled) {
      return;
    }

    for (GarbageCollectorMXBean gcMXBean : gcMXBeans) {
      String gcName = gcMXBean.getName();
      Long lastGcTime = lastGcTimes.get(gcName);
      long currGcTime = gcMXBean.getCollectionTime();
      if (lastGcTime == null) {
        lastGcTime = 0L;
      }
      if (currGcTime > lastGcTime) {
        // create a new event
        long gcDurationNanos = (currGcTime - lastGcTime) * 1000000L;
        Event gcEvent =
            new GcEvent(refEvent, gcName, gcMXBean.getCollectionCount(), gcDurationNanos);

        eventsToWrite.add(gcEvent);
        lastGcTimes.put(gcName, currGcTime);
      }
    }
  }
  
  void addOverheadEvent(Event refEvent) {
    Event overheadEvent =
        new Event(refEvent.devModeSession, refEvent, SpeedTracerEventType.OVERHEAD);
    // measure the time between the end of refEvent and now
    overheadEvent.setStartsAfter(refEvent);
    overheadEvent.updateDuration();

    refEvent.extendDuration(overheadEvent);
  }

  void endImpl(Event event, String... data) {
    if (!enabled) {
      return;
    }

    if (data.length % 2 == 1) {
      throw new IllegalArgumentException("Unmatched data argument");
    }
    
    Stack<Event> threadPendingEvents = pendingEvents.get();
    if (threadPendingEvents.isEmpty()) {
      throw new IllegalStateException(
          "Tried to end an event that never started!");
    }
    Event currentEvent = threadPendingEvents.pop();
    currentEvent.updateDuration();
    
    while (currentEvent != event && !threadPendingEvents.isEmpty()) {
      // Missed a closing end for one or more frames! Try to sync back up.
      currentEvent.addData("Missed",
          "This event was closed without an explicit call to Event.end()");
      currentEvent = threadPendingEvents.pop();
      currentEvent.updateDuration();
    }

    if (threadPendingEvents.isEmpty() && currentEvent != event) {
      currentEvent.addData(
          "Missed", "Fell off the end of the threadPending events");
    }
    
    if (logGcTime) {
      addGcEvents(currentEvent);
    }

    currentEvent.addData(data);
    
    if (logOverheadTime) {
      addOverheadEvent(currentEvent);
    }
    
    if (threadPendingEvents.isEmpty()) {
      if (fileLoggingEnabled) {
        eventsToWrite.add(currentEvent);
      }

      DashboardNotifierFactory.getNotifier().devModeEventEnd(currentEvent.getDevModeSession(),
          currentEvent.getType().getName(), currentEvent.getElapsedStartTimeNanos(),
          currentEvent.getElapsedDurationNanos());
    }
  }

  /**
   * Notifies the background thread to finish processing all data in the queue.
   * Blocks the current thread until the data is flushed in the Log Writer
   * thread.
   */
  void flush() {
    if (!fileLoggingEnabled) {
      return;
    }
    
    try {
      // Wait for the other thread to drain the queue.
      flushLatch = new CountDownLatch(1);
      eventsToWrite.add(flushSentinel);
      flushLatch.await();
    } catch (InterruptedException e) {
      // Ignored
    }
  }

  Event startImpl(DevModeSession session, EventType type, String... data) {
    if (!enabled) {
      return dummyEvent;
    }

    if (data.length % 2 == 1) {
      throw new IllegalArgumentException("Unmatched data argument");
    }

    Stack<Event> threadPendingEvents = pendingEvents.get();
    Event parent = null;
    if (!threadPendingEvents.isEmpty()) {
      parent = threadPendingEvents.peek();
    } else {
      DashboardNotifierFactory.getNotifier().devModeEventBegin();
      if (logThreadCpuTime) {
        // reset the thread CPU time base for top-level events (so events can be
        // properly sequenced chronologically)
        threadCpuTimeKeeper.resetTimeBase();
      }
    }
    
    Event newEvent = new Event(session, parent, type, data);
    // Add a field to the top level event in order to  track the base time
    // so we can re-normalize the data
    if (threadPendingEvents.size() == 0) {
      long baseTime = logProcessCpuTime ? processCpuTimeKeeper.zeroTimeMillis()
          : (logThreadCpuTime ? threadCpuTimeKeeper.zeroTimeMillis()
              : elapsedTimeKeeper.zeroTimeMillis());
      newEvent.addData("baseTime", "" + baseTime);
    }
    threadPendingEvents.push(newEvent);
    return newEvent;
  }

  private ThreadLocal<Stack<Event>> initPendingEvents() {
    return new ThreadLocal<Stack<Event>>() {
      @Override
      protected Stack<Event> initialValue() {
        return new Stack<Event>();
      }
    };
  }
  
  private BlockingQueue<Event> openDefaultLogWriter() {
    Writer writer = null;
    if (enabled) {
      try {
        writer = new BufferedWriter(new FileWriter(logFile));
        return openLogWriter(writer, logFile);
      } catch (IOException e) {
        log.log(Level.SEVERE, "Unable to open gwt.speedtracerlog '" + logFile + "'", e);
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
      log.log(Level.SEVERE, "Unable to write to gwt.speedtracerlog '"
          + (fileName == null ? "" : fileName) + "'", e);
      return null;
    }

    final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<Event>();
    
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
