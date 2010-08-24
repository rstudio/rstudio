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
package com.google.gwt.view.client;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Test cases for {@link ListDataProvider}.
 */
public class ListDataProviderTest extends AbstractDataProviderTest {

  public void testConstructorList() {
    List<String> list = new ArrayList<String>();
    list.add("helloworld");
    ListDataProvider<String> provider = new ListDataProvider<String>(list);
    assertEquals("helloworld", provider.getList().get(0));
  }

  public void testFlush() {
    ListDataProvider<String> provider = createDataProvider();
    List<String> list = provider.getList();
    MockHasData<String> display = new MockHasData<String>();
    display.setVisibleRange(0, 15);
    provider.addDataDisplay(display);
    display.clearLastRowDataAndRange();
    display.setRowCount(0, true);

    // Add data to the list.
    for (int i = 0; i < 10; i++) {
      list.add("test " + i);
    }
    assertEquals(0, display.getRowCount());
    assertNull(display.getLastRowData());
    assertNull(display.getLastRowDataRange());

    // Flush the data immediately.
    provider.flush();
    assertEquals(10, display.getRowCount());
    assertTrue(display.isRowCountExact());
    assertEquals(list, display.getLastRowData());
    assertEquals(new Range(0, 10), display.getLastRowDataRange());
  }

  public void testListAdd() {
    ListDataProvider<String> provider = createListDataProvider(10);
    List<String> list = provider.getList();
    MockHasData<String> display = new MockHasData<String>();
    display.setVisibleRange(0, 15);
    provider.addDataDisplay(display);
    provider.flush();
    display.clearLastRowDataAndRange();

    // add(String).
    list.add("added");
    assertEquals("added", list.get(10));
    provider.flush();
    assertEquals(new Range(10, 1), display.getLastRowDataRange());

    // add(int, String).
    list.add(2, "inserted");
    assertEquals("inserted", list.get(2));
    provider.flush();
    assertEquals(new Range(2, 10), display.getLastRowDataRange());
  }

  public void testListAddAll() {
    ListDataProvider<String> provider = createListDataProvider(10);
    List<String> list = provider.getList();
    MockHasData<String> display = new MockHasData<String>();
    display.setVisibleRange(0, 25);
    provider.addDataDisplay(display);
    provider.flush();
    display.clearLastRowDataAndRange();

    // addAll(Collection).
    List<String> toAdd = createData(10, 3);
    list.addAll(toAdd);
    assertEquals("test 10", list.get(10));
    assertEquals("test 11", list.get(11));
    assertEquals("test 12", list.get(12));
    provider.flush();
    assertEquals(toAdd, display.getLastRowData());
    assertEquals(new Range(10, 3), display.getLastRowDataRange());

    // addAll(int, Collection).
    List<String> toInsert = createData(20, 3);
    list.addAll(2, toInsert);
    assertEquals("test 20", list.get(2));
    assertEquals("test 21", list.get(3));
    assertEquals("test 22", list.get(4));
    provider.flush();
    assertEquals(new Range(2, 14), display.getLastRowDataRange());
  }

  public void testListClear() {
    ListDataProvider<String> provider = createListDataProvider(10);
    List<String> list = provider.getList();
    assertEquals(10, list.size());
    MockHasData<String> display = new MockHasData<String>();
    display.setVisibleRange(0, 15);
    provider.addDataDisplay(display);
    provider.flush();
    display.clearLastRowDataAndRange();

    list.clear();
    assertEquals(0, list.size());
    provider.flush();
    assertEquals(0, display.getRowCount());
  }

  public void testListContains() {
    List<String> list = createListDataProvider(5).getList();

    // contains(Object).
    assertTrue(list.contains("test 0"));
    assertFalse(list.contains("platypus"));

    // containsAll(Collection).
    assertTrue(list.containsAll(createData(1, 2)));
    assertFalse(list.containsAll(createData(10, 2)));
  }

  public void testListEquals() {
    List<String> list = createListDataProvider(5).getList();
    assertTrue(list.equals(createData(0, 5)));
    assertFalse(list.equals(createData(0, 4)));
  }

  public void testListIndexOf() {
    List<String> list = createListDataProvider(5).getList();

    // indexOf(Object).
    assertEquals(3, list.indexOf("test 3"));
    assertEquals(-1, list.indexOf("duck"));

    // lastIndexOf(Object).
    assertEquals(3, list.lastIndexOf("test 3"));
    assertEquals(-1, list.lastIndexOf("duck"));
    list.add("test 3");
    assertEquals(5, list.lastIndexOf("test 3"));
  }

  public void testListIsEmpty() {
    List<String> list = createListDataProvider(0).getList();
    assertTrue(list.isEmpty());

    list.add("test");
    assertFalse(list.isEmpty());
  }

  public void testListIterator() {
    List<String> list = createListDataProvider(3).getList();
    Iterator<String> iterator = list.iterator();

    // Modify before next.
    try {
      iterator.remove();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected.
    }

    // next and hasNext.
    assertTrue(iterator.hasNext());
    assertEquals("test 0", iterator.next());
    assertEquals("test 1", iterator.next());
    assertEquals("test 2", iterator.next());
    assertFalse(iterator.hasNext());

    // remove.
    iterator = list.iterator();
    iterator.next();
    iterator.remove();
    assertEquals("test 1", list.get(0));
    assertEquals(2, list.size());
    try {
      iterator.remove();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected.
    }
    assertEquals("test 1", iterator.next());
  }

  public void testListListIterator() {
    List<String> list = createListDataProvider(3).getList();
    ListIterator<String> iterator = list.listIterator();

    // Modify before next.
    try {
      iterator.set("test");
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected.
    }
    try {
      iterator.add("test");
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected.
    }
    try {
      iterator.remove();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected.
    }

    // next, hasNext, and nextIndex.
    assertTrue(iterator.hasNext());
    assertEquals(0, iterator.nextIndex());
    assertEquals("test 0", iterator.next());
    assertEquals("test 1", iterator.next());
    assertEquals("test 2", iterator.next());
    assertFalse(iterator.hasNext());
    assertEquals(3, iterator.nextIndex());

    // previo0us, hasPrevious, and previousIndex.
    assertTrue(iterator.hasPrevious());
    assertEquals(2, iterator.previousIndex());
    assertEquals("test 2", iterator.previous());
    assertEquals("test 1", iterator.previous());
    assertEquals("test 0", iterator.previous());
    assertFalse(iterator.hasPrevious());
    assertEquals(-1, iterator.previousIndex());

    // set.
    iterator.set("set0");
    assertEquals("set0", list.get(0));
    iterator.set("set1");
    assertEquals("set1", list.get(0));

    // add.
    iterator.add("added");
    assertEquals("added", list.get(0));
    assertEquals("set1", list.get(1));
    assertEquals(4, list.size());
    try {
      iterator.add("double add");
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected.
    }
    assertEquals("set1", iterator.next());

    // remove.
    iterator.remove();
    assertEquals("test 1", list.get(1));
    assertEquals(3, list.size());
    try {
      iterator.remove();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected.
    }
    assertEquals("added", iterator.previous());
  }

  public void testListListIteratorAtIndex() {
    List<String> list = createListDataProvider(3).getList();
    ListIterator<String> iterator = list.listIterator(2);
    assertEquals("test 2", iterator.next());
  }

  public void testListRemove() {
    ListDataProvider<String> provider = createListDataProvider(10);
    List<String> list = provider.getList();
    MockHasData<String> display = new MockHasData<String>();
    display.setVisibleRange(0, 15);
    provider.addDataDisplay(display);
    provider.flush();
    display.clearLastRowDataAndRange();

    // remove(int).
    assertEquals("test 4", list.remove(4));
    assertEquals("test 5", list.get(4));
    provider.flush();
    assertEquals(new Range(4, 5), display.getLastRowDataRange());

    // remove(String).
    assertTrue(list.remove("test 2"));
    assertEquals("test 3", list.get(2));
    provider.flush();
    assertEquals(new Range(2, 6), display.getLastRowDataRange());

    // remove(String)
    assertFalse(list.remove("not in list"));
  }

  public void testListRemoveAll() {
    ListDataProvider<String> provider = createListDataProvider(10);
    List<String> list = provider.getList();
    MockHasData<String> display = new MockHasData<String>();
    display.setVisibleRange(0, 15);
    provider.addDataDisplay(display);
    provider.flush();
    display.clearLastRowDataAndRange();

    List<String> toRemove = createData(2, 3);
    assertTrue(list.removeAll(toRemove));
    assertEquals(7, list.size());
    assertEquals("test 5", list.get(2));
    provider.flush();
    assertEquals(new Range(0, 7), display.getLastRowDataRange());

    assertFalse(list.removeAll(toRemove));
  }

  public void testListRetainAll() {
    ListDataProvider<String> provider = createListDataProvider(10);
    List<String> list = provider.getList();
    MockHasData<String> display = new MockHasData<String>();
    display.setVisibleRange(0, 15);
    provider.addDataDisplay(display);
    provider.flush();
    display.clearLastRowDataAndRange();

    List<String> toRetain = createData(2, 3);
    assertTrue(list.retainAll(toRetain));
    assertEquals(3, list.size());
    assertEquals("test 2", list.get(0));
    provider.flush();
    assertEquals(new Range(0, 3), display.getLastRowDataRange());
  }

  public void testListSet() {
    ListDataProvider<String> provider = createListDataProvider(10);
    List<String> list = provider.getList();
    MockHasData<String> display = new MockHasData<String>();
    display.setVisibleRange(0, 15);
    provider.addDataDisplay(display);
    provider.flush();
    display.clearLastRowDataAndRange();

    list.set(3, "newvalue");
    assertEquals("newvalue", list.get(3));
    provider.flush();
    assertEquals(new Range(3, 1), display.getLastRowDataRange());
  }

  public void testSubList() {
    ListDataProvider<String> provider = createListDataProvider(10);
    List<String> list = provider.getList();
    MockHasData<String> display = new MockHasData<String>();
    display.setVisibleRange(0, 15);
    provider.addDataDisplay(display);
    provider.flush();
    display.clearLastRowDataAndRange();

    List<String> subList = list.subList(2, 5);
    assertEquals(3, subList.size());

    subList.set(0, "test");
    assertEquals("test", subList.get(0));
    assertEquals("test", list.get(2));
    provider.flush();
    assertEquals(new Range(2, 1), display.getLastRowDataRange());
  }

  public void testToArray() {
    List<String> list = createListDataProvider(3).getList();
    String[] expected = new String[]{"test 0", "test 1", "test 2"};

    Object[] objects = list.toArray();
    String[] strings = list.toArray(new String[3]);
    assertEquals(3, strings.length);
    assertEquals(3, objects.length);
    for (int i = 0; i < 3; i++) {
      String s = expected[i];
      assertEquals(s, objects[i]);
      assertEquals(s, strings[i]);
    }
  }

  public void testListSize() {
    List<String> list = createListDataProvider(10).getList();
    assertEquals(10, list.size());
  }

  public void testOnRangeChanged() {
    ListDataProvider<String> provider = createListDataProvider(10);
    List<String> list = provider.getList();
    MockHasData<String> display0 = new MockHasData<String>();
    MockHasData<String> display1 = new MockHasData<String>();
    display0.setVisibleRange(0, 15);
    display1.setVisibleRange(0, 15);
    provider.addDataDisplay(display0);
    provider.addDataDisplay(display1);
    provider.flush();
    display0.clearLastRowDataAndRange();
    display1.clearLastRowDataAndRange();

    // Change the range of display0.
    display0.setVisibleRange(0, 12);
    assertEquals(list, display0.getLastRowData());
    assertEquals(new Range(0, 10), display0.getLastRowDataRange());
    assertNull(display1.getLastRowData());
    assertNull(display1.getLastRowDataRange());
  }

  public void testRefresh() {
    ListDataProvider<String> provider = createListDataProvider(10);
    List<String> list = provider.getList();
    MockHasData<String> display = new MockHasData<String>();
    display.setVisibleRange(0, 15);
    provider.addDataDisplay(display);
    provider.flush();
    display.clearLastRowDataAndRange();

    // Refresh the display.
    provider.refresh();
    assertEquals(list, display.getLastRowData());
    assertEquals(new Range(0, 10), display.getLastRowDataRange());
  }

  public void testSetList() {
    ListDataProvider<String> provider = createListDataProvider(10);
    final MockHasData<String> display = new MockHasData<String>();
    display.setVisibleRange(0, 15);
    provider.addDataDisplay(display);
    provider.flush();
    display.clearLastRowDataAndRange();
    List<String> oldList = provider.getList();
    assertEquals("test 0", oldList.get(0));

    // Replace the list.
    List<String> replace = new ArrayList<String>();
    replace.add("helloworld");
    provider.setList(replace);
    assertEquals("helloworld", provider.getList().get(0));
    assertEquals(1, display.getRowCount());
    assertEquals(replace, display.getLastRowData());
    assertEquals(new Range(0, 1), display.getLastRowDataRange());
    display.clearLastRowDataAndRange();

    // Verify that the old list doesn't trigger updates in the display.
    oldList.set(0, "newValue");
    delayTestFinish(2000);
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      public void execute() {
        assertNull(display.getLastRowData());
        finishTest();
      }
    });
  }

  public void testSetListEmpty() {
    ListDataProvider<String> provider = createListDataProvider(10);
    MockHasData<String> display = new MockHasData<String>();
    display.setVisibleRange(0, 15);
    provider.addDataDisplay(display);
    provider.flush();
    assertEquals(10, display.getRowCount());
    display.clearLastRowDataAndRange();
    assertEquals("test 0", provider.getList().get(0));

    List<String> replace = new ArrayList<String>();
    provider.setList(replace);
    assertEquals(0, display.getRowCount());
    // An empty set should NOT set the row values.
    assertEquals(replace, display.getLastRowData());
    assertEquals(new Range(0, 0), display.getLastRowDataRange());
  }

  @Override
  protected ListDataProvider<String> createDataProvider() {
    return createListDataProvider(0);
  }

  private ListDataProvider<String> createListDataProvider(int size) {
    return new ListDataProvider<String>(createData(0, size));
  }
}
