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
package com.google.gwt.util.tools;

/**
 * Argument handler for flags that take an integer as their parameter. 
 */
public abstract class ArgHandlerInt extends ArgHandler {

  @Override
  public int handle(String[] args, int startIndex) {
    int value;
    if (startIndex + 1 < args.length) {
      try {
        value = Integer.parseInt(args[startIndex + 1]);
      } catch (NumberFormatException e) {
        // fall-through
        value = -1;
      }

      setInt(value);
      return 1;
    }

    System.err.println(getTag() + " should be followed by an integer");
    return -1;
  }

  @Override
  public boolean isRequired() {
    return false;
  }

  public abstract void setInt(int value);
}
