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

package com.google.gwt.sample.logexample.shared;

import java.util.logging.Logger;

/**
 * Shared code which does a simple logging call. Used to demonstrate how
 * shared code will log to different handlers depending on whether it is called
 * from server or client side code.
 */
public class SharedClass {
  private static Logger logger = Logger.getLogger(SharedClass.class.getName());

  public static void doSomething(String source) {
    // Pretend that an error occured
    logger.severe("SharedClass.doSomething() encountered a (pretend) error " +
        "when it was called from the " + source);
  }
}
