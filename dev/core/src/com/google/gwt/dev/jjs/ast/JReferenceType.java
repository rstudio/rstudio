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

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for any reference type.
 */
public abstract class JReferenceType extends JType implements CanBeAbstract {

  public JClassType extnds;
  public List<JField> fields = new ArrayList<JField>();
  public List<JInterfaceType> implments = new ArrayList<JInterfaceType>();
  public List<JMethod> methods = new ArrayList<JMethod>();

  /**
   * Tracks whether this class has a dynamic clinit. Defaults to true until
   * shown otherwise.
   */
  private boolean hasClinit = true;

  public JReferenceType(SourceInfo info, String name) {
    super(info, name, JNullLiteral.INSTANCE);
  }

  /**
   * Returns <code>true</code> if a static field access of
   * <code>targetType</code> from within this type should generate a clinit
   * call. This will be true in cases where <code>targetType</code> has a live
   * clinit method which we cannot statically know has already run. We can
   * statically know the clinit method has already run when:
   * <ol>
   * <li><code>this == targetType</code></li>
   * <li><code>this</code> is a subclass of <code>targetType</code>,
   * because my clinit would have already run this <code>targetType</code>'s
   * clinit; see JLS 12.4</li>
   * </ol>
   */
  public boolean checkClinitTo(JReferenceType targetType) {
    if (this == targetType) {
      // Call to self (very common case).
      return false;
    }
    if (targetType == null || !targetType.hasClinit()) {
      // Target has no clinit (common case).
      return false;
    }

    // See if I'm a subclass.
    JClassType checkType = this.extnds;
    while (checkType != null) {
      if (checkType == targetType) {
        // I am a subclass.
        return false;
      }
      checkType = checkType.extnds;
    }
    return true;
  }

  @Override
  public String getJavahSignatureName() {
    return "L" + name.replaceAll("_", "_1").replace('.', '_') + "_2";
  }

  @Override
  public String getJsniSignatureName() {
    return "L" + name.replace('.', '/') + ';';
  }

  public String getShortName() {
    int dotpos = name.lastIndexOf('.');
    return name.substring(dotpos + 1);
  }

  /**
   * Returns <code>true</code> when this method's clinit must be run
   * dynamically.
   */
  public boolean hasClinit() {
    return hasClinit;
  }

  /**
   * Called when this class's clinit is empty or can be run at the top level.
   */
  void removeClinit() {
    assert hasClinit();
    JMethod clinitMethod = methods.get(0);
    assert JProgram.isClinit(clinitMethod);
    hasClinit = false;
  }
}
