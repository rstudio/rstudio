/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;
import com.google.gwt.dev.javac.mediatortest.CircularA;
import com.google.gwt.dev.javac.mediatortest.CircularB;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Set;

/**
 * Tests for LibraryCompilationUnitTypeOracleUpdaterFromSource.<br />
 *
 * Shows the correctness of the lazy LibraryTypeOracle for the common TypeOracle tests as well as
 * the breadth of lazy loads and recalculation of type hierarchy after repeated lazy loads.
 */
public class LibraryCompilationUnitTypeOracleUpdaterFromSourceTest extends
    CompilationUnitTypeOracleUpdaterFromSourceTest {

  protected static final CheckedJavaResource CU_CircularA =
      new CheckedJavaResource(CircularA.class) {
        @Override
        public void check(JClassType type) {
        }
      };

  protected static final CheckedJavaResource CU_CircularB =
      new CheckedJavaResource(CircularB.class) {
        @Override
        public void check(JClassType type) {
        }
      };

  public void testCircularLazyCascade() throws TypeOracleException {
    addTestResource(CU_Object);
    addTestResource(CU_CircularA);
    addTestResource(CU_CircularB);
    buildTypeOracle();

    // TypeOracle is initially empty, because it is lazy.
    assertEquals(0, typeOracle.getTypes().length);

    // But it will lazily load CircularA if asked.
    assertNotNull(typeOracle.findType(CU_CircularA.getTypeName()));

    // The lazy load also cascaded to load CU_CircularB and Object.
    assertEquals(3, typeOracle.getTypes().length);

    // Object sees CircularA and B subclasses.
    Set<JClassType> objectSubtypes =
        Sets.newHashSet(typeOracle.findType(CU_Object.getTypeName()).getSubtypes());
    assertTrue(objectSubtypes.contains(typeOracle.findType(CU_CircularA.getTypeName())));
    assertTrue(objectSubtypes.contains(typeOracle.findType(CU_CircularB.getTypeName())));
  }

  // Check that anonymous classes are not reflected in TypeOracle
  @Override
  public void testLocal() throws TypeOracleException {
    addTestResource(CU_Object);
    addTestResource(CU_EnclosingLocalClass);
    buildTypeOracle();
    // Unlike a regular TypeOracle, the lazy LibraryTypeOracle must be forced to load all types to
    // be able to prove that inner classes do not show up in the type list.
    ((com.google.gwt.dev.javac.typemodel.TypeOracle) typeOracle).ensureAllLoaded();

    assertEquals(2, typeOracle.getTypes().length);
  }

  public void testRepeatedRecalculatesTypeHierarchy() throws TypeOracleException {
    addTestResource(CU_Object);
    addTestResource(CU_String);
    addTestResource(CU_CircularA);
    addTestResource(CU_CircularB);
    buildTypeOracle();

    // Lazily load String type.
    typeOracle.findType(CU_String.getTypeName());

    // Object sees only String subclass
    Set<JClassType> objectSubtypes =
        Sets.newHashSet(typeOracle.findType(CU_Object.getTypeName()).getSubtypes());
    assertTrue(objectSubtypes.contains(typeOracle.findType(CU_String.getTypeName())));

    // Lazily load CircularA and CircularB types.
    typeOracle.findType(CU_CircularA.getTypeName());
    typeOracle.findType(CU_CircularB.getTypeName());

    // Object now additionally sees CircularA and B subclasses.
    objectSubtypes = Sets.newHashSet(typeOracle.findType(CU_Object.getTypeName()).getSubtypes());
    assertTrue(objectSubtypes.contains(typeOracle.findType(CU_String.getTypeName())));
    assertTrue(objectSubtypes.contains(typeOracle.findType(CU_CircularA.getTypeName())));
    assertTrue(objectSubtypes.contains(typeOracle.findType(CU_CircularB.getTypeName())));
  }

  public void testSimpleLazyCascade() throws TypeOracleException {
    addTestResource(CU_Object);
    addTestResource(CU_String);
    buildTypeOracle();

    // TypeOracle is initially empty, because it is lazy.
    assertEquals(0, typeOracle.getTypes().length);

    // But it will lazily load String if asked.
    assertNotNull(typeOracle.findType(CU_String.getTypeName()));

    // The lazy load also cascaded to load Object.
    assertEquals(2, typeOracle.getTypes().length);

    // Object sees String subclass
    Set<JClassType> objectSubtypes =
        Sets.newHashSet(typeOracle.findType(CU_Object.getTypeName()).getSubtypes());
    assertTrue(objectSubtypes.contains(typeOracle.findType(CU_String.getTypeName())));
  }

  @Override
  protected void buildTypeOracle() throws TypeOracleException {
    typeOracle = TypeOracleTestingUtils.buildLibraryTypeOracle(createTreeLogger(), resources);
    checkTypes(typeOracle.getTypes());
  }
}
