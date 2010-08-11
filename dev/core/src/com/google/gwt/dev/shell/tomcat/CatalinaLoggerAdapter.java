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
package com.google.gwt.dev.shell.tomcat;

import com.google.gwt.core.ext.TreeLogger;

import org.apache.catalina.logger.LoggerBase;

class CatalinaLoggerAdapter extends LoggerBase {

  private final TreeLogger logger;

  public CatalinaLoggerAdapter(TreeLogger logger) {
    this.logger = logger;
  }

  @Override
  public void log(Exception exception, String msg) {
    logger.log(TreeLogger.WARN, msg, exception);
  }

  @Override
  public void log(String msg) {
    logger.log(TreeLogger.INFO, msg, null);
  }

  @Override
  public void log(String message, int verbosity) {
    TreeLogger.Type type = mapVerbosityToLogType(verbosity);
    logger.log(type, message, null);
  }

  @Override
  public void log(String msg, Throwable throwable) {
    logger.log(TreeLogger.WARN, msg, throwable);
  }

  @Override
  public void log(String message, Throwable throwable, int verbosity) {
    TreeLogger.Type type = mapVerbosityToLogType(verbosity);
    logger.log(type, message, throwable);
  }

  private TreeLogger.Type mapVerbosityToLogType(int verbosity) {
    switch (verbosity) {
      case LoggerBase.FATAL:
      case LoggerBase.ERROR:
      case LoggerBase.WARNING:
        return TreeLogger.WARN;

      case LoggerBase.INFORMATION:
        return TreeLogger.DEBUG;
      case LoggerBase.DEBUG:
        return TreeLogger.SPAM;

      default:
        // really, this was an unexpected type
        return TreeLogger.WARN;
    }
  }

}