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
// //////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2005 Oliver Burn
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
// //////////////////////////////////////////////////////////////////////////////
package com.google.gwt.checkstyle;

import com.puppycrawl.tools.checkstyle.checks.header.RegexpHeaderCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.Utils;

import org.apache.commons.beanutils.ConversionException;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Custom version of {@link RegexpHeaderCheck} that has hooks for a custom log handler (see
 * {@link CustomLogHandler}).
 * <p>
 * This is an exact copy of {@link RegexpHeaderCheck} with three exceptions:
 * <ol>
 * <li>{@link CustomLogHandler} has been added for custom log callbacks.</li>
 * <li>{@link #doChecks(CustomLogHandler)} has been added for custom checks. This method is an exact
 * copy of {@link RegexpHeaderCheck#beginTree(DetailAST)} except all log calls have been replaced
 * with a call to a custom log handler.</li>
 * <li>{@link #beginTree(DetailAST)} has been refactored to call
 * {@link #doChecks(CustomLogHandler)}.
 * </ol>
 */
public class CustomRegexpHeaderCheck extends RegexpHeaderCheck {
  /**
   * Custom log handler callback.
   */
  abstract static class CustomLogHandler {
    abstract void log(int aLine, String aKey);

    abstract void log(int aLine, String aKey, Object aObject);
  }

  // empty array to avoid instantiations.
  private static final int[] EMPTY_INT_ARRAY = new int[0];

  // the header lines to repeat (0 or more) in the check, sorted.
  private int[] mMultiLines = EMPTY_INT_ARRAY;

  // the compiled regular expressions
  private Pattern[] mHeaderRegexps;

  /**
   * {@inheritDoc}
   */
  public void beginTree(DetailAST aRootAST) {
    doChecks(new CustomLogHandler() {
      @Override
      void log(int aLine, String aKey) {
        CustomRegexpHeaderCheck.this.log(aLine, aKey);
      }
      @Override
      void log(int aLine, String aKey, Object aObject) {
        CustomRegexpHeaderCheck.this.log(aLine, aKey, aObject);
      }
    });
  }

  /**
   * Check the current file using the same method as {@link RegexpHeaderCheck#beginTree(DetailAST)}
   * but pass all logging calls through a custom log handler (@see {@link CustomLogHandler}).
   * 
   * @param logHandler the custom log handler, or <code>null</code> to suppress logging.
   */
  public void doChecks(CustomLogHandler logHandler) {
    // With the exception of the logging hooks, the following is copied from
    // RegexpHeaderCheck.beginTree().

    final int headerSize = getHeaderLines().length;
    final int fileSize = getLines().length;

    if (headerSize - mMultiLines.length > fileSize) {
      if (logHandler != null) {
        logHandler.log(1, "gwtheader.missing", null);
      }
    } else {
      int headerLineNo = 0;
      int i;
      for (i = 0; (headerLineNo < headerSize) && (i < fileSize); i++) {
        boolean isMatch = isMatch(i, headerLineNo);
        while (!isMatch && isMultiLine(headerLineNo)) {
          headerLineNo++;
          isMatch = (headerLineNo == headerSize) || isMatch(i, headerLineNo);
        }
        if (!isMatch) {
          if (logHandler != null) {
            logHandler.log(i + 1, "gwtheader.mismatch", getHeaderLines()[headerLineNo]);
          }
          break; // stop checking
        }
        if (!isMultiLine(headerLineNo)) {
          headerLineNo++;
        }
      }
      if (i == fileSize) {
        // if file finished, but we have at least one non-multi-line
        // header isn't completed
        for (; headerLineNo < headerSize; headerLineNo++) {
          if (!isMultiLine(headerLineNo)) {
            if (logHandler != null) {
              logHandler.log(1, "gwtheader.missing");
            }
            break;
          }
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setHeader(String aHeader) {
    super.setHeader(aHeader);
    initHeaderRegexps();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setHeaderFile(String aFileName) throws ConversionException {
    super.setHeaderFile(aFileName);
    initHeaderRegexps();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setMultiLines(int[] aList) {
    if ((aList == null) || (aList.length == 0)) {
      mMultiLines = EMPTY_INT_ARRAY;
      return;
    }

    mMultiLines = new int[aList.length];
    System.arraycopy(aList, 0, mMultiLines, 0, aList.length);
    Arrays.sort(mMultiLines);
  }

  /**
   * Initializes {@link #mHeaderRegexps} from {@link AbstractHeaderCheck#getHeaderLines()}.
   */
  private void initHeaderRegexps() {
    final String[] headerLines = getHeaderLines();
    if (headerLines != null) {
      mHeaderRegexps = new Pattern[headerLines.length];
      for (int i = 0; i < headerLines.length; i++) {
        try {
          // todo: Not sure if chache in Utils is still necessary
          mHeaderRegexps[i] = Utils.getPattern(headerLines[i]);
        } catch (final PatternSyntaxException ex) {
          throw new ConversionException("line " + i + " in header specification"
              + " is not a regular expression");
        }
      }
    }
  }

  /**
   * Checks if a code line matches the required header line.
   * 
   * @param aLineNo the line number to check against the header
   * @param aHeaderLineNo the header line number.
   * @return true if and only if the line matches the required header line.
   */
  private boolean isMatch(int aLineNo, int aHeaderLineNo) {
    final String line = getLines()[aLineNo];
    return mHeaderRegexps[aHeaderLineNo].matcher(line).find();
  }

  /**
   * @param aLineNo a line number
   * @return if <code>aLineNo</code> is one of the repeat header lines.
   */
  private boolean isMultiLine(int aLineNo) {
    return (Arrays.binarySearch(mMultiLines, aLineNo + 1) >= 0);
  }
}