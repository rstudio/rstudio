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
 * Mock object for testing SpeedTracerLogger
 */
public class SpeedTracerLoggerTestMockNotifier implements DashboardNotifier {
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
   * This keeps track of events
   */
  private LinkedList<Event> eventSeq = new LinkedList<Event>();

  @Override
  public void devModeEvent(DevModeSession session, Event event) {
    eventSeq.add(event);
  }

  @Override
  public void devModeSession(DevModeSession session) {
    // always raise exception here - this method shouldn't be invoked from
    // SpeedTracerLogger
    Assert.assertTrue("SpeedTracerLogger should not be calling DashboardNotifier.devModeSession()",
        false);
  }

  public LinkedList<Event> getEventSequence() {
    return eventSeq;
  }
}
