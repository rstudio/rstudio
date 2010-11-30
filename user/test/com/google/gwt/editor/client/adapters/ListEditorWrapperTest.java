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
package com.google.gwt.editor.client.adapters;

import com.google.gwt.editor.client.testing.FakeEditorSource;
import com.google.gwt.editor.client.testing.FakeLeafValueEditor;
import com.google.gwt.editor.client.testing.MockEditorChain;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests the {@link ListEditorWrapper} type. This is where all of
 * {@link ListEditor}'s bookkeeping lies.
 */
public class ListEditorWrapperTest extends TestCase {

  public void testAdd() {
    List<Object> backing = new ArrayList<Object>();
    FakeEditorSource<Object> source = new FakeEditorSource<Object>();
    MockEditorChain<Object> chain = new MockEditorChain<Object>();

    ListEditorWrapper<Object, FakeLeafValueEditor<Object>> wrapper = new ListEditorWrapper<Object, FakeLeafValueEditor<Object>>(
        backing, chain, source);
    wrapper.attach();

    assertTrue(wrapper.getEditors().isEmpty());

    Object o1 = new Object();
    wrapper.add(o1);
    assertEquals(0, backing.size());
    wrapper.flush();
    assertEquals(1, backing.size());
    assertSame(o1, backing.get(0));
    FakeLeafValueEditor<Object> editor1 = wrapper.getEditors().get(0);
    assertSame(o1, editor1.getValue());
    assertEquals(0, source.getLastKnownPosition(editor1));
    assertTrue(chain.isAttached(editor1));

    Object o0 = new Object();
    wrapper.add(0, o0);
    assertEquals(1, backing.size());
    wrapper.flush();
    assertEquals(2, backing.size());
    assertSame(o0, backing.get(0));
    assertSame(o1, backing.get(1));
    FakeLeafValueEditor<Object> editor0 = wrapper.getEditors().get(0);
    assertNotSame(editor0, editor1);
    assertSame(editor1, wrapper.getEditors().get(1));
    assertSame(o0, editor0.getValue());
    assertEquals(0, source.getLastKnownPosition(editor0));
    assertEquals(1, source.getLastKnownPosition(editor1));
    assertTrue(chain.isAttached(editor0));

    o1 = new Object();
    editor1.setValue(o1);
    assertNotSame(backing.get(1), o1);
    wrapper.flush();
    assertSame(backing.get(1), o1);

    wrapper.detach();
    assertFalse(chain.isAttached(editor0));
    assertEquals(FakeEditorSource.DISPOSED,
        source.getLastKnownPosition(editor0));
    assertFalse(chain.isAttached(editor1));
    assertEquals(FakeEditorSource.DISPOSED,
        source.getLastKnownPosition(editor1));
  }

  public void testEmpty() {
    List<Object> backing = new ArrayList<Object>();
    FakeEditorSource<Object> source = new FakeEditorSource<Object>();

    ListEditorWrapper<Object, FakeLeafValueEditor<Object>> wrapper = new ListEditorWrapper<Object, FakeLeafValueEditor<Object>>(
        backing, new MockEditorChain<Object>(), source);
    wrapper.attach();

    assertTrue(wrapper.getEditors().isEmpty());
    wrapper.flush();
    wrapper.detach();
  }

  @SuppressWarnings("unchecked")
  public void testRemove() {
    Object o0 = new Object();
    Object o1 = new Object();
    Object o2 = new Object();
    List<Object> backing = new ArrayList<Object>(Arrays.asList(o0, o1, o2));
    FakeEditorSource<Object> source = new FakeEditorSource<Object>();
    MockEditorChain<Object> chain = new MockEditorChain<Object>();

    ListEditorWrapper<Object, FakeLeafValueEditor<Object>> wrapper = new ListEditorWrapper<Object, FakeLeafValueEditor<Object>>(
        backing, chain, source);
    wrapper.attach();

    assertEquals(3, wrapper.getEditors().size());
    FakeLeafValueEditor<Object> e0 = wrapper.getEditors().get(0);
    FakeLeafValueEditor<Object> e1 = wrapper.getEditors().get(1);
    FakeLeafValueEditor<Object> e2 = wrapper.getEditors().get(2);
    assertSame(o0, e0.getValue());
    assertSame(o1, e1.getValue());
    assertSame(o2, e2.getValue());

    wrapper.remove(1);
    assertEquals(Arrays.asList(o0, o1, o2), backing);
    assertEquals(Arrays.asList(e0, e2), wrapper.getEditors());
    wrapper.flush();
    assertEquals(Arrays.asList(o0, o2), backing);
    assertEquals(Arrays.asList(e0, e2), wrapper.getEditors());
    assertFalse(chain.isAttached(e1));
    assertEquals(FakeEditorSource.DISPOSED, source.getLastKnownPosition(e1));
    assertEquals(1, source.getLastKnownPosition(e2));

    wrapper.set(1, o1);
    assertEquals(Arrays.asList(o0, o2), backing);
    wrapper.flush();
    assertEquals(Arrays.asList(o0, o1), backing);
    // Re-use existing editor
    assertEquals(Arrays.asList(e0, e2), wrapper.getEditors());
    assertSame(o1, e2.getValue());
  }
}
