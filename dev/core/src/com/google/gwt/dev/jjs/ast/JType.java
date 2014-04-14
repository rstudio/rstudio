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

  public abstract String getClassLiteralFactoryMethod();

  public abstract JLiteral getDefaultValue();

  public abstract String getJavahSignatureName();

  public abstract String getJsniSignatureName();

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
  boolean replaces(JType originalType) {
    if (this == originalType) {
      return true;
    }
    return originalType.isExternal() && originalType.getName().equals(this.getName());
  }

}
