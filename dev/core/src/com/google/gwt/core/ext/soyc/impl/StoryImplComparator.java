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

import com.google.gwt.core.ext.soyc.Story;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Exists as a real class to allow TreeMaps to be serialized.
 */
public class StoryImplComparator implements Comparator<Story>, Serializable {
  @Override
  public int compare(Story o1, Story o2) {
    return ((StoryImpl) o1).getId() - ((StoryImpl) o2).getId();
  }

  /**
   * Use the singleton instance.
   */
  private Object readResolve() {
    return StoryImpl.ID_COMPARATOR;
  }
}
