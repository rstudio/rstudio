/*
 * Copyright 2014 Google Inc.
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

import java.util.logging.Logger;
/**
 * A simple {@link LoggerConfigurator} that configures the root logger to log to the console.
 * <p>
 * This is only used when the application doesn't depend on com.google.gwt.logging.Logging.
 */
class LoggerConfiguratorConsole implements LoggerConfigurator {

  @Override
  public void configure(Logger logger) {
    if (logger.getName().isEmpty()) {
      logger.addHandler(new SimpleConsoleLogHandler());
    }
  }
}
