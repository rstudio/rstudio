/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.validation.client.constraints;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Pattern.Flag;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * {@link Pattern} constraint validator implementation.
 * <p>
 * Note this implementation uses {@link RegExp} which differs from
 * {@link java.util.regex.Pattern}.
 */
public class PatternValidator implements
    ConstraintValidator<Pattern, String> {
  private RegExp pattern;

  public final void initialize(Pattern annotation) {
    Pattern.Flag flags[] = annotation.flags();
    String flagString = "";
    for (Pattern.Flag flag : flags) {
      flagString += toString(flag);
    }
    pattern = RegExp.compile(annotation.regexp(), flagString);
  }

  public final boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    MatchResult match = pattern.exec(value);
    if (match == null) {
      return false;
    }
    // Must match the entire string
    return match.getGroup(0).length() == value.length();
  }

  private final String toString(Flag flag) {
    String value;
    switch (flag) {
      case CASE_INSENSITIVE:
      case UNICODE_CASE:
        value = "i";
        break;
      case MULTILINE:
        value = "m";
        break;
      default:
        throw new IllegalArgumentException(flag
            + " is not a suppoted gwt Pattern (RegExp) flag");
    }
    return value;
  }
}
