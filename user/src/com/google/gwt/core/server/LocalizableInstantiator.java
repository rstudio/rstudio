/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.core.server;

import com.google.gwt.core.server.ServerGwtBridge.ClassInstantiator;
import com.google.gwt.core.server.ServerGwtBridge.ClassInstantiatorBase;
import com.google.gwt.core.server.ServerGwtBridge.Properties;
import com.google.gwt.i18n.shared.GwtLocale;

/**
 * Instantiator that knows how to lookup locale-specific implementations.
 *
 * It tries pkg.class_locale (including nested classes),
 * pkg.impl.class_locale, and pkg.classImpl_locale, following the inheritance
 * chain for the requested locale.
 */
class LocalizableInstantiator extends ClassInstantiatorBase implements ClassInstantiator {

  @Override
  public <T> T create(Class<?> clazz, Properties properties) {
    String pkgName = clazz.getPackage().getName();
    Class<?> enclosingClass = clazz.getEnclosingClass();
    String className = clazz.getSimpleName();
    GwtLocale locale = ServerGwtBridge.getLocale(properties);
    for (GwtLocale search : locale.getCompleteSearchList()) {
      String suffix = "_" + search.getAsString();
      T obj = this.<T>tryCreate(pkgName + "." + className + suffix);
      if (obj != null) {
        return obj;
      }
      obj = this.<T>tryCreate(pkgName + ".impl." + className + suffix);
      if (obj != null) {
        return obj;
      }
      obj = this.<T>tryCreate(pkgName + "." + className + "Impl" + suffix);
      if (obj != null) {
        return obj;
      }
      if (enclosingClass != null) {
        obj = this.<T>tryCreate(enclosingClass.getCanonicalName() + "$" + className + suffix);
        if (obj != null) {
          return obj;
        }
      }
    }
    return null;
  }
}