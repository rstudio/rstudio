/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.checkstyle;

import com.google.gwt.checkstyle.CustomRegexpHeaderCheck.CustomLogHandler;

import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.DetailAST;

/**
 * Checks the header against one of two header options, to support GWT's 80 and 100 column headers.
 * <p>
 * To use, set <code>&lt;property name="header" value="[regular expression]" /&gt;</code> and
 * <code>&lt;property name="headerAlt" value="[alternate regular expression]" /&gt;</code>
 */
public class GwtHeaderCheck extends Check {
  private CustomRegexpHeaderCheck regexpChecker = new CustomRegexpHeaderCheck();
  private CustomRegexpHeaderCheck regexpCheckerAlt = new CustomRegexpHeaderCheck();

  public void beginTree(DetailAST aRootAST) {
    final boolean[] passedChecks = new boolean[]{true};
    regexpChecker.setFileContents(this.getFileContents());
    regexpChecker.doChecks(new CustomLogHandler() {
      @Override
      void log(int aLine, String aKey) {
        passedChecks[0] = false;
      }

      @Override
      void log(int aLine, String aKey, Object aObject) {
        passedChecks[0] = false;
      }
    });
    if (passedChecks[0]) {
      // regexpChecker passed, no need to run alternate checker
      return;
    }

    regexpCheckerAlt.setFileContents(this.getFileContents());
    regexpCheckerAlt.doChecks(new CustomLogHandler() {
      @Override
      void log(int aLine, String aKey) {
        GwtHeaderCheck.this.log(aLine, aKey);
      }

      @Override
      void log(int aLine, String aKey, Object aObject) {
        GwtHeaderCheck.this.log(aLine, aKey, aObject);
      }
    });
  }

  @Override
  public int[] getDefaultTokens() {
    return new int[0];
  }

  /**
   * Set the header to check against. Individual lines in the header must be separated by '\n'
   * characters.
   * 
   * @param aHeader header content to check against.
   */
  public void setHeader(String aHeader) {
    regexpChecker.setHeader(aHeader);
  }

  /**
   * Set the alternate header to check against. Individual lines in the header must be separated by
   * '\n' characters.
   * 
   * @param aHeader header content to check against.
   */
  public void setHeaderAlt(String aHeader) {
    regexpCheckerAlt.setHeader(aHeader);
  }
}