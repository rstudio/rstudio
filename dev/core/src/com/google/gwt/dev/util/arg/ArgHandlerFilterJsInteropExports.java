/*
 * Copyright 2016 Google Inc.
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
 * Add inclusion/exclusion patterns to the generation of JsInterop exports.
 */
public class ArgHandlerFilterJsInteropExports extends ArgHandler {
  private final OptionGenerateJsInteropExports options;

  public ArgHandlerFilterJsInteropExports(OptionGenerateJsInteropExports options) {
    this.options = options;
  }
  @Override
  public String getPurpose() {
    return "Include/exclude members and classes while generating JsInterop exports."
        + " Flag could be set multiple times to expand the pattern."
        + " (The flag has only effect if exporting is enabled via -generateJsInteropExports)";
  }

  @Override
  public String getTag() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String[] getTags() {
    return new String[] { "-includeJsInteropExports", "-excludeJsInteropExports" };
  }

  @Override
  public String getHelpTag() {
    return "-includeJsInteropExports/excludeJsInteropExports";
  }

  @Override
  public String[] getTagArgs() {
    return new String[] {"regex"};
  }

  @Override
  public int handle(String[] args, int startIndex) {
    if (startIndex + 1 >= args.length) {
      return -1;
    }

    String tagName = args[startIndex];
    boolean excludePattern = tagName.equals("-excludeJsInteropExports");
    if (excludePattern && options.getJsInteropExportFilter().isEmpty()) {
      System.err.println("-excludeJsInteropExports must be preceeded by -includeJsInteropExports");
      return -1;
    }

    String regex = args[startIndex + 1];
    if (regex.startsWith("+") || regex.startsWith("-")) {
      System.err.println(tagName + " regex cannot start with '+' or '-'");
      return -1;
    }

    try {
      options.getJsInteropExportFilter().add(excludePattern ? "-" + regex : regex);
    } catch (IllegalArgumentException e) {
      System.err.println(tagName + " regex is invalid: " + e.getMessage());
      return -1;
    }
    return 1;
  }
}
