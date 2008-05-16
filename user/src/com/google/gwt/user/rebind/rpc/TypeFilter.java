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

package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.typeinfo.JClassType;

/**
 * Used to filter types out of serialization.
 */
interface TypeFilter {
  /**
   * Returns the name of this filter.
   * 
   * 
   * @return the name of this filter
   */
  String getName();

  /**
   * Returns <code>true</code> if the type should be included.
   * 
   * @param type
   * @return <code>true</code> if the type should be included
   */
  boolean isAllowed(JClassType type);
}
