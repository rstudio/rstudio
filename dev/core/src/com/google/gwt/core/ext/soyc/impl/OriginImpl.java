/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.core.ext.soyc.impl;

import com.google.gwt.core.ext.soyc.Story.Origin;
import com.google.gwt.dev.jjs.SourceOrigin;

/**
 * An implementation of Origin, that initializes itself from a SourceOrigin.
 */
public class OriginImpl implements Origin, Comparable<OriginImpl> {

  private final int lineNum;
  private final String location;

  public OriginImpl(SourceOrigin origin) {
    this.location = origin.getFileName();
    this.lineNum = origin.getStartLine();
  }

  public int compareTo(OriginImpl o) {
    int a = location.compareTo(o.location);
    if (a != 0) {
      return a;
    }
    return lineNum - o.lineNum;
  }
  
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof OriginImpl)) {
      return false;
    }
    OriginImpl other = (OriginImpl) o;
    return location.equals(other.location) && lineNum == other.lineNum;
  }

  public int getLineNumber() {
    return lineNum;
  }

  public String getLocation() {
    return location;
  }
  
  @Override
  public int hashCode() {
    return location.hashCode() ^ lineNum;
  }

  @Override
  public String toString() {
    return location + " : " + lineNum;
  }
}