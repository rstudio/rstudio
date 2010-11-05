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
package com.google.gwt.autobean.shared.impl;

import com.google.gwt.autobean.shared.Splittable;

import java.util.List;

/**
 * Holds a string payload with the expectation that the object will be used only
 * for creating a larger payload.
 */
public class LazySplittable implements Splittable {
  public static final Splittable NULL = new LazySplittable("null");

  private final String payload;
  private Splittable split;

  public LazySplittable(String payload) {
    this.payload = payload;
  }

  public String asString() {
    maybeSplit();
    return split.asString();
  }

  public Splittable get(int index) {
    maybeSplit();
    return split.get(index);
  }

  public Splittable get(String key) {
    maybeSplit();
    return split.get(key);
  }

  public String getPayload() {
    return payload;
  }

  public List<String> getPropertyKeys() {
    maybeSplit();
    return split.getPropertyKeys();
  }

  public boolean isNull(int index) {
    maybeSplit();
    return split.isNull(index);
  }

  public boolean isNull(String key) {
    maybeSplit();
    return split.isNull(key);
  }

  public int size() {
    maybeSplit();
    return split.size();
  }

  private void maybeSplit() {
    if (split == null) {
      split = StringQuoter.split(payload);
    }
  }
}
