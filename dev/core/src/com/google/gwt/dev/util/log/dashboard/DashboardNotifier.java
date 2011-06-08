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

/**
 * Sends information to a dashboard service. The dashboard service collects
 * information from GWT runtime and compiler instrumentation.
 */
public interface DashboardNotifier {
  // First: Compiler related entry points

  // TODO(jhumphries) Add interface methods for collecting data from the
  // compiler

  // Second: Devmode related entry points

  /**
   * Notifies the dashboard of a top-level event.
   */
  void devModeEvent(DevModeSession session, String eventType, long startTimeNanos,
      long durationNanos);

  /**
   * Notifies the dashboard of a new session starting.
   */
  void devModeSession(DevModeSession session);

  /**
   * Notifies the dashboard of a session ending.
   */
  void devModeSessionEnded(DevModeSession session);

  // Third: Test related entry points

  // TODO(jhumphries) Add interface methods for collecting data from automated
  // tests

}
