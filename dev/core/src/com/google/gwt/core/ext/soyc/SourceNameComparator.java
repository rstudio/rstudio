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
package com.google.gwt.core.ext.soyc;

import java.io.Serializable;
import java.util.Comparator;

/**
 * This is a top-level type so that we can serialize any TreeMaps that happen to
 * use {@link Member#SOURCE_NAME_COMPARATOR}.
 */
class SourceNameComparator implements Comparator<Member>, Serializable {
  public int compare(Member o1, Member o2) {
    return o1.getSourceName().compareTo(o2.getSourceName());
  }

  /**
   * Always use the singleton instance.
   */
  private Object readResolve() {
    return Member.SOURCE_NAME_COMPARATOR;
  }
}