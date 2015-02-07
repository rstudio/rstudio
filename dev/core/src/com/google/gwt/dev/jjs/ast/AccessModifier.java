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
package com.google.gwt.dev.jjs.ast;

import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;

/**
 * The Java access modifiers.
 */
public enum AccessModifier {
  /*
   * DO NOT SORT. Will break ordinal-based serialization in JMethod. If this is
   * updated, you must bump the AST serialization version.
   */
  PUBLIC, PROTECTED, DEFAULT, PRIVATE;

  static {
    assert PUBLIC.ordinal() == 0;
    assert PROTECTED.ordinal() == 1;
    assert DEFAULT.ordinal() == 2;
    assert PRIVATE.ordinal() == 3;
  }

  public static AccessModifier fromFieldBinding(FieldBinding fieldBinding) {
    if (fieldBinding.isPublic()) {
      return PUBLIC;
    } else if (fieldBinding.isProtected()) {
      return PROTECTED;
    } else if (fieldBinding.isPrivate()) {
      return PRIVATE;
    }
    assert fieldBinding.isDefault();
    return DEFAULT;
  }

  public static AccessModifier fromMethodBinding(MethodBinding methodBinding) {
    if (methodBinding.isPublic()) {
      return PUBLIC;
    } else if (methodBinding.isProtected()) {
      return PROTECTED;
    } else if (methodBinding.isPrivate()) {
      return PRIVATE;
    }
    assert methodBinding.isDefault();
    return DEFAULT;
  }
}
