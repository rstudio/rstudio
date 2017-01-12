/*
 * Copyright 2009 Google Inc.
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

package com.google.gwt.util.regexfilter;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This class implements filters that are configured with a sequence of regexes.
 * Each regex in the sequence can be preceded by a + or a - to indicate whether
 * it indicates that queries matching the regex should be included or excluded.
 * Concrete subclasses indicate the default behaviors by overriding
 * {@link #acceptByDefault()} and {@link #entriesArePositiveByDefault()}.
 */
public abstract class RegexFilter {
  private final List<String> values;
  private final ArrayList<Pattern> typePatterns;
  private final ArrayList<Boolean> includeType;

  public RegexFilter(TreeLogger logger, List<String> values)
      throws UnableToCompleteException {
    this.values = values;
    int size = values.size();
    typePatterns = new ArrayList<Pattern>(size);
    includeType = new ArrayList<Boolean>(size);

    // TODO investigate grouping multiple patterns into a single regex
    for (String regex : values) {
      // Patterns that don't start with [+-] are considered to be [-]
      boolean include = entriesArePositiveByDefault();
      // Ignore empty regexes
      if (regex.length() == 0) {
        logger.log(TreeLogger.ERROR, "Got empty blacklist entry");
        throw new UnableToCompleteException();
      }
      char c = regex.charAt(0);
      if (c == '+' || c == '-') {
        regex = regex.substring(1); // skip initial character
        include = (c == '+');
      }
      try {
        Pattern p = Pattern.compile(regex);
        typePatterns.add(p);
        includeType.add(include);

        if (logger.isLoggable(TreeLogger.DEBUG)) {
          logger.log(TreeLogger.DEBUG, "Got filter entry '" + regex + "'");
        }
      } catch (PatternSyntaxException e) {
        logger.log(TreeLogger.ERROR, "Got malformed filter entry '" + regex
            + "'");
        throw new UnableToCompleteException();
      }
    }
  }

  public boolean isIncluded(TreeLogger logger, String query) {
    logger = logger.branch(TreeLogger.DEBUG, "Considering query " + query);

    // Process patterns in reverse order for early exit
    int size = typePatterns.size();
    for (int idx = size - 1; idx >= 0; idx--) {
      if (logger.isLoggable(TreeLogger.DEBUG)) {
        logger.log(TreeLogger.DEBUG, "Considering filter rule "
            + values.get(idx) + " for query " + query);
      }
      boolean include = includeType.get(idx);
      Pattern pattern = typePatterns.get(idx);
      if (pattern.matcher(query).matches()) {
        if (include) {
          if (logger.isLoggable(TreeLogger.DEBUG)) {
            logger.log(TreeLogger.DEBUG, "Whitelisting " + query
                + " according to rule " + values.get(idx));
          }
          return true;
        } else {
          if (logger.isLoggable(TreeLogger.DEBUG)) {
            logger.log(TreeLogger.DEBUG, "Blacklisting " + query
                + " according to rule " + values.get(idx));
          }
          return false;
        }
      }
    }

    // None of the patterns matched
    return acceptByDefault();
  }

  /**
   * If no pattern matches, whether the query should be considered as an accept.
   */
  protected abstract boolean acceptByDefault();

  /**
   * If a pattern is not preceded by + or -, whether the query should be
   * considered positive.
   */
  protected abstract boolean entriesArePositiveByDefault();
}
