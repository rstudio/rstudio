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

package com.google.gwt.requestfactory.server;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side object that handles log messages sent by
 * {@link RequestFactoryLogHandler}.
 */
public class Logging {
  private static Logger logger = Logger.getLogger(Logging.class.getName());
  
  public static Long logMessage(
      String levelString, String loggerName, String originalMessage) {
    Level level = Level.SEVERE;
    try {
      level = Level.parse(levelString);
    } catch (IllegalArgumentException e) {
      return 0L;
    }
    String message = String.format("Client Side Logger: %s Message: %s",
        loggerName, originalMessage);
    
    logger.log(level, message);
    return 1L;
  }
  
  private Long id = 0L;
  private Integer version = 0;
  
  public Long getId() {
    return this.id;
  }
  
  public Integer getVersion() {
    return this.version;
  }
    
  public void setId(Long id) {
    this.id = id;
  }
  
  public void setVersion(Integer version) {
    this.version = version;
  }
}

