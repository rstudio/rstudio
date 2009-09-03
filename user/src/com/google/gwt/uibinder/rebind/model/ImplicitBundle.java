/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.uibinder.rebind.model;

import com.google.gwt.core.ext.typeinfo.JClassType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Models the ClientBundle to be generated from a ui.xml.
 */
public class ImplicitBundle {

  private final Set<CssResourceGetter> cssMethods = new HashSet<CssResourceGetter>();
  private final String packageName;
  private final String className;
  private final String fieldName;

  /**
   * @param packageName Where the bundle should live
   * @param uiBinderImplClassName The name of the generated ui binder
   *          implementation that owns the bundle
   * @param fieldName The bundle's field name
   */
  public ImplicitBundle(String packageName, String uiBinderImplClassName,
      String fieldName) {
    this.packageName = packageName;
    this.className = uiBinderImplClassName + "GenBundle";
    this.fieldName = fieldName;
  }

  /**
   * Called to declare a new CssResource accessor on this bundle.
   *
   * @param name the method name
   * @param source path to the .css file resource
   * @param extendedInterface the public interface implemented by this
   *          CssResource, or null
   * @return
   */
  public CssResourceGetter createCssResource(String name, String source,
      JClassType extendedInterface) {
    CssResourceGetter css = new CssResourceGetter(name, source, extendedInterface);
    cssMethods.add(css);
    return css;
  }

  public String getClassName() {
    return className;
  }

  public Set<CssResourceGetter> getCssMethods() {
    return Collections.unmodifiableSet(cssMethods);
  }

  public String getFieldName() {
    return fieldName;
  }

  public String getPackageName() {
    return packageName;
  }
}
