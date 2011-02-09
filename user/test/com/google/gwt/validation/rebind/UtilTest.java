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
package com.google.gwt.validation.rebind;

import static com.google.gwt.validation.rebind.Util.findBestMatches;

import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.base.Functions;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;

import junit.framework.TestCase;

import java.util.List;
import java.util.Set;

/**
 * Tests for {@link Util}.
 */
public class UtilTest extends TestCase {

  private class Alice {
  }

  private class Bob {
  }

  private class Bobby extends Bob {
  }
  private class Bobby2 extends Bobby {
  }

  private interface C1 {
  }

  private interface C2 {
  }

  private class Chuck implements C1, C2 {
  }

  private final static Function<Class<?>, Class<?>> classIdentity = Functions.identity();

  private static void assertContentsInOrder(List<Class<?>> actual,
      Class<?>... classes) {
    assertEquals(ImmutableList.copyOf(classes), ImmutableList.copyOf(actual));
  }

  private static ImmutableList<Class<?>> list(Class<?>... classes) {
    return ImmutableList.copyOf(classes);
  }

  private static ImmutableSet<Class<?>> set(Class<?>... classes) {
    return ImmutableSet.copyOf(classes);
  }

  public void testBestMatches_Bobby2() {
    Set<Class<?>> actual = findBestMatches(Bobby2.class,
        set(Alice.class, Bob.class, Bobby.class));
    assertEquals(1, actual.size());
    assertEquals(Bobby.class, Iterables.get(actual, 0));
  }

  public void testBestMatches_none() {
    Set<Class<?>> actual = Util.findBestMatches(Bob.class, set(Alice.class));
    assertEquals(0, actual.size());
  }

  public void testBestMatches_one() {
    Set<Class<?>> actual = findBestMatches(Bob.class,
        set(Alice.class, Bob.class));
    assertEquals(1, actual.size());
    assertEquals(Bob.class, Iterables.get(actual, 0));
  }

  public void testBestMatches_two() {
    Set<Class<?>> actual = findBestMatches(Chuck.class, set(C1.class, C2.class));
    assertEquals(2, actual.size());
  }

  public void testSortMostSpecificFirst_chuck() {

    List<Class<?>> actual = Util.sortMostSpecificFirst(
        list(C2.class, C1.class, Chuck.class), classIdentity);
    assertContentsInOrder(actual, Chuck.class, C2.class, C1.class);
  }

  public void testSortMostSpecificFirst_double() {
    List<Class<?>> actual = Util.sortMostSpecificFirst(
        list(Alice.class, Alice.class, Bob.class), classIdentity);
    assertContentsInOrder(actual, Alice.class, Bob.class);
  }

  public void testSortMostSpecificFirst_one() {
    List<Class<?>> actual = Util.sortMostSpecificFirst(list(Alice.class),
        classIdentity);
    assertContentsInOrder(actual, Alice.class);
  }
}
