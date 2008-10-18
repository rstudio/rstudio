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
package com.google.gwt.tools.apichecker;

import com.google.gwt.core.ext.typeinfo.JAbstractMethod;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates an API constructor.
 */
final class ApiConstructor extends ApiAbstractMethod {

  ApiConstructor(JAbstractMethod method, ApiClass apiClass) {
    super(method, apiClass);
  }

  @Override
  public boolean isOverridable() {
    return false;
  }

  @Override
  ApiChange checkReturnTypeCompatibility(ApiAbstractMethod newMethod) {
    return null;
  }

  @Override
  List<ApiChange> getAllChangesInApi(ApiAbstractMethod newMethod) {
    return Collections.emptyList();
  }

  /**
   * returns an immutable List.
   */
  @Override
  List<ApiChange.Status> getModifierChanges(ApiAbstractMethod newMethod) {
    return Collections.emptyList();
  }
}