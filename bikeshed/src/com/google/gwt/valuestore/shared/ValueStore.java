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
package com.google.gwt.valuestore.shared;

/**
 * A store of records with properties. Each record has a {@link ValuesKey}
 * associated with it to indicate its type.
 */
public interface ValueStore {
  /**
   * Most validations are per field or per id and set via annotation. Note that
   * validations are only actually enforced by in {@link DeltaValueStore}
   * instances spawned by {@link #forEditContext(Object)}
   */
  void addValidation(/* what's a validation. JSR 303? Learn from Pectin? */);
  
  DeltaValueStore spawnDeltaView();
}
