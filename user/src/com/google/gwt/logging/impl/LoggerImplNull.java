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
 * Null implementation for the Logger class which ensures that calls to Logger
 * compile out when logging is disabled.
 */
public class LoggerImplNull implements LoggerImpl {

  public void addHandler(Handler handler) { 
    // Do nothing
  }

  public void config(String msg) { 
    // Do nothing
  }

  public void fine(String msg) { 
    // Do nothing
  }

  public void finer(String msg) { 
    // Do nothing
  }

  public void finest(String msg) { 
    // Do nothing
  }
  
  public Handler[] getHandlers() {
    return null;
  }

  public Level getLevel() {
    return null;
  }
  
  public Logger getLoggerHelper(String name) {
    // We need to return an actual logger here rather than null, because client
    // code will make calls like Logger.getLogger().log(), and we don't want a
    // NullPointerException. Since a LoggerImplNull will be behind this logger
    // that we are returning, that will ensure that the log() call does nothing
    // and gets compiled out.
    return new LoggerWithExposedConstructor("");
  }
  
  public String getName() {
    return "";
  }
  
  public Logger getParent() {
    return null;
  }
  
  public boolean getUseParentHandlers() {
    return false;
  }
  
  public void info(String msg) {
    // Do nothing
  }
  
  public boolean isLoggable(Level messageLevel) {
    return false;
  }
  
  public void log(Level level, String msg) {
    // Do nothing  
  }
  
  public void log(Level level, String msg, Throwable thrown) {
    // Do nothing
  }

  public void log(LogRecord record) {
    // Do nothing
  }

  public void removeHandler(Handler handler) { 
    // Do nothing
  }

  public void setLevel(Level newLevel) { 
    // Do nothing
  }

  public void setName(String newName) { 
    // Do nothing
  }

  public void setParent(Logger newParent) { 
    // Do nothing
  }

  public void setUseParentHandlers(boolean newUseParentHandlers) { 
    // Do nothing
  }

  public void severe(String msg) { 
    // Do nothing
  }

  public void warning(String msg) { 
    // Do nothing
  }  
}
