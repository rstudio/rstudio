/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.test.GenericSubclass;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;

import junit.framework.TestCase;

/**
 * Test cases for the {@link TypeOracle}'s and TypeOracleBuilder support for
 * generics.
 */
public class TypeOracleGenericsSupportTest extends TestCase {
  static {
    ModuleDefLoader.setEnableCachingModules(true);
  }

  private final TreeLogger logger = TreeLogger.NULL;
  private ModuleDef moduleDef;

  private final TypeOracle typeOracle;

  public TypeOracleGenericsSupportTest() throws UnableToCompleteException {
    moduleDef = ModuleDefLoader.loadFromClassPath(logger,
        "com.google.gwt.core.ext.typeinfo.TypeOracleTest");
    typeOracle = moduleDef.getTypeOracle(logger);
  }
  
  public void test() throws NotFoundException {
    JClassType type = typeOracle.getType(GenericSubclass.class.getName());
  }
}
