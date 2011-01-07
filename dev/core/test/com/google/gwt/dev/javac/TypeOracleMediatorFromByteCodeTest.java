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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.typeinfo.TypeOracleException;
import com.google.gwt.dev.javac.TypeOracleMediator.TypeData;
import com.google.gwt.dev.resource.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Tests the type oracle when loaded from byte code extracted from the current classpath.
 *
 * This test uses the byte code on the class path created when compiling this
 * GWT instance, so it may fail in different ways depending on your environment.
 * When reporting a bug, make sure to report which JDK was used to compile the
 * tests.
 */
public class TypeOracleMediatorFromByteCodeTest extends
    TypeOracleMediatorTestBase {

  @Override
  protected synchronized void buildTypeOracle() throws TypeOracleException {
    Collection<TypeOracleMediator.TypeData> typeDataList = new ArrayList<TypeOracleMediator.TypeData>();
    for (Resource resource : resources) {
      if (resource instanceof MutableJavaResource) {
        MutableJavaResource javaResource = (MutableJavaResource) resource;
        try {
          for (TypeData result : javaResource.getTypeData()) {
            typeDataList.add(result);
          }
        } catch (IOException e) {
          e.printStackTrace();
          throw new TypeOracleException(e);
        }
      }
    }
    TypeOracleMediator mediator = new TypeOracleMediator();
    mediator.addNewTypes(createTreeLogger(), typeDataList);
    typeOracle = mediator.getTypeOracle();
    checkTypes(typeOracle.getTypes());
  }
}
