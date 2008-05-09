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
package com.google.gwt.dev.javac;

/**
 * Provides information about a single Java source file.
 */
public abstract class JavaSourceFile {

  /**
   * Overridden to finalize; always returns object identity.
   */
  @Override
  public final boolean equals(Object obj) {
    return super.equals(obj);
  }

  /**
   * Returns the user-relevant location of the source file. No programmatic
   * assumptions should be made about the return value.
   */
  public abstract String getLocation();

  /**
   * Returns the name of the package.
   */
  public abstract String getPackageName();

  /**
   * Returns the unqualified name of the top level public type.
   */
  public abstract String getShortName();

  /**
   * Returns the fully-qualified name of the top level public type.
   */
  public abstract String getTypeName();

  /**
   * Overridden to finalize; always returns identity hash code.
   */
  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  /**
   * Returns the Java code contained in this source file. May return
   * <code>null</code> if this {@link JavaSourceFile} has been invalidated by
   * its containing {@link JavaSourceOracle}. This method may be expensive as
   * the implementor is generally not required to cache the results.
   */
  public abstract String readSource();

  /**
   * Overridden to finalize; always returns {@link #getLocation()}.
   */
  public final String toString() {
    return getLocation();
  }
}
