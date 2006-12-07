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

import com.google.gwt.util.tools.ArgHandler;

/**
 * Argument handler for processing the script style flag.
 */
public abstract class ArgHandlerScriptStyle extends ArgHandler {

  public String[] getDefaultArgs() {
    return new String[]{"-style", "obfuscate"};
  }

  public String getPurpose() {
    return "Script output style: OBF[USCATED], PRETTY, or DETAILED (defaults to OBF)";
  }

  public String getTag() {
    return "-style";
  }

  public String[] getTagArgs() {
    return new String[]{"style"};
  }

  public int handle(String[] args, int startIndex) {
    if (startIndex + 1 < args.length) {
      String style = args[startIndex + 1].toLowerCase();
      if (style.startsWith("obf")) {
        setStyleObfuscated();
        return 1;
      } else if ("pretty".equals(style)) {
        setStylePretty();
        return 1;
      } else if ("detailed".equals(style)) {
        setStyleDetailed();
        return 1;
      }
    }

    System.err.println(getTag() + " should be followed by one of");
    System.err.println("  OBF, PRETTY, or DETAILED");
    return -1;
  }

  public abstract void setStyleDetailed();

  public abstract void setStyleObfuscated();

  public abstract void setStylePretty();
}