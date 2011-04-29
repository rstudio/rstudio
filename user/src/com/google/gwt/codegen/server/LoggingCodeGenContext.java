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
package com.google.gwt.codegen.server;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base implementation of {@link CodeGenContext} which logs via
 * {@link java.util.logging.Logger}.
 * <p>
 * Experimental API - subject to change.
 */
public abstract class LoggingCodeGenContext implements CodeGenContext {

  private final Logger logger;

  protected LoggingCodeGenContext() {
    this(Logger.getAnonymousLogger());
  }

  protected LoggingCodeGenContext(String loggerName) {
    this(Logger.getLogger(loggerName));
  }

  protected LoggingCodeGenContext(Logger logger) {
    this.logger = logger;
  }

  public JavaSourceWriterBuilder addClass(String pkgName, String className) {
    return addClass(null, pkgName, className);
  }

  public abstract JavaSourceWriterBuilder addClass(String superPkg, String pkgName,
      String className);

  public void error(String msg) {
    logger.log(Level.SEVERE, msg);
  }

  public void error(String msg, Throwable cause) {
    logger.log(Level.SEVERE, msg, cause);
  }

  public void error(Throwable cause) {
    logger.log(Level.SEVERE, cause.getMessage(), cause);
  }

  public void warn(String msg) {
    logger.log(Level.WARNING, msg);
  }

  public void warn(String msg, Throwable cause) {
    logger.log(Level.WARNING, msg);
  }

  public void warn(Throwable cause) {
    logger.log(Level.WARNING, cause.getMessage(), cause);
  }
}
