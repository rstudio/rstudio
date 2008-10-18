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
package com.google.gwt.core.ext.typeinfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A container for methods and fields.
 */
class Members extends AbstractMembers {
  private final List<JConstructor> constructors = new ArrayList<JConstructor>();
  private final Map<String, JField> fields = new LinkedHashMap<String, JField>();
  private final Map<String, List<JMethod>> methods = new LinkedHashMap<String, List<JMethod>>();

  public Members(JClassType classType) {
    super(classType);
  }

  @Override
  protected List<JConstructor> doGetConstructors() {
    return constructors;
  }

  @Override
  protected Map<String, JField> doGetFields() {
    return fields;
  }

  @Override
  protected Map<String, List<JMethod>> doGetMethods() {
    return methods;
  }
}