/*
 * Copyright 2015 Google Inc.
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
package java.util.logging;

import javaemul.internal.ConsoleLogger;

/**
 * A simple console logger used in super dev mode.
 */
class SimpleConsoleLogHandler extends Handler {

  @Override
  public void publish(LogRecord record) {
    ConsoleLogger console = ConsoleLogger.createIfSupported();
    if (console == null) {
      return;
    }
    if (!isLoggable(record)) {
      return;
    }

    String level = toConsoleLogLevel(record.getLevel());
    console.log(level, record.getMessage());
    if (record.getThrown() != null) {
      console.log(level, record.getThrown());
    }
  }

  private String toConsoleLogLevel(Level level) {
    int val = level.intValue();
    if (val >= Level.SEVERE.intValue()) {
      return "error";
    } else if (val >= Level.WARNING.intValue()) {
      return "warn";
    } else if (val >= Level.INFO.intValue()) {
      return "info";
    } else {
      return "log";
    }
  }

  @Override
  public void close() {
    // No action needed
  }

  @Override
  public void flush() {
    // No action needed
  }
}