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

package com.google.gwt.event.shared;

/**
 * Base class for all handler adaptors. Handler adaptors are used for
 * convenience when users want to bundle common handler types together. The most
 * commonly used are
 * {@link com.google.gwt.event.dom.client.HasAllKeyHandlers.Adaptor} and
 * {@link com.google.gwt.event.dom.client.HasAllMouseHandlers.Adaptor}.
 */
public class HandlerAdaptor {

  /**
   * Human readable debugging string.
   * 
   * @return debugging string.
   */
  public String toDebugString() {
    return super.toString();
  }

  /**
   * The toString() for {@link HandlerAdaptor} is overridden to avoid accidently
   * including class literals in the the compiled output. Use
   * {@link HandlerAdaptor} #toDebugString to get more information about the
   * event.
   */
  @Override
  public String toString() {
    return "handler adaptor";
  }

}
