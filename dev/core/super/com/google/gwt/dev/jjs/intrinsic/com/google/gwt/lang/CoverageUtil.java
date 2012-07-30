/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.lang;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Utilities used for code coverage, to be injected into the JavaScript AST.
 */
public class CoverageUtil {
  /**
   * An object whose keys are Java filenames and whose values are objects mapping
   * line numbers to 1 (executed) or 0 (not executed).
   */
  private static JavaScriptObject coverage = JavaScriptObject.createObject();

  /**
   * Updates the coverage object, marking the given filename-line number pair as executed.
   */
  public static native void cover(String filename, String lineNumber) /*-{
    @com.google.gwt.lang.CoverageUtil::coverage[filename][lineNumber] = 1;
  }-*/;

  /**
   * Reads existing coverage data from localStorage, merges it with data collected on this page,
   * and flushes it back to localStorage. This function is used as an onbeforeunload handler.
   */
  public static native void onBeforeUnload() /*-{
    var merge_coverage = function(x, y) {
      var merge = function(x, y, merger) {
        for (var key in y)
          if (x.hasOwnProperty(key))
            x[key] = merger(x[key], y[key]);
          else
            x[key] = y[key];
          return x;
      };

      merge(x, y, function(u, v) {
        return merge(u, v, Math.max);
      });
    };

    var $coverage = @com.google.gwt.lang.CoverageUtil::coverage;
    var coverage = JSON.parse(localStorage.getItem('gwt_coverage'));
    if (coverage !== null)
      merge_coverage($coverage, coverage);
    localStorage.setItem('gwt_coverage', JSON.stringify($coverage));
  }-*/;
}
