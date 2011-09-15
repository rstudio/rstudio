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
package com.google.gwt.dev.util.log.dashboard;

import com.google.gwt.dev.shell.DevModeSession;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import junit.framework.Assert;

import java.util.LinkedList;

/**
 * Mock object for testing integration of {@code SpeedTracerLogger} and
 * {@code DashboardNotifier}.
 */
public class SpeedTracerLoggerTestMockNotifier implements DashboardNotifier {

  /**
   * Represents the parameters passed to {@code devModeEvent()}.
   */
  public static class DevModeEvent {
    private DevModeSession session;
    private String eventType;
    private long startTimeNanos;
    private long durationNanos;

    public DevModeEvent(DevModeSession session, String eventType, long startTimeNanos,
        long durationNanos) {
      this.session = session;
      this.eventType = eventType;
      this.startTimeNanos = startTimeNanos;
      this.durationNanos = durationNanos;
    }
    
    public DevModeEvent(Event e) {
      this.session = e.getDevModeSession();
      this.eventType = e.getType().getName();
      this.startTimeNanos = e.getElapsedStartTimeNanos();
      this.durationNanos = e.getElapsedDurationNanos();
    }

    public DevModeSession getDevModeSession() {
      return session;
    }
    
    @Override
    public boolean equals(Object o) {
      if (o != null && o instanceof DevModeEvent) {
        DevModeEvent e = (DevModeEvent) o;
        return session.equals(e.session) && eventType.equals(e.eventType)
            && startTimeNanos == e.startTimeNanos && durationNanos == e.durationNanos;
      }
      return false;
    }

    @Override
    public int hashCode() {
      int hash = 37;
      hash = hash*19 + session.hashCode();
      hash = hash*19 + eventType.hashCode();
      hash = hash*19 + Long.valueOf(startTimeNanos).hashCode();
      hash = hash*19 + Long.valueOf(durationNanos).hashCode();
      return hash;
    }
  }
  
  /**
   * Activates this mock object. After calling this, the notifier factory will
   * be setup so that dashboard notifications are enabled and the notifier
   * instance returned is an instance of this class
   */
  public static SpeedTracerLoggerTestMockNotifier enable() {
    SpeedTracerLoggerTestMockNotifier ret = new SpeedTracerLoggerTestMockNotifier();
    DashboardNotifierFactory.setNotifier(ret);
    return ret;
  }

  /**
   * Keeps track of calls to {@code devModeEvent()}.
   */
  private LinkedList<DevModeEvent> eventSeq = new LinkedList<DevModeEvent>();
  private boolean started;

  @Override
  public void devModeEventBegin() {
    Assert.assertFalse("DashboardNotifier.devModeEventBegin() called more than once "
        + "before call DashboardNotifier.devModeEventEnd()", started);
    started = true;
  }
  
  @Override
  public void devModeEventEnd(DevModeSession session, String eventType, long startTimeNanos,
      long durationNanos) {
    Assert.assertTrue("DashboardNotifier.devModeEventEnd() without prior call to "
        + "DashboardNotifier.devModeEventBegin()", started);
    started = false;
    DevModeEvent e = new DevModeEvent(session, eventType, startTimeNanos, durationNanos);
    eventSeq.add(e);
  }

  @Override
  public void devModeSessionBegin(DevModeSession session) {
    // always raise exception here - this method shouldn't be invoked from
    // SpeedTracerLogger
    Assert.fail("SpeedTracerLogger should not be calling DashboardNotifier.devModeSessionBegin()");
  }

  @Override
  public void devModeSessionEnd(DevModeSession session) {
    // always raise exception here - this method shouldn't be invoked from
    // SpeedTracerLogger
    Assert.fail("SpeedTracerLogger should not be calling DashboardNotifier.devModeSessionEnd()");
  }

  /**
   * Returns the sequence of events posted to the notifier. Also validates that
   * the notifier is in a valid state (i.e. not between calls to beginning and
   * ending an event).
   * 
   * @return the sequence of events posted to the notifier
   */
  public LinkedList<DevModeEvent> getEventSequence() {
    Assert.assertFalse("DashboardNotifier.devModeEventBegin() called without matching "
        + "call to DashboardNotifier.devModeEventEnd()", started);
    return eventSeq;
  }
}
