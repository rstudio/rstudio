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
import com.google.gwt.uibinder.rebind.MortalLogger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Models the ClientBundle to be generated from a ui.xml.
 */
public class ImplicitClientBundle {

  private final Set<ImplicitCssResource> cssMethods = new HashSet<ImplicitCssResource>();
  private final String packageName;
  private final String className;
  private final String fieldName;
  private final String cssBaseName;
  private final MortalLogger logger;

  /**
   * @param packageName Where the bundle should live
   * @param uiBinderImplClassName The name of the generated ui binder
   *          implementation that owns the bundle
   * @param fieldName The bundle's field name
   * @param logger TODO
   */
  public ImplicitClientBundle(String packageName, String uiBinderImplClassName,
      String fieldName, MortalLogger logger) {
    this.packageName = packageName;
    this.className = uiBinderImplClassName + "_GenBundle";
    this.cssBaseName = uiBinderImplClassName + "_GenCss";
    this.fieldName = fieldName;
    this.logger = logger;
  }

  /**
   * Called to declare a new CssResource accessor on this bundle.
   *
   * @param name the method name
   * @param source path to the .css file resource
   * @param extendedInterface the public interface implemented by this
   *          CssResource, or null
   * @param body the inline css text
   * @return
   */
  public ImplicitCssResource createCssResource(String name, String source,
      JClassType extendedInterface, String body) {
    ImplicitCssResource css = new ImplicitCssResource(packageName, cssBaseName
        + name, name, source, extendedInterface, body, logger);
    cssMethods.add(css);
    return css;
  }

  public String getClassName() {
    return className;
  }

  public Set<ImplicitCssResource> getCssMethods() {
    return Collections.unmodifiableSet(cssMethods);
  }

  public String getFieldName() {
    return fieldName;
  }

  public String getPackageName() {
    return packageName;
  }
}
