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
package com.google.gwt.dev.jdt;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JType;

import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods and constants.
 */
class Shared {
  static final int MOD_ABSTRACT = 0x00000001;
  static final int MOD_FINAL = 0x00000002;
  static final int MOD_NATIVE = 0x00000004;
  static final int MOD_PRIVATE = 0x00000008;
  static final int MOD_PROTECTED = 0x00000010;
  static final int MOD_PUBLIC = 0x00000020;
  static final int MOD_STATIC = 0x00000040;
  static final int MOD_TRANSIENT = 0x00000080;
  static final int MOD_VOLATILE = 0x00000100;
  static final JClassType[] NO_JCLASSES = new JClassType[0];
  static final JConstructor[] NO_JCTORS = new JConstructor[0];
  static final JField[] NO_JFIELDS = new JField[0];
  static final JMethod[] NO_JMETHODS = new JMethod[0];
  static final JPackage[] NO_JPACKAGES = new JPackage[0];
  static final JParameter[] NO_JPARAMS = new JParameter[0];
  static final JType[] NO_JTYPES = new JType[0];
  static final String[][] NO_STRING_ARR_ARR = new String[0][];
  static final String[] NO_STRINGS = new String[0];

  public static int bindingToModifierBits(FieldBinding binding) {
    int bits = 0;
    bits |= (binding.isPublic() ? MOD_PUBLIC : 0);
    bits |= (binding.isPrivate() ? MOD_PRIVATE : 0);
    bits |= (binding.isProtected() ? MOD_PROTECTED : 0);
    bits |= (binding.isStatic() ? MOD_STATIC : 0);
    bits |= (binding.isTransient() ? MOD_TRANSIENT : 0);
    bits |= (binding.isFinal() ? MOD_FINAL : 0);
    bits |= (binding.isVolatile() ? MOD_VOLATILE : 0);
    return bits;
  }

  public static int bindingToModifierBits(MethodBinding binding) {
    int bits = 0;
    bits |= (binding.isPublic() ? MOD_PUBLIC : 0);
    bits |= (binding.isPrivate() ? MOD_PRIVATE : 0);
    bits |= (binding.isProtected() ? MOD_PROTECTED : 0);
    bits |= (binding.isStatic() ? MOD_STATIC : 0);
    bits |= (binding.isFinal() ? MOD_FINAL : 0);
    bits |= (binding.isNative() ? MOD_NATIVE : 0);
    bits |= (binding.isAbstract() ? MOD_ABSTRACT : 0);
    return bits;
  }

  public static int bindingToModifierBits(ReferenceBinding binding) {
    int bits = 0;
    bits |= (binding.isPublic() ? MOD_PUBLIC : 0);
    bits |= (binding.isPrivate() ? MOD_PRIVATE : 0);
    bits |= (binding.isProtected() ? MOD_PROTECTED : 0);
    bits |= (binding.isStatic() ? MOD_STATIC : 0);
    bits |= (binding.isFinal() ? MOD_FINAL : 0);
    bits |= (binding.isAbstract() ? MOD_ABSTRACT : 0);
    return bits;
  }

  static String[] modifierBitsToNames(int bits) {
    List<String> strings = new ArrayList<String>();

    // The order is based on the order in which we want them to appear.
    //
    if (0 != (bits & MOD_PUBLIC)) {
      strings.add("public");
    }

    if (0 != (bits & MOD_PRIVATE)) {
      strings.add("private");
    }

    if (0 != (bits & MOD_PROTECTED)) {
      strings.add("protected");
    }

    if (0 != (bits & MOD_STATIC)) {
      strings.add("static");
    }

    if (0 != (bits & MOD_ABSTRACT)) {
      strings.add("abstract");
    }

    if (0 != (bits & MOD_FINAL)) {
      strings.add("final");
    }

    if (0 != (bits & MOD_NATIVE)) {
      strings.add("native");
    }

    if (0 != (bits & MOD_TRANSIENT)) {
      strings.add("transient");
    }

    if (0 != (bits & MOD_VOLATILE)) {
      strings.add("volatile");
    }

    return strings.toArray(NO_STRINGS);
  }
}
