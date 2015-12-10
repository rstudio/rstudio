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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.util.StringInterner;

import java.util.List;

/**
 * Base class for any types entity.
 */
public abstract class JType extends JNode implements HasName, CanBeFinal {

  static boolean replaces(List<? extends JType> newTypes, List<? extends JType> oldTypes) {
    if (newTypes.size() != oldTypes.size()) {
      return false;
    }
    for (int i = 0, c = newTypes.size(); i < c; ++i) {
      if (!newTypes.get(i).replaces(oldTypes.get(i))) {
        return false;
      }
    }
    return true;
  }

  protected final String name;

  private String shortName = null;

  private String packageName = null;

  /**
   * Base type for AST type definitions.
   *
   * @param info tracks the source file origin of this type through compilation.
   * @param name binary name of the type.
   */
  public JType(SourceInfo info, String name) {
    super(info);
    this.name = StringInterner.get().intern(name);
  }

  /**
   * Returns <code>true</code> if it's possible for this type to be
   * <code>null</code>.
   *
   * @see JAnalysisDecoratedType
   */
  public abstract boolean canBeNull();

  public abstract boolean isArrayType();

  /**
   * Returns {@code true} if this is {@link JReferenceType.JNullType.INSTANCE}.
   */
  public boolean isNullType() {
    return false;
  }

  public abstract boolean isJsType();

  public abstract boolean isJsFunction();

  public abstract boolean isJsNative();

  public abstract boolean canBeImplementedExternally();

  public abstract boolean canBeReferencedExternally();

  public abstract boolean isJavaLangObject();

  /**
   * Returns {@code true} if this is a JavaScriptObject type.
   */
  public abstract boolean isJsoType();
  /**
   * Returns <code>true</code> if it's possible for this type to be
   * a subclass of the type denoted with this type.
   *
   * @see JAnalysisDecoratedType
   */
  public abstract boolean canBeSubclass();

  public abstract JLiteral getDefaultValue();

  public abstract String getJavahSignatureName();

  public abstract String getJsniSignatureName();

  public String getShortName() {
    if (shortName == null) {
      shortName = StringInterner.get().intern(name.substring(name.lastIndexOf('.') + 1));
    }
    return shortName;
  }

  /**
   * Returns the compound name.
   * <p>
   * The compound name of a class is an array that contains the simple names of all enclosing
   * types followed by the simple name of this type (in outer to inner order).
   * <p>
   * A simple name is the name as it appears in the class declaration (e.g. Name in
   * "class Name { .. }"), i.e. it is a name that does not include enclosing type names nor package.
   */
  public String[] getCompoundName() {
    return new String[] { getShortName() };
  }

  /**
   * Returns a customized description to be used by {@link
   * com.google.gwt.dev.jjs.impl.ToStringGenerationVisitor}.
   */
  public String getDescription() {
    return getName();
  }
  /**
   * If this type is a non-null type, returns the underlying (original) type.
   */
  public JType getUnderlyingType() {
    return this;
  }

  public String getPackageName() {
    if (packageName == null) {
      int dotpos = name.lastIndexOf('.');
      packageName = StringInterner.get().intern(name.substring(0, dotpos < 0 ? 0 : dotpos));
    }
    return packageName;
  }

  /**
   * Returns the (closest) enum supertype if the type is a subclass of an enum; it returns
   * {@code this} if {@code this} is a {@link JEnumType} and {@code null} otherwise.
   */
  public abstract JEnumType isEnumOrSubclass();

  /**
   * Binary name of the type.
   *
   * For example "com.example.Foo$Bar"
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * True if this class is provided externally to the program by the program's
   * host execution environment. For example, while compiling for the JVM, JRE
   * types are external types. External types definitions are provided by class
   * files which are considered opaque by the GWT compiler.
   *
   * TODO(scottb): Means something totally different after AST stiching is done.
   */
  public boolean isExternal() {
    return false;
  }

  /**
   * Checks type replacement from an external type to a resolved canonical type.
   */
  public boolean replaces(JType originalType) {
    if (this == originalType) {
      return true;
    }
    return originalType.isExternal() && originalType.getName().equals(this.getName());
  }
}
