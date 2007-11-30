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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a bound for a {@link JTypeParameter} or a {@link JWildcardType}.
 */
public abstract class JBound {
  /**
   * Types which make up this bound.
   */
  private final List<JClassType> bounds = new ArrayList<JClassType>();

  JBound(JClassType[] bounds) {
    this.bounds.addAll(Arrays.asList(bounds));
  }

  public JClassType[] getBounds() {
    return bounds.toArray(TypeOracle.NO_JCLASSES);
  }

  public JClassType getFirstBound() {
    assert (!bounds.isEmpty());

    return bounds.get(0);
  }

  public String getQualifiedSourceName() {
    return toString(true);
  }

  public String getSimpleSourceName() {
    return toString(false);
  }

  public abstract JLowerBound isLowerBound();

  public abstract JUpperBound isUpperBound();

  @Override
  public String toString() {
    return getQualifiedSourceName();
  }

  abstract JClassType[] getSubtypes();

  abstract boolean isAssignableFrom(JClassType otherType);
  
  abstract boolean isAssignableTo(JClassType otherType);
  
  private String toString(boolean useQualifiedNames) {
    StringBuffer sb = new StringBuffer();

    if (isUpperBound() != null) {
      sb.append(" extends ");
    } else {
      sb.append(" super ");
    }

    boolean needsAmpersand = false;
    for (JClassType bound : bounds) {

      if (needsAmpersand) {
        sb.append(" & ");
      } else {
        needsAmpersand = true;
      }

      String name;
      if (useQualifiedNames) {
        name = bound.getParameterizedQualifiedSourceName();
      } else {
        name = bound.getSimpleSourceName();
      }

      sb.append(name);
    }

    return sb.toString();
  }
}
