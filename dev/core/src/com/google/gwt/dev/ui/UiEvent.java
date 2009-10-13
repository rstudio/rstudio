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
package com.google.gwt.dev.ui;

/**
 * Base class for any UI event that has a callback.
 *
 * @param <C> callback type
 */
public abstract class UiEvent<C extends UiCallback> {
  
  /**
   * Type token for a UI event.
   * 
   * <p>Any UiEvent subclasses must have exactly one corresponding Type instance
   * created.
   *
   * @param <C> callback type
   */
  public static class Type<C> {

    private final String name;

    protected Type(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}