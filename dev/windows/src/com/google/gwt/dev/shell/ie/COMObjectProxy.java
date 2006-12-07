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
package com.google.gwt.dev.shell.ie;

import com.google.gwt.dev.shell.LowLevel;

import org.eclipse.swt.internal.ole.win32.COMObject;

import java.util.Map;

/**
 * A proxy object that allows you to override behavior in an existing COM
 * object. This is used primarily for fixing up the
 * {@link org.eclipse.swt.browser.Browser} object's 'window.external' handling.
 */
class COMObjectProxy extends COMObject {

  private static final int MAX_METHODS_WRAPPED = 23;

  private COMObject target;

  /**
   * Construct a proxy object.
   * 
   * @param argCounts must be the same array of argCounts used to contruct the
   *          wrapped object.
   */
  public COMObjectProxy(int[] argCounts) {
    // Construct myself as a COMObject, even though my vtbl will never
    // actually be used, I need to castable to a COMObject when I injected
    // myself into the ObjectMap.
    super(argCounts);

    // Because I will never be called through my vtbl, I can free my OS
    // memory created in the superclass ctor and remove myself from the
    // ObjectMap. If this didn't work, we'd be leaking memory when
    // the last release is called (unless we assume that method index 2 was
    // Release(), which actually is likely a safe assumption.)
    dispose();

    // Make sure the interface isn't too big.
    if (argCounts != null && argCounts.length >= MAX_METHODS_WRAPPED) {
      throw new IllegalArgumentException("No more than " + MAX_METHODS_WRAPPED
          + " methods can be wrapped right now.");
    }
  }

  /**
   * Interpose this object in front of an existing object.
   */
  public void interpose(COMObject victim) {
    if (this.target != null) {
      throw new IllegalStateException("interpose() can only be called once");
    }

    // Hang onto the object we're wrapping so that we can delegate later.
    this.target = victim;

    // Get the COMObject ObjectMap so that we can hijack the target's slot.
    Map objectMap = (Map) LowLevel.snatchFieldObjectValue(COMObject.class,
        null, "ObjectMap");
    Integer ppVtableTarget = new Integer(target.getAddress());

    // First, make sure that the target is still actually in the map.
    // If it isn't still in there, then the caller is using me incorrectly.
    Object currValue = objectMap.get(ppVtableTarget);
    if (currValue != target) {
      throw new IllegalStateException("target object is not currently mapped");
    }

    // Replace target's entry in COMObject's (vtbl -> instance) map with
    // a reference to this object instead. Calls still come in on the
    // target's vtbl, but COMObject will route them to me instead,
    // so that I can hook/delegate them.
    objectMap.put(ppVtableTarget, this);
  }

  public int method0(int[] args) {
    return target.method0(args);
  }

  public int method1(int[] args) {
    return target.method1(args);
  }

  public int method10(int[] args) {
    return target.method10(args);
  }

  public int method11(int[] args) {
    return target.method11(args);
  }

  public int method12(int[] args) {
    return target.method12(args);
  }

  public int method13(int[] args) {
    return target.method13(args);
  }

  public int method14(int[] args) {
    return target.method14(args);
  }

  public int method15(int[] args) {
    return target.method15(args);
  }

  public int method16(int[] args) {
    return target.method16(args);
  }

  public int method17(int[] args) {
    return target.method17(args);
  }

  public int method18(int[] args) {
    return target.method18(args);
  }

  public int method19(int[] args) {
    return target.method19(args);
  }

  public int method2(int[] args) {
    return target.method2(args);
  }

  public int method20(int[] args) {
    return target.method20(args);
  }

  public int method21(int[] args) {
    return target.method21(args);
  }

  public int method22(int[] args) {
    return target.method22(args);
  }
}