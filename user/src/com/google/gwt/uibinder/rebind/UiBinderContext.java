/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.uibinder.rebind;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.uibinder.rebind.model.OwnerFieldClass;

import java.util.HashMap;
import java.util.Map;

/**
 * A shared context cache for UiBinder.
 */
public class UiBinderContext {

  private final Map<JClassType, OwnerFieldClass> fieldClassesCache =
      new HashMap<JClassType, OwnerFieldClass>();

  public OwnerFieldClass getOwnerFieldClass(JClassType type) {
    return fieldClassesCache.get(type);
  }

  public void putOwnerFieldClass(JClassType forType, OwnerFieldClass clazz) {
    fieldClassesCache.put(forType, clazz);    
  }
}
