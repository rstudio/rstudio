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
 * Null implementation for the Level class which ensures that calls to Level
 * compile out when logging is disabled.
 */
public class LevelImplNull implements LevelImpl {

  public Level all() {
    return null;
  }

  public Level config() {
    return null;
  }

  public Level fine() {
    return null;
  }

  public Level finer() {
    return null;
  }

  public Level finest() {
    return null;
  }

  public String getName() {
    return null;
  }

  public Level info() {
    return null;
  }

  public int intValue() {
    return 0;
  }

  public Level off() {
    return null;
  }

  public void setName(String newName) {
  }

  public void setValue(int newValue) {
  }

  public Level severe() {
    return null;
  }

  public Level warning() {
    return null;
  }

}
