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
package com.google.gwt.place.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import com.google.gwt.place.testplaces.Place1;
import com.google.gwt.place.testplaces.Place2;
import com.google.gwt.place.testplaces.Place3;
import com.google.gwt.place.testplaces.Place4;
import com.google.gwt.place.testplaces.Place5;

import junit.framework.TestCase;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test case for {@link MostToLeastDerivedPlaceTypeComparator} that uses mock
 * CompilationStates.
 */
public class MostToLeastDerivedPlaceTypeComparatorTest extends TestCase {

  private static TreeLogger createCompileLogger() {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(new PrintWriter(
        System.err, true));
    logger.setMaxDetail(TreeLogger.ERROR);
    return logger;
  }

  private TypeOracle typeOracle;

  private Comparator<JClassType> comparator;

  private JClassType place;
  private JClassType place1;
  private JClassType place2;
  private JClassType place3;
  private JClassType place4;
  private JClassType place5;

  @Override
  protected void setUp() throws Exception {
    comparator = new MostToLeastDerivedPlaceTypeComparator();

    TreeLogger logger = createCompileLogger();
    CompilationState state =
        CompilationStateBuilder.buildFrom(logger, new CompilerContext(), getJavaResources());
    typeOracle = state.getTypeOracle();

    place = typeOracle.getType("com.google.gwt.place.shared.Place");
    assertNotNull(place);
    place1 = typeOracle.getType("com.google.gwt.place.testplaces.Place1");
    assertNotNull(place1);
    place2 = typeOracle.getType("com.google.gwt.place.testplaces.Place2");
    assertNotNull(place2);
    place3 = typeOracle.getType("com.google.gwt.place.testplaces.Place3");
    assertNotNull(place3);
    place4 = typeOracle.getType("com.google.gwt.place.testplaces.Place4");
    assertNotNull(place4);
    place5 = typeOracle.getType("com.google.gwt.place.testplaces.Place5");
    assertNotNull(place5);
  }

  private Set<Resource> getJavaResources() {
    Set<Resource> rtn = new HashSet<Resource>(
        Arrays.asList(JavaResourceBase.getStandardResources()));
    rtn.add(new RealJavaResource(Place.class));
    // referenced by Place1
    rtn.add(new RealJavaResource(PlaceTokenizer.class));
    // referenced by Place1.Tokenizer
    rtn.add(new RealJavaResource(Prefix.class));
    rtn.add(new RealJavaResource(Place1.class));
    rtn.add(new RealJavaResource(Place2.class));
    rtn.add(new RealJavaResource(Place3.class));
    rtn.add(new RealJavaResource(Place4.class));
    rtn.add(new RealJavaResource(Place5.class));
    return rtn;
  }

  public void testEquality() {
    for (JClassType p : new JClassType[] {
        place, place1, place2, place3, place4, place5}) {
      assertEquals(0, comparator.compare(p, p));
    }
  }

  public void testPlaceComparesGreaterThanAnyDerivedClass() {
    for (JClassType p : new JClassType[] {
        place1, place2, place3, place4, place5}) {
      assertEquals(1, (int) Math.signum(comparator.compare(place, p)));
      assertEquals(-1, (int) Math.signum(comparator.compare(p, place)));
    }
  }

  public void testPlaceInheritanceOrder() {
    // Place3 extends Place1
    assertEquals(1, (int) Math.signum(comparator.compare(place1, place3)));
    assertEquals(-1, (int) Math.signum(comparator.compare(place3, place1)));

    // Place5 extends Place3 extends Place1
    assertEquals(1, (int) Math.signum(comparator.compare(place1, place5)));
    assertEquals(-1, (int) Math.signum(comparator.compare(place5, place1)));

    // Place4 extends Place1
    assertEquals(1, (int) Math.signum(comparator.compare(place1, place4)));
    assertEquals(-1, (int) Math.signum(comparator.compare(place4, place1)));

    // Place5 extends Place3
    assertEquals(1, (int) Math.signum(comparator.compare(place3, place5)));
    assertEquals(-1, (int) Math.signum(comparator.compare(place5, place3)));
  }

  public void testFallbackToClassName() {
    JClassType[][] places = {
        {place3, place4}, // place3 and place4 both extend directly from place1
        {place1, place2}, // place1 and place2 both extend directly from place
    };
    for (JClassType[] pair : places) {
      assertEquals(-1, (int) Math.signum(comparator.compare(pair[0], pair[1])));
      assertEquals(1, (int) Math.signum(comparator.compare(pair[1], pair[0])));
    }
  }

  public void testCollectionSort() {
    List<JClassType> actual = Arrays.asList(place, place1, place3, place5);
    Collections.sort(actual, comparator);
    assertEquals(Arrays.asList(place5, place3, place1, place), actual);

    actual = Arrays.asList(place5, place3, place1, place);
    Collections.sort(actual, comparator);
    assertEquals(Arrays.asList(place5, place3, place1, place), actual);

    actual = Arrays.asList(place, place1, place2, place3, place4, place5);
    Collections.sort(actual, comparator);
    assertEquals(Arrays.asList(place5, place3, place4, place1, place2, place), actual);

    actual = Arrays.asList(place5, place4, place3, place2, place1, place);
    Collections.sort(actual, comparator);
    assertEquals(Arrays.asList(place5, place3, place4, place1, place2, place), actual);

    // This is equivalent to the test-case from issue 8036
    // https://code.google.com/p/google-web-toolkit/issues/detail?id=8036
    actual = Arrays.asList(place2, place1, place3);
    Collections.sort(actual, comparator);
    assertEquals(Arrays.asList(place3, place1, place2), actual);
  }
}
