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
import java.util.Comparator;
import java.util.HashSet;
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
    CompilationState state = CompilationStateBuilder.buildFrom(logger,
        getJavaResources());
    typeOracle = state.getTypeOracle();

    place = typeOracle.getType("com.google.gwt.app.place.shared.Place");
    assertNotNull(place);
    place1 = typeOracle.getType("com.google.gwt.app.place.shared.testplaces.Place1");
    assertNotNull(place1);
    place2 = typeOracle.getType("com.google.gwt.app.place.shared.testplaces.Place2");
    assertNotNull(place2);
    place3 = typeOracle.getType("com.google.gwt.app.place.shared.testplaces.Place3");
    assertNotNull(place3);
    place4 = typeOracle.getType("com.google.gwt.app.place.shared.testplaces.Place4");
    assertNotNull(place4);
    place5 = typeOracle.getType("com.google.gwt.app.place.shared.testplaces.Place5");
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
    // Array sorted from least derived to most derived. In each pair of adjacent
    // values, neither place extends the other.
    JClassType[] places = {place1, place2, place3, place4, place5};
    for (int i = 0; i < places.length - 1; i++) {
      assertEquals(-1, (int) Math.signum(comparator.compare(places[i],
          places[i + 1])));
      assertEquals(1, (int) Math.signum(comparator.compare(places[i + 1],
          places[i])));
    }
  }
}
