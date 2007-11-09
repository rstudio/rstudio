/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.core.ext.typeinfo;

/**
 * Represents the extends bound used in {@link JTypeParameter}s and
 * {@link JWildcardType}s.
 */
public class JUpperBound extends JBound {
  public JUpperBound(JClassType upperBound) {
    this(new JClassType[] {upperBound});
  }

  /**
   * 
   */
  public JUpperBound(JClassType[] upperBounds) {
    super(upperBounds);
  }

  @Override
  public JLowerBound isLowerBound() {
    return null;
  }

  @Override
  public JUpperBound isUpperBound() {
    return this;
  }

  @Override
  JClassType[] getSubtypes() {
    return getFirstBound().getSubtypes();
  }

  @Override
  boolean isAssignableFrom(JBound possibleSubWildcard) {
    JClassType firstBound = getFirstBound();

    JUpperBound upperBound = possibleSubWildcard.isUpperBound();
    if (upperBound != null) {
      // Upper bound
      return firstBound.isAssignableFrom(upperBound.getFirstBound());
    }

    // Lower bound
    JClassType javaLangObject = firstBound.getOracle().getJavaLangObject();
    return firstBound == javaLangObject
        && possibleSubWildcard.getFirstBound() == javaLangObject;
  }
}
