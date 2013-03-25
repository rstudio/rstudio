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
package com.google.gwt.dev.util.log.speedtracer;

import com.google.gwt.dev.shell.DevModeSession;
import com.google.gwt.dev.shell.DevModeSessionTestUtil;
import com.google.gwt.dev.util.log.dashboard.SpeedTracerLoggerTestMockNotifier;
import com.google.gwt.dev.util.log.dashboard.SpeedTracerLoggerTestMockNotifier.DevModeEvent;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.Properties;

/**
 * Flaky test for the SpeedTracerLogger class.
 *
 * Needs to run in its own test suite because of
 * https://code.google.com/p/google-web-toolkit/issues/detail?id=8081.
 */
public class SpeedTracerLoggerDashboardEnabledTest extends TestCase {

  public void testSpeedTracerWhenOnlyDashboardEnabled() {
    // backup system properties before making changes to them
    Properties props = (Properties) System.getProperties().clone();

    try {
      // no logging to file!
      System.clearProperty("gwt.speedtracerlog");
      // we don't capture GC events in dashboard, so setting this will allow us
      // to confirm that they *don't* show up in dashboard notices
      System.setProperty("gwt.speedtracer.logGcTime", "yes");

      // now enable the mock dashboard notifier
      SpeedTracerLoggerTestMockNotifier notifier = SpeedTracerLoggerTestMockNotifier.enable();

      // create "sessions"
      DevModeSession session1 = DevModeSessionTestUtil.createSession("test1", "test", true);
      DevModeSession session2 = DevModeSessionTestUtil.createSession("test2", "test", false);

      // expected values (used in final assertions below)
      LinkedList<DevModeEvent> expectedEvents = new LinkedList<DevModeEvent>();
      LinkedList<DevModeSession> expectedSessions = new LinkedList<DevModeSession>();

      Event evt1, evt2;

      // test events with no session specified
      evt1 = SpeedTracerLogger.start(DevModeEventType.MODULE_INIT, "k1", "v1", "k2", "v2");
      // also test that child events aren't posted (only top-level events)
      evt2 = SpeedTracerLogger.start(DevModeEventType.CLASS_BYTES_REWRITE);
      evt2.end();
      evt1.end();
      // expect only first event
      expectedEvents.add(new DevModeEvent(evt1));
      expectedSessions.add(session1); // event should get "default" session

      // now with session specified
      evt1 = SpeedTracerLogger.start(session2, DevModeEventType.JAVA_TO_JS_CALL, "k1", "v1");
      // also test that child events aren't posted (only top-level events)
      evt2 = SpeedTracerLogger.start(DevModeEventType.CREATE_UI);
      evt2.end();
      evt1.end();
      // expect only first event
      expectedEvents.add(new DevModeEvent(evt1));
      expectedSessions.add(session2);

      evt1 = SpeedTracerLogger.start(session1, DevModeEventType.JS_TO_JAVA_CALL, "k1", "v1");
      evt1.end();
      expectedEvents.add(new DevModeEvent(evt1));
      expectedSessions.add(session1);

      // Finally, assert that the events and corresponding sessions sent to the
      // notifier are exactly as expected
      assertEquals("Events posted to dashboard do not match expected events!", expectedEvents,
          notifier.getEventSequence());

      // Collect sessions associated with each event
      LinkedList<DevModeSession> actualSessions = new LinkedList<DevModeSession>();
      for (DevModeEvent event : notifier.getEventSequence()) {
        actualSessions.add(event.getDevModeSession());
      }

      // and confirm the sessions are correct
      assertEquals("Events posted to dashboard are associated with incorrect sessions!",
          expectedSessions, actualSessions);

    } finally {
      // restore system properties
      System.setProperties(props);
    }
  }
}
