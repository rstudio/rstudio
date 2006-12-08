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
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;

/**
 * Implements a {@link SourceOracle} in terms of a {@link TypeOracle}.
 */
public class SourceOracleOnTypeOracle implements SourceOracle {

  private final TypeOracle typeOracle;

  public SourceOracleOnTypeOracle(TypeOracle typeOracle) {
    this.typeOracle = typeOracle;
  }

  public CompilationUnitProvider findCompilationUnit(TreeLogger logger,
      String sourceTypeName) {
    JClassType type = typeOracle.findType(sourceTypeName);
    if (type != null) {
      return type.getCompilationUnit();
    }
    return null;
  }

  public boolean isPackage(String possiblePackageName) {
    if (typeOracle.findPackage(possiblePackageName) != null) {
      return true;
    } else {
      return false;
    }
  }
}
