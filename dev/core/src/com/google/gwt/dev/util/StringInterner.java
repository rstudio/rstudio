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

package com.google.gwt.dev.util;

import com.google.gwt.thirdparty.guava.common.collect.Interner;
import com.google.gwt.thirdparty.guava.common.collect.Interners;
/**
 * The string interner singleton.
 *
 * <p>We don't use the String.intern() method because it would prevent GC and fill the PermGen
 * space. It also runs comparatively slowly </p>
 *
 */
public final class StringInterner {
  private static final Interner<String> instance = Interners.newWeakInterner();

  public static Interner<String> get() {
    return instance;
  }
}
