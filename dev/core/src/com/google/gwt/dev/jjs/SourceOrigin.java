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
package com.google.gwt.dev.jjs;

import org.apache.commons.collections.map.ReferenceMap;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * Describes where a SourceInfo's node came from. This class currently includes
 * only physical origin information, but could be extended to provide support
 * for source-Module and -Generators.
 */
public final class SourceOrigin implements Serializable {
  /**
   * This is synchronized since several threads could operate on it at once
   * during parallel optimization phases.
   */
  @SuppressWarnings("unchecked")
  private static final Map<SourceOrigin, SourceOrigin> CANONICAL_SOURCE_ORIGINS = Collections.synchronizedMap(new ReferenceMap(
      ReferenceMap.WEAK, ReferenceMap.SOFT));

  /**
   * Creates SourceOrigin nodes. This factory method will attempt to provide
   * canonicalized instances of SourceOrigin objects.
   */
  public static SourceOrigin create(int startPos, int endPos, int startLine,
      String fileName) {

    SourceOrigin newInstance = new SourceOrigin(fileName, startLine, startPos,
        endPos);
    SourceOrigin canonical = CANONICAL_SOURCE_ORIGINS.get(newInstance);

    assert canonical == null
        || (newInstance != canonical && newInstance.equals(canonical));

    if (canonical != null) {
      return canonical;
    } else {
      CANONICAL_SOURCE_ORIGINS.put(newInstance, newInstance);
      return newInstance;
    }
  }

  // TODO: Add Module and Generator tracking
  private final int endPos;
  private final String fileName;
  private final int hash;
  private final int startLine;
  private final int startPos;

  private SourceOrigin(String location, int startLine, int startPos, int endPos) {
    this.fileName = location;
    this.startLine = startLine;
    this.startPos = startPos;
    this.endPos = endPos;

    // Go ahead and compute the hash, since it'll be used for canonicalization
    this.hash = 13 * endPos + 17 * fileName.hashCode() + 29 * startLine + 31
        * startPos + 2;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SourceOrigin)) {
      return false;
    }
    SourceOrigin other = (SourceOrigin) o;
    return endPos == other.endPos && fileName.equals(other.fileName)
        && startLine == other.startLine && startPos == other.startPos;
  }

  public int getEndPos() {
    return endPos;
  }

  public String getFileName() {
    return fileName;
  }

  public int getStartLine() {
    return startLine;
  }

  public int getStartPos() {
    return startPos;
  }

  @Override
  public int hashCode() {
    return hash;
  }
}