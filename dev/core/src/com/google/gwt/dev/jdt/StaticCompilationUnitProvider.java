/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.jdt;

import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;

/**
 * Implements a {@link CompilationUnitProvider} as transient (in-memory) source.
 */
public class StaticCompilationUnitProvider implements CompilationUnitProvider {

  private final String packageName;

  private final String simpleTypeName;

  private final char[] source;

  /**
   * @param source if <code>null</code>, override this class and return
   *          source from {@link #getSource()}
   */
  public StaticCompilationUnitProvider(String packageName,
      String simpleTypeName, char[] source) {
    this.packageName = packageName;
    this.simpleTypeName = simpleTypeName;
    this.source = source;
  }

  /**
   * Stubbed to return the same value every time.
   */
  public long getLastModified() {
    return 0;
  }

  /**
   * Creates a stable name for this compilation unit.
   */
  public final String getLocation() {
    return "transient source for " + packageName + "." + simpleTypeName;
  }

  public String getPackageName() {
    return packageName;
  }

  public char[] getSource() {
    return source;
  }

  public String getTypeName() {
    return simpleTypeName;
  }

  public boolean isTransient() {
    return true;
  }

  public String toString() {
    return getLocation();
  }
}
