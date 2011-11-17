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

package com.google.gwt.logging.impl;

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Implementation for the Logger class when logging is enabled only at Warning and above.
 */
public class LoggerImplWarning extends LoggerImplRegular {
  @Override
  public void config(String msg) {
    // Do nothing
  }

  @Override
  public void fine(String msg) {
    // Do nothing
  }

  @Override
  public void finer(String msg) {
    // Do nothing
  }

  @Override
  public void finest(String msg) {
    // Do nothing
  }

  @Override
  public void info(String msg) {
    // Do nothing
  }

  @Override
  public void log(Level level, String msg) {
    if (level.intValue() >= Level.WARNING.intValue()) {
      super.log(level, msg);
    }
  }

  @Override
  public void log(Level level, String msg, Throwable thrown) {
    if (level.intValue() >= Level.WARNING.intValue()) {
      super.log(level, msg, thrown);
    }
  }

  @Override
  public void log(LogRecord record) {
    if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
      super.log(record);
    }
  }

  @Override
  public void severe(String msg) {
    super.severe(msg);
  }

  @Override
  public void warning(String msg) {
    super.warning(msg);
  }
}
