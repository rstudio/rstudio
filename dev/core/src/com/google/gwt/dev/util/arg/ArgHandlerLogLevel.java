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
import com.google.gwt.util.tools.ArgHandler;

/**
 * Argument handler for processing the log level flag.
 */
public class ArgHandlerLogLevel extends ArgHandler {

  private static final String OPTIONS_STRING = computeOptionsString();

  private static String computeOptionsString() {
    StringBuffer sb = new StringBuffer();
    TreeLogger.Type[] values = TreeLogger.Type.values();
    for (int i = 0, c = values.length; i < c; ++i) {
      if (i > 0) {
        sb.append(", ");
      }
      if (i + 1 == c) {
        sb.append("or ");
      }
      sb.append(values[i].name());
    }
    return sb.toString();
  }

  private final OptionLogLevel options;

  public ArgHandlerLogLevel(OptionLogLevel options) {
    this.options = options;
  }

  @Override
  public String[] getDefaultArgs() {
    return new String[] {getTag(), getDefaultLogLevel().name()};
  }

  @Override
  public String getPurpose() {
    return "The level of logging detail: " + OPTIONS_STRING;
  }

  @Override
  public String getTag() {
    return "-logLevel";
  }

  @Override
  public String[] getTagArgs() {
    return new String[] {"level"};
  }

  @Override
  public int handle(String[] args, int startIndex) {
    if (startIndex + 1 < args.length) {
      try {
        TreeLogger.Type level = TreeLogger.Type.valueOf(args[startIndex + 1]);
        options.setLogLevel(level);
        return 1;
      } catch (IllegalArgumentException e) {
        // Argument did not match any enum value; fall through to error case.
      }
    }

    System.err.println(getTag() + " should be followed by one of");
    System.err.println("  " + OPTIONS_STRING);
    return -1;
  }

  protected TreeLogger.Type getDefaultLogLevel() {
    return TreeLogger.INFO;
  }
}
