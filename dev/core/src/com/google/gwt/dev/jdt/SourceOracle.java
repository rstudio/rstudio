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
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;

public interface SourceOracle {

  /**
   * Attempts to find a compilation unit for the specified source type name.
   * 
   * @return <code>null</code> if a compilation unit for the specified type
   *         was not found or an error prevented the compilation unit from being
   *         provided
   */
  CompilationUnitProvider findCompilationUnit(TreeLogger logger,
      String sourceTypeName) throws UnableToCompleteException;

  /**
   * Determines whether or not a string is the name of a package. Remember that
   * every part of a package name is also a package. For example, the fact that
   * <code>java.lang</code> is a package implies that <code>java</code> is
   * also a package.
   */
  boolean isPackage(String possiblePackageName);
}
