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
package com.google.gwt.dev.javac;

import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.base.Strings;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;

/**
 * Utility functions to interact with JDT classes.
 */
public final class JdtUtil {
  /**
   * Returns a source name from an array of names.
   */
  public static String asDottedString(char[][] name) {
    StringBuilder result = new StringBuilder();
    if (name.length > 0) {
      result.append(name[0]);
    }

    for (int i = 1; i < name.length; ++i) {
      result.append('.');
      result.append(name[i]);
    }
    return result.toString();
  }

  public static String getSourceName(ReferenceBinding classBinding) {
    return Joiner.on(".").skipNulls().join(new String[] {
        Strings.emptyToNull(CharOperation.charToString(classBinding.qualifiedPackageName())),
        CharOperation.charToString(classBinding.qualifiedSourceName())});
  }

  private JdtUtil() {
  }
}
