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

/**
 * Gets an instance of {@link DashboardNotifier} for sending data to a GWT
 * dashboard.
 */
public class DashboardNotifierFactory {
  private static final NoOpDashboardNotifier defaultNotifier = new NoOpDashboardNotifier();
  private static DashboardNotifier theNotifier;

  static {
    setNotifier(createNotifier(System.getProperty("gwt.dashboard.notifierClass")));
  }

  /**
   * Determines whether notifications to a GWT Dashboard are enabled. Returns
   * true if the current notifier is <strong>not</strong> the default "no-op"
   * instance.
   */
  public static boolean areNotificationsEnabled() {
    return theNotifier != defaultNotifier;
  }

  /**
   * Returns an instance of {@code DashboardNotifier} for sending data to a GWT
   * dashboard.
   */
  public static DashboardNotifier getNotifier() {
    return theNotifier;
  }

  /**
   * Creates a {@code DashboardNotifier} from a given class name. The object is
   * instantiated via reflection.
   * 
   * @return the new notifier instance if the creation was successful or null on
   *         failure
   */
  static DashboardNotifier createNotifier(String className) {
    // Create the instance!
    DashboardNotifier notifier = null;
    if (className != null) {
      try {
        Class<?> clazz = Class.forName(className);
        notifier = (DashboardNotifier) clazz.newInstance();
      } catch (Exception e) {
        // print error and skip dashboard notifications...
        new Exception("Unexpected failure while trying to load dashboard class: " + className
            + ". Notifications to the dashboard will be disabled.", e).printStackTrace();
        return null;
      }
    }
    return notifier;
  }

  /**
   * Defines the {@code DashboardNotifier} returned by this factory. Exposed for
   * unit testing purposes (to support mock notifier objects). If set to null, a
   * default notifier (whose methods do nothing) will be returned by subsequent
   * calls to {@code getNotifier}, not null.
   */
  static void setNotifier(DashboardNotifier notifier) {
    theNotifier = notifier == null ? defaultNotifier : notifier;
  }

  /**
   * Prevents this class from being instantiated. Instead use static method
   * {@link #getNotifier()}.
   */
  private DashboardNotifierFactory() {
  }
}
