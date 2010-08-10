/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.user.client;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;

/**
 * Encapsulates an action for later execution, often from a different context.
 * 
 * <p>
 * The Command interface provides a layer of separation between the code
 * specifying some behavior and the code invoking that behavior. This separation
 * aids in creating reusable code. For example, a
 * {@link com.google.gwt.user.client.ui.MenuItem} can have a Command associated
 * with it that it executes when the menu item is chosen by the user.
 * Importantly, the code that constructed the Command to be executed when the
 * menu item is invoked knows nothing about the internals of the MenuItem class
 * and vice-versa.
 * </p>
 * 
 * <p>
 * The Command interface is often implemented with an anonymous inner class. For
 * example,
 * 
 * <pre>
 * Command sayHello = new Command() {
 *   public void execute() {
 *     Window.alert("Hello");
 *   }
 * };
 * sayHello.execute();
 * </pre>
 * 
 * </p>
 */
public interface Command extends ScheduledCommand {
  /*
   * NB: This extends ScheduledCommand to aid in transitioning from the old
   * DeferredCommand interface. We can't have a class in core depend on a class
   * in user, but we'd like switching to the Scheduler API to be pretty much
   * just a name change. This type isn't deprecated, because it's used by other
   * UI widget classes.
   */

  /**
   * Causes the Command to perform its encapsulated behavior.
   * */
  void execute();
}
