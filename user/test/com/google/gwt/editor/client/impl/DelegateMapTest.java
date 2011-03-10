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
package com.google.gwt.editor.client.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Address;
import com.google.gwt.editor.client.AddressEditor;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorDelegate;
import com.google.gwt.editor.client.HasEditorDelegate;
import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.client.Person;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.editor.client.adapters.SimpleEditor;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for DelegateMap.
 */
public class DelegateMapTest extends GWTTestCase {
  class AddressCoEditorView extends AddressEditor implements IsEditor<AddressEditor> {
    private AddressEditor addressEditor = new AddressEditor();

    public AddressEditor asEditor() {
      return addressEditor;
    }
  }

  class PersonEditorWithCoAddressEditorView implements Editor<Person> {
    AddressCoEditorView addressEditor = new AddressCoEditorView();
    SimpleEditor<String> name = SimpleEditor.of("uninitialized");
    @Path("manager.name")
    SimpleEditorWithDelegate<String> managerName = new SimpleEditorWithDelegate<String>(
        "uninitialized");
  }

  interface PersonEditorWithCoAddressEditorViewDriver extends
      SimpleBeanEditorDriver<Person, PersonEditorWithCoAddressEditorView> {
  }

  class SimpleEditorWithDelegate<T> extends SimpleEditor<T> implements HasEditorDelegate<T> {
    EditorDelegate<T> delegate;

    public SimpleEditorWithDelegate(T value) {
      super(value);
    }

    public void setDelegate(EditorDelegate<T> delegate) {
      this.delegate = delegate;
    }
  }

  private AbstractSimpleBeanEditorDriver<Person, PersonEditorWithCoAddressEditorView> driver;
  private PersonEditorWithCoAddressEditorView editor;
  private DelegateMap map;
  private Person person;

  @Override
  public String getModuleName() {
    return "com.google.gwt.editor.Editor";
  }

  public void test() {
    // Test by-object
    assertEquals(Arrays.asList(editor), editors(map, person));
    assertEquals(Arrays.asList(editor.addressEditor.addressEditor, editor.addressEditor), editors(
        map, person.getAddress()));

    // Test by-path
    assertEquals(Arrays.asList(editor), editors(map, ""));
    assertEquals(Arrays.asList(editor.addressEditor.addressEditor, editor.addressEditor), editors(
        map, "address"));
    assertEquals(Arrays.<Editor<?>> asList(editor.managerName), editors(map, "manager.name"));
  }

  public void testSimplePath() {
    assertSame(editor.name, map.getEditorByPath("name").get(0));
    assertSame(editor.managerName, map.getEditorByPath("manager.name").get(0));
  }

  @Override
  protected void gwtSetUp() throws Exception {
    Address a = new Address();
    a.setCity("city");
    a.setStreet("street");

    Person m = new Person();
    m.setName("manager");

    person = new Person();
    person.setName("name");
    person.setAddress(a);
    person.setManager(m);

    editor = new PersonEditorWithCoAddressEditorView();
    driver = GWT.create(PersonEditorWithCoAddressEditorViewDriver.class);
    driver.initialize(editor);
    driver.edit(person);

    map = DelegateMap.of(driver, DelegateMap.IDENTITY);
  }

  private List<Editor<?>> editors(DelegateMap map, Object o) {
    List<Editor<?>> toReturn = new ArrayList<Editor<?>>();
    for (AbstractEditorDelegate<?, ?> delegate : map.get(o)) {
      toReturn.add(delegate.getEditor());
    }
    return toReturn;
  }

  private List<Editor<?>> editors(DelegateMap map, String path) {
    List<Editor<?>> toReturn = new ArrayList<Editor<?>>();
    for (AbstractEditorDelegate<?, ?> delegate : map.getDelegatesByPath(path)) {
      toReturn.add(delegate.getEditor());
    }
    return toReturn;
  }
}
