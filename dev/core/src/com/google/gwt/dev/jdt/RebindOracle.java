/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.jdt;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

/**
 * Abstracts the implementation of making deferred binding decisions.
 */
public interface RebindOracle {

  /**
   * Determines which type should be substituted for the requested type. The
   * caller must ensure that the result type is instantiable.
   * 
   * @return the substitute type name, which may be the requested type itself;
   *         this method must not return <code>null</code> if sourceTypeName is
   *         not <code>null</code>
   */
  String rebind(TreeLogger logger, String sourceTypeName) throws UnableToCompleteException;
}
