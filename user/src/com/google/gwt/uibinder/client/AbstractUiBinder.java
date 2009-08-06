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
package com.google.gwt.uibinder.client;

/**
 * Convenient base class for implementations of {@link UiBinder}, 
 * generated or otherwise.
 * 
 * @param <U> The type of the UI's root object
 * @param <O> The type of the owner of the UI
 */
public abstract class AbstractUiBinder<U,O> implements UiBinder<U, O> {
  
  /**
   * An interface used by some generated implementations of UiBinder.
   * 
   * @param <U> The type of the UI's root object
   * @param <O> The type of the owner of the UI
   */
  protected interface InstanceBinder<U, O> {
    U makeUi();
    void doBind(O owner);
  }

  public U createAndBindUi(O owner) {
    U ui = createUiRoot(owner);
    bindUi(ui, owner);
    return ui;
  }
}
