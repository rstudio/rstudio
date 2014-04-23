/*
 * Copyright 2014 Google Inc.
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

import com.google.gwt.core.ext.soyc.Range;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * An unmodifiable container of mappings from one JavaScript file to the
 * Java code it came from.
 *
 * (This class doesn't implement Map because we only use a few methods.)
 */
public class JsSourceMap {
  private final Map<Range, SourceInfo> delegate;

  public JsSourceMap(Map<Range, SourceInfo> delegate) {
    this.delegate = delegate;
  }

  public Set<Range> keySet() {
    return Collections.unmodifiableSet(delegate.keySet());
  }

  public SourceInfo get(Range key) {
    return delegate.get(key);
  }

  public int size() {
    return delegate.size();
  }
}
