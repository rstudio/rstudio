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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Test cases for {@link ListViewAdapter}.
 */
public class ListViewAdapterTest extends AbstractListViewAdapterTest {

  public void testConstructorList() {
    List<String> list = new ArrayList<String>();
    list.add("helloworld");
    ListViewAdapter<String> adapter = new ListViewAdapter<String>(list);
    assertEquals("helloworld", adapter.getList().get(0));
  }

  public void testFlush() {
    ListViewAdapter<String> adapter = createListViewAdapter();
    List<String> list = adapter.getList();
    MockHasData<String> view = new MockHasData<String>();
    view.setVisibleRange(0, 15);
    adapter.addView(view);
    view.clearLastRowValuesAndRange();
    view.setRowCount(0, true);

    // Add data to the list.
    for (int i = 0; i < 10; i++) {
      list.add("test " + i);
    }
    assertEquals(0, view.getRowCount());
    assertNull(view.getLastRowValues());
    assertNull(view.getLastRowValuesRange());

    // Flush the data immediately.
    adapter.flush();
    assertEquals(10, view.getRowCount());
    assertTrue(view.isRowCountExact());
    assertEquals(list, view.getLastRowValues());
    assertEquals(new Range(0, 10), view.getLastRowValuesRange());
  }

  public void testListAdd() {
    ListViewAdapter<String> adapter = createListViewAdapter(10);
    List<String> list = adapter.getList();
    MockHasData<String> view = new MockHasData<String>();
    view.setVisibleRange(0, 15);
    adapter.addView(view);
    adapter.flush();
    view.clearLastRowValuesAndRange();

    // add(String).
    list.add("added");
    assertEquals("added", list.get(10));
    adapter.flush();
    assertEquals(new Range(10, 1), view.getLastRowValuesRange());

    // add(int, String).
    list.add(2, "inserted");
    assertEquals("inserted", list.get(2));
    adapter.flush();
    assertEquals(new Range(2, 10), view.getLastRowValuesRange());
  }

  public void testListAddAll() {
    ListViewAdapter<String> adapter = createListViewAdapter(10);
    List<String> list = adapter.getList();
    MockHasData<String> view = new MockHasData<String>();
    view.setVisibleRange(0, 25);
    adapter.addView(view);
    adapter.flush();
    view.clearLastRowValuesAndRange();

    // addAll(Collection).
    List<String> toAdd = createData(10, 3);
    list.addAll(toAdd);
    assertEquals("test 10", list.get(10));
    assertEquals("test 11", list.get(11));
    assertEquals("test 12", list.get(12));
    adapter.flush();
    assertEquals(toAdd, view.getLastRowValues());
    assertEquals(new Range(10, 3), view.getLastRowValuesRange());

    // addAll(int, Collection).
    List<String> toInsert = createData(20, 3);
    list.addAll(2, toInsert);
    assertEquals("test 20", list.get(2));
    assertEquals("test 21", list.get(3));
    assertEquals("test 22", list.get(4));
    adapter.flush();
    assertEquals(new Range(2, 14), view.getLastRowValuesRange());
  }

  public void testListClear() {
    ListViewAdapter<String> adapter = createListViewAdapter(10);
    List<String> list = adapter.getList();
    assertEquals(10, list.size());
    MockHasData<String> view = new MockHasData<String>();
    view.setVisibleRange(0, 15);
    adapter.addView(view);
    adapter.flush();
    view.clearLastRowValuesAndRange();

    list.clear();
    assertEquals(0, list.size());
    adapter.flush();
    assertEquals(0, view.getRowCount());
  }

  public void testListContains() {
    List<String> list = createListViewAdapter(5).getList();

    // contains(Object).
    assertTrue(list.contains("test 0"));
    assertFalse(list.contains("platypus"));

    // containsAll(Collection).
    assertTrue(list.containsAll(createData(1, 2)));
    assertFalse(list.containsAll(createData(10, 2)));
  }

  public void testListEquals() {
    List<String> list = createListViewAdapter(5).getList();
    assertTrue(list.equals(createData(0, 5)));
    assertFalse(list.equals(createData(0, 4)));
  }

  public void testListIndexOf() {
    List<String> list = createListViewAdapter(5).getList();

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
    List<String> list = createListViewAdapter(0).getList();
    assertTrue(list.isEmpty());

    list.add("test");
    assertFalse(list.isEmpty());
  }

  public void testListIterator() {
    List<String> list = createListViewAdapter(3).getList();
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
    List<String> list = createListViewAdapter(3).getList();
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
    List<String> list = createListViewAdapter(3).getList();
    ListIterator<String> iterator = list.listIterator(2);
    assertEquals("test 2", iterator.next());
  }

  public void testListRemove() {
    ListViewAdapter<String> adapter = createListViewAdapter(10);
    List<String> list = adapter.getList();
    MockHasData<String> view = new MockHasData<String>();
    view.setVisibleRange(0, 15);
    adapter.addView(view);
    adapter.flush();
    view.clearLastRowValuesAndRange();

    // remove(int).
    assertEquals("test 4", list.remove(4));
    assertEquals("test 5", list.get(4));
    adapter.flush();
    assertEquals(new Range(4, 5), view.getLastRowValuesRange());

    // remove(String).
    assertTrue(list.remove("test 2"));
    assertEquals("test 3", list.get(2));
    adapter.flush();
    assertEquals(new Range(2, 6), view.getLastRowValuesRange());

    // remove(String)
    assertFalse(list.remove("not in list"));
  }

  public void testListRemoveAll() {
    ListViewAdapter<String> adapter = createListViewAdapter(10);
    List<String> list = adapter.getList();
    MockHasData<String> view = new MockHasData<String>();
    view.setVisibleRange(0, 15);
    adapter.addView(view);
    adapter.flush();
    view.clearLastRowValuesAndRange();

    List<String> toRemove = createData(2, 3);
    assertTrue(list.removeAll(toRemove));
    assertEquals(7, list.size());
    assertEquals("test 5", list.get(2));
    adapter.flush();
    assertEquals(new Range(0, 7), view.getLastRowValuesRange());

    assertFalse(list.removeAll(toRemove));
  }

  public void testListRetainAll() {
    ListViewAdapter<String> adapter = createListViewAdapter(10);
    List<String> list = adapter.getList();
    MockHasData<String> view = new MockHasData<String>();
    view.setVisibleRange(0, 15);
    adapter.addView(view);
    adapter.flush();
    view.clearLastRowValuesAndRange();

    List<String> toRetain = createData(2, 3);
    assertTrue(list.retainAll(toRetain));
    assertEquals(3, list.size());
    assertEquals("test 2", list.get(0));
    adapter.flush();
    assertEquals(new Range(0, 3), view.getLastRowValuesRange());
  }

  public void testListSet() {
    ListViewAdapter<String> adapter = createListViewAdapter(10);
    List<String> list = adapter.getList();
    MockHasData<String> view = new MockHasData<String>();
    view.setVisibleRange(0, 15);
    adapter.addView(view);
    adapter.flush();
    view.clearLastRowValuesAndRange();

    list.set(3, "newvalue");
    assertEquals("newvalue", list.get(3));
    adapter.flush();
    assertEquals(new Range(3, 1), view.getLastRowValuesRange());
  }

  public void testSubList() {
    ListViewAdapter<String> adapter = createListViewAdapter(10);
    List<String> list = adapter.getList();
    MockHasData<String> view = new MockHasData<String>();
    view.setVisibleRange(0, 15);
    adapter.addView(view);
    adapter.flush();
    view.clearLastRowValuesAndRange();

    List<String> subList = list.subList(2, 5);
    assertEquals(3, subList.size());

    subList.set(0, "test");
    assertEquals("test", subList.get(0));
    assertEquals("test", list.get(2));
    adapter.flush();
    assertEquals(new Range(2, 1), view.getLastRowValuesRange());
  }

  public void testToArray() {
    List<String> list = createListViewAdapter(3).getList();
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
    List<String> list = createListViewAdapter(10).getList();
    assertEquals(10, list.size());
  }

  public void testOnRangeChanged() {
    ListViewAdapter<String> adapter = createListViewAdapter(10);
    List<String> list = adapter.getList();
    MockHasData<String> view0 = new MockHasData<String>();
    MockHasData<String> view1 = new MockHasData<String>();
    view0.setVisibleRange(0, 15);
    view1.setVisibleRange(0, 15);
    adapter.addView(view0);
    adapter.addView(view1);
    adapter.flush();
    view0.clearLastRowValuesAndRange();
    view1.clearLastRowValuesAndRange();

    // Change the range of view0.
    view0.setVisibleRange(0, 12);
    assertEquals(list, view0.getLastRowValues());
    assertEquals(new Range(0, 10), view0.getLastRowValuesRange());
    assertNull(view1.getLastRowValues());
    assertNull(view1.getLastRowValuesRange());
  }

  public void testRefresh() {
    ListViewAdapter<String> adapter = createListViewAdapter(10);
    List<String> list = adapter.getList();
    MockHasData<String> view = new MockHasData<String>();
    view.setVisibleRange(0, 15);
    adapter.addView(view);
    adapter.flush();
    view.clearLastRowValuesAndRange();

    // Refresh the view.
    adapter.refresh();
    assertEquals(list, view.getLastRowValues());
    assertEquals(new Range(0, 10), view.getLastRowValuesRange());
  }

  public void testSetList() {
    ListViewAdapter<String> adapter = createListViewAdapter(10);
    MockHasData<String> view = new MockHasData<String>();
    view.setVisibleRange(0, 15);
    adapter.addView(view);
    adapter.flush();
    view.clearLastRowValuesAndRange();
    assertEquals("test 0", adapter.getList().get(0));

    List<String> replace = new ArrayList<String>();
    replace.add("helloworld");
    adapter.setList(replace);
    assertEquals("helloworld", adapter.getList().get(0));
    assertEquals(1, view.getRowCount());
    assertEquals(replace, view.getLastRowValues());
    assertEquals(new Range(0, 1), view.getLastRowValuesRange());
  }

  public void testSetListEmpty() {
    ListViewAdapter<String> adapter = createListViewAdapter(10);
    MockHasData<String> view = new MockHasData<String>();
    view.setVisibleRange(0, 15);
    adapter.addView(view);
    adapter.flush();
    assertEquals(10, view.getRowCount());
    view.clearLastRowValuesAndRange();
    assertEquals("test 0", adapter.getList().get(0));

    List<String> replace = new ArrayList<String>();
    adapter.setList(replace);
    assertEquals(0, view.getRowCount());
    // An empty set should set the row values.
    assertEquals(replace, view.getLastRowValues());
    assertEquals(new Range(0, 0), view.getLastRowValuesRange());
  }

  @Override
  protected ListViewAdapter<String> createListViewAdapter() {
    return createListViewAdapter(0);
  }

  private ListViewAdapter<String> createListViewAdapter(int size) {
    return new ListViewAdapter<String>(createData(0, size));
  }
}
