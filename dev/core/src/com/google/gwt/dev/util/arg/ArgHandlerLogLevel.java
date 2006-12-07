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
package com.google.gwt.dev.util.arg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.util.tools.ArgHandler;

/**
 * Arugment handler for processing the log level flag.
 */
public abstract class ArgHandlerLogLevel extends ArgHandler {

  public String[] getDefaultArgs() {
    return new String[]{getTag(), "INFO"};
  }

  public String getPurpose() {
    return "The level of logging detail: ERROR, WARN, INFO, TRACE, DEBUG, SPAM, or ALL";
  }

  public String getTag() {
    return "-logLevel";
  }

  public String[] getTagArgs() {
    return new String[]{"level"};
  }

  public int handle(String[] args, int startIndex) {
    if (startIndex + 1 < args.length) {
      TreeLogger.Type level = TreeLogger.Type.valueOf(args[startIndex + 1]);
      if (level != null) {
        setLogLevel(level);
        return 1;
      }
    }

    System.err.println(getTag() + " should be followed by one of");
    System.err.println("  ERROR, WARN, INFO, TRACE, DEBUG, SPAM, or ALL");
    return -1;
  }

  public abstract void setLogLevel(Type level);

}
