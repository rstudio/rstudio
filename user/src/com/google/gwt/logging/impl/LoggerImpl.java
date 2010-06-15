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

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Interface for the implementation of Logger. We use a LoggerImplNull to ensure
 * that logging code compiles out when logging is disabled, and a
 * LoggerImplRegular to provide normal functionality when logging is enabled.
 */
public interface LoggerImpl {
  void addHandler(Handler handler);
  void config(String msg);
  void fine(String msg);
  void finer(String msg);
  void finest(String msg);
  
  /**
   * Get the handlers attached to this logger.
   * @return the array of handlers, or null if there are no handlers
   */
  Handler[] getHandlers();

  Level getLevel();
  Logger getLoggerHelper(String name);
  String getName();
  Logger getParent();
  boolean getUseParentHandlers();
  void info(String msg);
  boolean isLoggable(Level messageLevel);
  void log(Level level, String msg);
  void log(Level level, String msg, Throwable thrown);
  void log(LogRecord record);
  void removeHandler(Handler handler);
  void setLevel(Level newLevel);
  void setName(String newName);
  void setParent(Logger newParent);
  void setUseParentHandlers(boolean newUseParentHandlers);
  void severe(String msg);
  void warning(String msg);
}
