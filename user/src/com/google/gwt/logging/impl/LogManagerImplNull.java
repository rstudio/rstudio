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

import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Implementation for the LogManager class when logging is disabled.
 */
public class LogManagerImplNull {
  
  boolean addLogger(Logger logger) {
    return false;
  }
  
  Logger getLogger(String name) {
    return null;
  }
  
  LogManager getLogManager() {
    return null;
  }
}
