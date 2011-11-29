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
 * Base class for command line argument handlers. 
 */
public abstract class ArgHandler {

  public String[] getDefaultArgs() {
    return null;
  }

  public abstract String getPurpose();

  public abstract String getTag();

  /**
   * A list of words representing the arguments in help text.
   */
  public abstract String[] getTagArgs();

  /**
   * Attempts to process one flag or "extra" command-line argument (that appears
   * without a flag).
   * @param args  the arguments passed in to main()
   * @param tagIndex  an index into args indicating the first argument to use.
   * If this is a handler for a flag argument. Otherwise it's the index of the
   * "extra" argument.
   * @return the number of additional arguments consumed, not including the flag or
   * extra argument. Alternately, returns -1 if the argument cannot be used. This will
   * causes the program to abort and usage to be displayed.
   */
  public abstract int handle(String[] args, int tagIndex);

  public boolean isRequired() {
    return false;
  }

  public boolean isUndocumented() {
    return false;
  }

}
