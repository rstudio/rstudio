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
package com.google.gwt.dev.util.log.dashboard;

import com.google.gwt.dev.shell.DevModeSession;

import junit.framework.TestCase;

/**
 * Tests the DashboardNotifierFactory class.
 */
public class DashboardNotifierFactoryTest extends TestCase {

  public void testSetNotifier() {
    // create test notifier instance
    DashboardNotifier obj = new DashboardNotifier() {
      @Override
      public void devModeEventBegin() {
        // no need to do anything
      }

      @Override
      public void devModeEventEnd(DevModeSession session, String eventType, long startTimeNanos,
          long durationNanos) {
        // no need to do anything
      }

      @Override
      public void devModeSessionBegin(DevModeSession session) {
        // no need to do anything
      }

      @Override
      public void devModeSessionEnd(DevModeSession session) {
        // no need to do anything
      }
    };
    // call method
    DashboardNotifierFactory.setNotifier(obj);
    // verify it worked
    assertTrue("Notifier is not set correctly!", DashboardNotifierFactory.getNotifier() == obj);
    assertTrue("Setting notifier failed to enable notifications!", DashboardNotifierFactory
        .areNotificationsEnabled());
  }

  public void testClearNotifier() {
    // clearing the notifier should use a "no-op" instance and disable
    // notifications
    DashboardNotifierFactory.setNotifier(null);
    // verify it worked
    assertTrue("Notifier is of wrong type!",
        DashboardNotifierFactory.getNotifier() instanceof NoOpDashboardNotifier);
    assertFalse("Resetting notifier failed to disable notifications!", DashboardNotifierFactory
        .areNotificationsEnabled());
  }

  public void testCreateNotifier() {
    DashboardNotifier notifier =
        DashboardNotifierFactory.createNotifier(SpeedTracerLoggerTestMockNotifier.class.getName());
    assertNotNull("Notifier could not be created!", notifier);
    assertTrue(notifier instanceof SpeedTracerLoggerTestMockNotifier);
  }

  public void testCreateNotifierBadClass() {
    assertNull(DashboardNotifierFactory.createNotifier("this.is.not.a.valid.Notifier"));
  }
}
