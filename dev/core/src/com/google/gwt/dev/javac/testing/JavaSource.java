/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.dev.javac.testing;

import com.google.gwt.dev.javac.Shared;

/**
 * Convenience base class for java sources used during testing.
 */
public abstract class JavaSource implements Source {

  private final String path;

  /**
   * Constructs a new source with the given fully qualified java type name.
   *
   * @param typeName fully qualified java type name, cannot be a nested class
   */
  public JavaSource(String typeName) {
    this.path = Shared.toPath(typeName);
  }

  public String getPath() {
    return path;
  }
}
