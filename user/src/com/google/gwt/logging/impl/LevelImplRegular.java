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

package com.google.gwt.logging.impl;

import java.util.logging.Level;

/**
 * Implementation for the Level class when logging is enabled.
 */
public class LevelImplRegular implements LevelImpl {
  public Level parse(String name) {
    if (name.equalsIgnoreCase("ALL")) {
      return Level.ALL;
    } else if (name.equalsIgnoreCase("CONFIG")) {
      return Level.CONFIG;
    } else if (name.equalsIgnoreCase("FINE")) {
      return Level.FINE;
    } else if (name.equalsIgnoreCase("FINER")) {
      return Level.FINER;
    } else if (name.equalsIgnoreCase("FINEST")) {
      return Level.FINEST;
    } else if (name.equalsIgnoreCase("INFO")) {
      return Level.INFO;
    } else if (name.equalsIgnoreCase("OFF")) {
      return Level.OFF;
    } else if (name.equalsIgnoreCase("SEVERE")) {
      return Level.SEVERE;
    } else if (name.equalsIgnoreCase("WARNING")) {
      return Level.WARNING;
    }
    return null;
  }
}
