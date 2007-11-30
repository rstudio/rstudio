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
  boolean isAssignableFrom(JClassType otherType) {
    JWildcardType wildcard = otherType.isWildcard();
    if (wildcard != null) {
      if (wildcard.getBounds().isLowerBound() != null) {
        /*
         * Upper bounds can be assigned from lower bounds if both of their
         * bounding types are Object.
         */
        JClassType firstBound = getFirstBound();
        JClassType javaLangObject = firstBound.getOracle().getJavaLangObject();
        return firstBound == javaLangObject && wildcard.getFirstBound() == javaLangObject;
      }

      return getFirstBound().isAssignableFrom(wildcard.getFirstBound());
    } else {
      /*
       * This bound can be assigned from another type if each of the bounding
       * types is assignable from the other type.
       */
      JClassType[] bounds = getBounds();
      for (JClassType bound : bounds) {
        if (!bound.isAssignableFrom(otherType)) {
          return false;
        }
      }
    }
    
    return true;
  }
  
  @Override 
  boolean isAssignableTo(JClassType otherType) {
    JClassType[] bounds = getBounds();
    for (JClassType bound : bounds) {
      if (bound.isAssignableTo(otherType)) {
        return true;
      }
    }
    
    return false;
  }
}
