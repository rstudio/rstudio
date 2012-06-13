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
package elemental.js.util;

import static elemental.js.util.TestUtils.assertSamelitude;

import com.google.gwt.junit.client.GWTTestCase;

import elemental.util.ArrayOf;
import elemental.util.ArrayOfBoolean;
import elemental.util.ArrayOfInt;
import elemental.util.ArrayOfNumber;
import elemental.util.ArrayOfString;
import elemental.util.CanCompare;
import elemental.util.CanCompareInt;
import elemental.util.CanCompareNumber;
import elemental.util.CanCompareString;
import elemental.util.Collections;

/**
 * Tests for {@link ArrayOf}, {@link ArrayOfBoolean}, {@link ArrayOfInt},
 * {@link ArrayOfString} and {@link ArrayOfNumber}.
 *
 */
public class ArrayTests extends GWTTestCase {
  private static ArrayOfBoolean arrayFrom(boolean... items) {
    final ArrayOfBoolean array = Collections.arrayOfBoolean();
    for (int i = 0, n = items.length; i < n; ++i) {
      array.push(items[i]);
    }
    return array;
  }

  private static ArrayOfNumber arrayFrom(double... items) {
    final ArrayOfNumber array = Collections.arrayOfNumber();
    for (int i = 0, n = items.length; i < n; ++i) {
      array.push(items[i]);
    }
    return array;
  }

  private static ArrayOfInt arrayFrom(int... items) {
    final ArrayOfInt array = Collections.arrayOfInt();
    for (int i = 0, n = items.length; i < n; ++i) {
      array.push(items[i]);
    }
    return array;
  }

  private static ArrayOfString arrayFrom(String... items) {
    final ArrayOfString array = Collections.arrayOfString();
    for (int i = 0, n = items.length; i < n; ++i) {
      array.push(items[i]);
    }
    return array;
  }

  @SuppressWarnings("unchecked")
  private static <T> ArrayOf<T> arrayFrom(T... items) {
    final ArrayOf<T> array = (ArrayOf<T>) Collections.arrayOf(Object.class);
    for (int i = 0, n = items.length; i < n; ++i) {
      array.push(items[i]);
    }
    return array;
  }

  @Override
  public String getModuleName() {
    return "elemental.Elemental";
  }

  /**
   * Tests {@link ArrayOf}.
   */
  public void testArrays() {
    // This is our test subject.
    final ArrayOf<TestItem> array = Collections.arrayOf(TestItem.class);

    // These are items to put in him.
    final TestItem[] items = new TestItem[] {new TestItem(0), new TestItem(1), new TestItem(2)};

    // Let's put the items in him.
    for (int i = 0, n = items.length; i < n; ++i) {
      array.push(items[i]);
    }

    // Are the items in the right places?
    assertEquals(items.length, array.length());
    for (int i = 0, n = items.length; i < n; ++i) {
      assertEquals(items[i], array.get(i));
    }

    // These are some more items to put in him.
    final TestItem[] newItems = new TestItem[] {new TestItem(3), new TestItem(4), new TestItem(5)};

    // Put all these items in where the others were.
    for (int i = 0, n = items.length; i < n; ++i) {
      array.set(i, newItems[i]);
      assertEquals(newItems[i], array.get(i));
    }

    // Shift our test subject to squeeze out the first item.
    assertEquals(newItems[0], array.shift());
    assertEquals(newItems.length - 1, array.length());

    // Then unshift.
    array.unshift(newItems[0]);
    assertEquals(newItems[0], array.get(0));
    assertEquals(newItems.length, array.length());

    // Now join them together in harmony.
    assertEquals("item3$item4$item5", array.join("$"));

    for (int i = 0, n = items.length; i < n; ++i) {
      assertEquals(i, array.indexOf(newItems[i]));
      assertTrue(array.contains(newItems[i]));
    }

    final TestItem imposter = new TestItem(100);
    assertEquals(-1, array.indexOf(imposter));
    assertFalse(array.contains(imposter));

    final TestItem[] itemsA = new TestItem[] {new TestItem(12), new TestItem(13)};
    final TestItem[] itemsB = new TestItem[] {new TestItem(14), new TestItem(15)};
    final ArrayOf<TestItem> a = arrayFrom(itemsA);
    final ArrayOf<TestItem> b = arrayFrom(itemsB);
    assertSamelitude(new TestItem[] {itemsA[0], itemsA[1], itemsB[0], itemsB[1]}, a.concat(b));
    assertSamelitude(itemsA, a);
    assertSamelitude(itemsB, b);
  }

  /**
   * Tests {@link ArrayOfBoolean}.
   */
  public void testArraysOfBooleans() {
    // This is our test subject.
    final ArrayOfBoolean array = Collections.arrayOfBoolean();

    // These are items to put in him.
    final boolean[] items = new boolean[] {true, false, true};

    // Let's put the items in him.
    for (int i = 0, n = items.length; i < n; ++i) {
      array.push(items[i]);
    }

    // Are the items in the right places?
    assertEquals(items.length, array.length());
    for (int i = 0, n = items.length; i < n; ++i) {
      assertEquals(items[i], array.get(i));
    }

    // These are some more items to put in him.
    final boolean[] newItems = new boolean[] {false, true, false};

    // Put all these items in where the others were.
    for (int i = 0, n = items.length; i < n; ++i) {
      array.set(i, newItems[i]);
      assertEquals(newItems[i], array.get(i));
    }

    // Shift our test subject to squeeze out the first item.
    assertEquals(newItems[0], array.shift());
    assertEquals(newItems.length - 1, array.length());

    // Then Unshift.
    array.unshift(newItems[0]);
    assertEquals(newItems[0], array.get(0));
    assertEquals(newItems.length, array.length());

    // Now join them together in harmony.
    assertEquals("false$true$false", array.join("$"));

    assertEquals(0, array.indexOf(false));
    assertTrue(array.contains(false));
    assertEquals(1, array.indexOf(true));
    assertTrue(array.contains(true));

    final ArrayOfBoolean allTrue = Collections.arrayOfBoolean();
    allTrue.push(true);
    allTrue.push(true);
    allTrue.push(true);
    assertEquals(-1, allTrue.indexOf(false));
    assertFalse(allTrue.contains(false));

    final boolean[] itemsA = new boolean[] {true, false};
    final boolean[] itemsB = new boolean[] {false, true};
    final ArrayOfBoolean a = arrayFrom(itemsA);
    final ArrayOfBoolean b = arrayFrom(itemsB);
    assertSamelitude(new boolean[] {itemsA[0], itemsA[1], itemsB[0], itemsB[1]}, a.concat(b));
    assertSamelitude(itemsA, a);
    assertSamelitude(itemsB, b);
  }

  /**
   * Tests {@link ArrayOfInt}.
   */
  public void testArraysOfInts() {
    // This is our test subject.
    final ArrayOfInt array = Collections.arrayOfInt();

    // These are items to put in him.
    final int[] items = new int[] {0, 1, 2};

    // Let's put the items in him.
    for (int i = 0, n = items.length; i < n; ++i) {
      array.push(items[i]);
    }

    // Are the items in the right places?
    assertEquals(items.length, array.length());
    for (int i = 0, n = items.length; i < n; ++i) {
      assertEquals(items[i], array.get(i));
    }

    // These are some more items to put in him.
    final int[] newItems = new int[] {3, 4, 5};

    // Put all these items in where the others were.
    for (int i = 0, n = items.length; i < n; ++i) {
      array.set(i, newItems[i]);
      assertEquals(newItems[i], array.get(i));
    }

    // Shift our test subject to squeeze out the first item.
    assertEquals(newItems[0], array.shift());
    assertEquals(newItems.length - 1, array.length());

    // Then Unshift.
    array.unshift(newItems[0]);
    assertEquals(newItems[0], array.get(0));
    assertEquals(newItems.length, array.length());

    // Now join them together in harmony.
    assertEquals("3$4$5", array.join("$"));

    for (int i = 0, n = items.length; i < n; ++i) {
      assertEquals(i, array.indexOf(newItems[i]));
      assertTrue(array.contains(newItems[i]));
    }

    final int imposter = 100;
    assertEquals(-1, array.indexOf(imposter));
    assertFalse(array.contains(imposter));

    final int[] itemsA = new int[] {11, 12};
    final int[] itemsB = new int[] {13, 14};
    final ArrayOfInt a = arrayFrom(itemsA);
    final ArrayOfInt b = arrayFrom(itemsB);
    assertSamelitude(new int[] {itemsA[0], itemsA[1], itemsB[0], itemsB[1]}, a.concat(b));
    assertSamelitude(itemsA, a);
    assertSamelitude(itemsB, b);
  }

  /**
   * Tests {@link ArrayOfNumber}.
   */
  public void testArraysOfNumbers() {
    // This is our test subject.
    final ArrayOfNumber array = Collections.arrayOfNumber();

    // These are items to put in him.
    final double[] items = new double[] {0.0, 1.0, 2.0};

    // Let's put the items in him.
    for (int i = 0, n = items.length; i < n; ++i) {
      array.push(items[i]);
    }

    // Are the items in the right places?
    assertEquals(items.length, array.length());
    for (int i = 0, n = items.length; i < n; ++i) {
      assertEquals(items[i], array.get(i));
    }

    // These are some more items to put in him.
    final double[] newItems = new double[] {3.0, 4.0, 5.0};

    // Put all these items in where the others were.
    for (int i = 0, n = items.length; i < n; ++i) {
      array.set(i, newItems[i]);
      assertEquals(newItems[i], array.get(i));
    }

    // Shift our test subject to squeeze out the first item.
    assertEquals(newItems[0], array.shift());
    assertEquals(newItems.length - 1, array.length());

    // Then Unshift.
    array.unshift(newItems[0]);
    assertEquals(newItems[0], array.get(0));
    assertEquals(newItems.length, array.length());

    // Now join them together in harmony.
    assertEquals("3$4$5", array.join("$"));

    final double[] itemsA = new double[] {0.01, 0.02};
    final double[] itemsB = new double[] {0.03, 0.04};
    final ArrayOfNumber a = arrayFrom(itemsA);
    final ArrayOfNumber b = arrayFrom(itemsB);
    assertSamelitude(new double[] {itemsA[0], itemsA[1], itemsB[0], itemsB[1]}, a.concat(b));
    assertSamelitude(itemsA, a);
    assertSamelitude(itemsB, b);
  }

  /**
   * Tests for {@link ArrayOfString}.
   */
  public void testArraysOfStrings() {
    // This is our test subject.
    final ArrayOfString array = Collections.arrayOfString();

    // These are items to put in him.
    final String[] items = new String[] {"zero goats", "one goat", "two goats"};

    // Let's put the items in him.
    for (int i = 0, n = items.length; i < n; ++i) {
      array.push(items[i]);
    }

    // Are the items in the right places?
    assertEquals(items.length, array.length());
    for (int i = 0, n = items.length; i < n; ++i) {
      assertEquals(items[i], array.get(i));
    }

    // These are some more items to put in him.
    final String[] newItems = new String[] {"three goats", "four goats", "SQUIRREL!"};

    // Put all these items in where the others were.
    for (int i = 0, n = items.length; i < n; ++i) {
      array.set(i, newItems[i]);
      assertEquals(newItems[i], array.get(i));
    }

    // Shift our test subject to squeeze out the first item.
    assertEquals(newItems[0], array.shift());
    assertEquals(newItems.length - 1, array.length());

    // Then Unshift.
    array.unshift(newItems[0]);
    assertEquals(newItems[0], array.get(0));
    assertEquals(newItems.length, array.length());

    // Now join them together in harmony.
    assertEquals("three goats$four goats$SQUIRREL!", array.join("$"));

    for (int i = 0, n = items.length; i < n; ++i) {
      assertEquals(i, array.indexOf(newItems[i]));
      assertTrue(array.contains(newItems[i]));
    }

    final String imposter = "Pajamas?";
    assertEquals(-1, array.indexOf(imposter));
    assertFalse(array.contains(imposter));

    final String[] itemsA = new String[] {"atlanta", "eagle"};
    final String[] itemsB = new String[] {"chaps", "profit"};
    final ArrayOfString a = arrayFrom(itemsA);
    final ArrayOfString b = arrayFrom(itemsB);
    assertSamelitude(new String[] {itemsA[0], itemsA[1], itemsB[0], itemsB[1]}, a.concat(b));
    assertSamelitude(itemsA, a);
    assertSamelitude(itemsB, b);
  }

  /**
   * Tests {@link ArrayOf#insert(int, Object)}.
   */
  public void testInsertingIntoArrays() {
    final ArrayOf<TestItem> array = Collections.arrayOf(TestItem.class);

    final TestItem a = new TestItem(0);
    array.insert(0, a);
    assertSamelitude(new TestItem[] {a}, array);

    final TestItem b = new TestItem(1);
    array.insert(0, b);
    assertSamelitude(new TestItem[] {b, a}, array);

    final TestItem c = new TestItem(2);
    array.insert(100, c);
    assertSamelitude(new TestItem[] {b, a, c}, array);

    final TestItem d = new TestItem(3);
    array.insert(-1, d);
    assertSamelitude(new TestItem[] {b, a, d, c}, array);
  }

  /**
   * Tests {@link ArrayOfBoolean#insert(int, boolean)}.
   */
  public void testInsertingIntoArraysOfBooleans() {
    final ArrayOfBoolean array = Collections.arrayOfBoolean();

    array.insert(0, true);
    assertSamelitude(new boolean[] {true}, array);

    array.insert(0, false);
    assertSamelitude(new boolean[] {false, true}, array);

    array.insert(100, false);
    assertSamelitude(new boolean[] {false, true, false}, array);

    array.insert(-1, true);
    assertSamelitude(new boolean[] {false, true, true, false}, array);
  }

  /**
   * Tests {@link ArrayOfInt#insert(int, int)}.
   */
  public void testInsertingIntoArraysOfInts() {
    final ArrayOfInt array = Collections.arrayOfInt();

    array.insert(0, 0);
    assertSamelitude(new int[] {0}, array);

    array.insert(0, 1);
    assertSamelitude(new int[] {1, 0}, array);

    array.insert(100, 2);
    assertSamelitude(new int[] {1, 0, 2}, array);

    array.insert(-1, 3);
    assertSamelitude(new int[] {1, 0, 3, 2}, array);
  }

  /**
   * Tests {@link ArrayOfNumber#insert(int, double)}.
   */
  public void testInsertingIntoArraysOfNumbers() {
    final ArrayOfNumber array = Collections.arrayOfNumber();

    array.insert(0, 0.1);
    assertSamelitude(new double[] {0.1}, array);

    array.insert(0, 0.2);
    assertSamelitude(new double[] {0.2, 0.1}, array);

    array.insert(100, 0.3);
    assertSamelitude(new double[] {0.2, 0.1, 0.3}, array);

    array.insert(-1, 0.4);
    assertSamelitude(new double[] {0.2, 0.1, 0.4, 0.3}, array);
  }

  /**
   * Tests {@link ArrayOfString#insert(int, String)}.
   */
  public void testInsertingIntoArraysOfStrings() {
    final ArrayOfString array = Collections.arrayOfString();

    array.insert(0, "beer");
    assertSamelitude(new String[] {"beer"}, array);

    array.insert(0, "cigars");
    assertSamelitude(new String[] {"cigars", "beer"}, array);

    array.insert(100, "porn");
    assertSamelitude(new String[] {"cigars", "beer", "porn"}, array);

    array.insert(-1, "profit");
    assertSamelitude(new String[] {"cigars", "beer", "profit", "porn"}, array);
  }

  /**
   * Tests {@link ArrayOf#sort(CanCompare)}.
   */
  public void testSortingOfArrays() {
    final TestItem[] items =
        new TestItem[] {new TestItem(0), new TestItem(1), new TestItem(2), new TestItem(3)};

    final ArrayOf<TestItem> array = Collections.arrayOf(TestItem.class);
    array.push(items[2]);
    array.push(items[1]);
    array.push(items[3]);
    array.push(items[0]);

    array.sort(new CanCompare<TestItem>() {
      @Override
      public int compare(TestItem a, TestItem b) {
        return a.id() - b.id();
      }
    });
    assertEquals(items.length, array.length());
    for (int i = 0, n = array.length(); i < n; ++i) {
      assertEquals(items[i], array.get(i));
    }
  }

  /**
   * Tests {@link ArrayOfInt#sort()} and
   * {@link ArrayOfInt#sort(CanCompareInt)}.
   */
  public void testSortingOfArraysOfInts() {
    final int[] items = new int[] {0, 1, 2, 3};
    final ArrayOfInt array = Collections.arrayOfInt();
    array.push(items[2]);
    array.push(items[1]);
    array.push(items[0]);
    array.push(items[3]);

    array.sort();
    assertEquals(items.length, array.length());
    for (int i = 0, n = array.length(); i < n; ++i) {
      assertEquals(items[i], array.get(i));
    }

    array.sort(new CanCompareInt() {
      @Override
      public int compare(int a, int b) {
        return b - a;
      }
    });
    assertEquals(items.length, array.length());
    for (int i = 0, n = array.length(); i < n; ++i) {
      assertEquals(items[n - 1 - i], array.get(i));
    }
  }

  /**
   * Tests {@link ArrayOfNumber#sort()} and
   * {@link ArrayOfNumber#sort(CanCompareNumber)}.
   */
  public void testSortingOfArraysOfNumbers() {
    final double[] items = new double[] {0.0, 0.1, 0.2, 0.3};
    final ArrayOfNumber array = Collections.arrayOfNumber();
    array.push(items[2]);
    array.push(items[1]);
    array.push(items[3]);
    array.push(items[0]);

    array.sort();
    assertEquals(items.length, array.length());
    for (int i = 0, n = array.length(); i < n; ++i) {
      assertEquals(items[i], array.get(i), 0.01);
    }

    array.sort(new CanCompareNumber() {
      @Override
      public int compare(double a, double b) {
        return (a > b) ? -1 : (a < b) ? 1 : 0;
      }
    });
    assertEquals(items.length, array.length());
    for (int i = 0, n = array.length(); i < n; ++i) {
      assertEquals(items[n - 1 - i], array.get(i), 0.01);
    }
  }

  /**
   * Tests {@link ArrayOfString#sort()} and
   * {@link ArrayOfString#sort(CanCompareString)}.
   */
  public void testSortingOfArraysOfStrings() {
    final String[] items = new String[] {"aaa", "aab", "baa", "bab"};
    final ArrayOfString array = Collections.arrayOfString();
    array.push(items[2]);
    array.push(items[1]);
    array.push(items[3]);
    array.push(items[0]);

    array.sort();
    assertEquals(items.length, array.length());
    for (int i = 0, n = array.length(); i < n; ++i) {
      assertEquals(items[i], array.get(i));
    }

    array.sort(new CanCompareString() {
      @Override
      public native int compare(String a, String b) /*-{
    return (a > b) ? -1 : (a < b) ? 1 : 0;
  }-*/;
    });
    assertEquals(items.length, array.length());
    for (int i = 0, n = array.length(); i < n; ++i) {
      assertEquals(items[n - 1 - i], array.get(i));
    }
  }

  /**
   * Tests {@link ArrayOf#splice(int, int)}.
   */
  public void testSplicingOfArrays() {
    final TestItem[] items = new TestItem[] {
        new TestItem(0), new TestItem(1), new TestItem(2), new TestItem(3), new TestItem(4)};

    final ArrayOf<TestItem> a = arrayFrom(items);
    assertSamelitude(items, a.splice(0, items.length));
    assertSamelitude(new TestItem[] {}, a);

    final ArrayOf<TestItem> b = arrayFrom(items);
    assertSamelitude(new TestItem[] {}, b.splice(0, 0));
    assertSamelitude(items, b);

    final ArrayOf<TestItem> c = arrayFrom(items);
    assertSamelitude(new TestItem[] {items[0], items[1]}, c.splice(0, 2));
    assertSamelitude(new TestItem[] {items[2], items[3], items[4]}, c);
  }

  /**
   * Tests {@link ArrayOfBoolean#splice(int, int)}.
   */
  public void testSplicingOfArraysOfBooleans() {
    final boolean[] items = new boolean[] {true, false, true, false, true};

    final ArrayOfBoolean a = arrayFrom(items);
    assertSamelitude(items, a.splice(0, items.length));
    assertSamelitude(new boolean[] {}, a);

    final ArrayOfBoolean b = arrayFrom(items);
    assertSamelitude(new boolean[] {}, b.splice(0, 0));
    assertSamelitude(items, b);

    final ArrayOfBoolean c = arrayFrom(items);
    assertSamelitude(new boolean[] {items[0], items[1]}, c.splice(0, 2));
    assertSamelitude(new boolean[] {items[2], items[3], items[4]}, c);
  }

  /**
   * Tests {@link ArrayOfInt#splice(int, int)}.
   */
  public void testSplicingOfArraysOfInts() {
    final int[] items = new int[] {0, 1, 2, 3, 4};

    final ArrayOfInt a = arrayFrom(items);
    assertSamelitude(items, a.splice(0, items.length));
    assertSamelitude(new int[] {}, a);

    final ArrayOfInt b = arrayFrom(items);
    assertSamelitude(new int[] {}, b.splice(0, 0));
    assertSamelitude(items, b);

    final ArrayOfInt c = arrayFrom(items);
    assertSamelitude(new int[] {items[0], items[1]}, c.splice(0, 2));
    assertSamelitude(new int[] {items[2], items[3], items[4]}, c);
  }

  /**
   * Tests {@link ArrayOfNumber#splice(int, int)}.
   */
  public void testSplicingOfArraysOfNumbers() {
    final double[] items = new double[] {0.0, 0.01, 0.001, 0.0001, 0.00001};

    final ArrayOfNumber a = arrayFrom(items);
    assertSamelitude(items, a.splice(0, items.length));
    assertSamelitude(new double[] {}, a);

    final ArrayOfNumber b = arrayFrom(items);
    assertSamelitude(new double[] {}, b.splice(0, 0));
    assertSamelitude(items, b);

    final ArrayOfNumber c = arrayFrom(items);
    assertSamelitude(new double[] {items[0], items[1]}, c.splice(0, 2));
    assertSamelitude(new double[] {items[2], items[3], items[4]}, c);
  }

  /**
   * Tests {@link ArrayOfString#splice(int, int)}.
   */
  public void testSplicingOfArraysOfStrings() {
    final String[] items =
        new String[] {"One Gerbil", "Two Gerbil", "Three Gerbil", "Four Gerbil", "Five Gerbil"};

    final ArrayOfString a = arrayFrom(items);
    assertSamelitude(items, a.splice(0, items.length));
    assertSamelitude(new String[] {}, a);

    final ArrayOfString b = arrayFrom(items);
    assertSamelitude(new String[] {}, b.splice(0, 0));
    assertSamelitude(items, b);

    final ArrayOfString c = arrayFrom(items);
    assertSamelitude(new String[] {items[0], items[1]}, c.splice(0, 2));
    assertSamelitude(new String[] {items[2], items[3], items[4]}, c);
  }
}
