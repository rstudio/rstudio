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
 * Represents the super bound used in {@link JWildcardType}s.
 */
public class JLowerBound extends JBound {
  /**
   * 
   */
  public JLowerBound(JClassType lowerBound) {
    this(new JClassType[] {lowerBound});
  }

  /**
   * 
   */
  public JLowerBound(JClassType[] lowerBounds) {
    super(lowerBounds);
  }

  @Override
  public JLowerBound isLowerBound() {
    return this;
  }

  @Override
  public JUpperBound isUpperBound() {
    return null;
  }

  @Override
  JClassType[] getSubtypes() {
    return TypeOracle.NO_JCLASSES;
  }

  @Override
  boolean isAssignableFrom(JBound possibleSubWildcard) {
    JLowerBound lowerBound = possibleSubWildcard.isLowerBound();
    if (lowerBound != null) {
      return lowerBound.getFirstBound().isAssignableFrom(getFirstBound());
    }

    return false;
  }
}
