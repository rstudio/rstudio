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
package com.google.gwt.editor.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.adapters.EditorSource;
import com.google.gwt.editor.client.adapters.ListEditor;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests error propagation in generated code.
 */
public class EditorErrorTest extends GWTTestCase {
  class AddressEditorWithErrors extends AddressEditor implements
      ValueAwareEditor<Address> {

    private EditorDelegate<Address> delegate;

    public void flush() {
      delegate.recordError("Hello Errors!", null, null);
    }

    public void onPropertyChange(String... paths) {
    }

    public void setDelegate(EditorDelegate<Address> delegate) {
      this.delegate = delegate;
    }

    public void setValue(Address value) {
    }
  }

  class PersonEditorReceivesErrors extends PersonEditor implements
      HasEditorErrors<Person> {
    List<EditorError> errors;

    public void showErrors(List<EditorError> errors) {
      this.errors = errors;
      for (EditorError error : errors) {
        error.setConsumed(true);
      }
    }
  }

  class Workgroup {
    List<Person> people = new ArrayList<Person>();

    List<Person> getPeople() {
      return people;
    }
  }

  class WorkgroupEditor implements HasEditorErrors<Workgroup> {
    ListEditor<Person, PersonEditor> people = ListEditor.of(new EditorSource<PersonEditor>() {
      @Override
      public PersonEditor create(int index) {
        PersonEditor toReturn = new PersonEditor();
        toReturn.addressEditor = new AddressEditorWithErrors();
        return toReturn;
      }
    });

    private List<EditorError> errors;

    public void showErrors(List<EditorError> errors) {
      this.errors = errors;
    }
  }

  interface WorkgroupEditorDriver extends
      SimpleBeanEditorDriver<Workgroup, WorkgroupEditor> {
  }

  class WorkgroupNestedErrorsEditor implements HasEditorErrors<Workgroup> {
    ListEditor<Person, PersonEditorReceivesErrors> people = ListEditor.of(new EditorSource<PersonEditorReceivesErrors>() {
      @Override
      public PersonEditorReceivesErrors create(int index) {
        PersonEditorReceivesErrors toReturn = new PersonEditorReceivesErrors();
        toReturn.addressEditor = new AddressEditorWithErrors();
        return toReturn;
      }
    });

    List<EditorError> errors;

    public void showErrors(List<EditorError> errors) {
      this.errors = errors;
    }
  }

  interface WorkgroupNestedErrorsEditorDriver extends
      SimpleBeanEditorDriver<Workgroup, WorkgroupNestedErrorsEditor> {
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.editor.Editor";
  }

  public void testListOfErrors() {
    Address a = new Address();
    Person p1 = new Person();
    p1.address = a;

    Person p2 = new Person();
    p2.address = a;

    Workgroup workgroup = new Workgroup();
    workgroup.people.addAll(Arrays.asList(p1, p2));

    WorkgroupEditorDriver driver = GWT.create(WorkgroupEditorDriver.class);
    WorkgroupEditor editor = new WorkgroupEditor();
    driver.initialize(editor);
    driver.edit(workgroup);
    driver.flush();

    List<EditorError> errors = editor.errors;
    assertNotNull(errors);
    assertEquals("Wrong number of EditorErrors", 2, errors.size());
    assertEquals(Arrays.asList("people[0].address", "people[1].address"),
        Arrays.asList(errors.get(0).getPath(), errors.get(1).getPath()));
  }

  public void testNestedErrors() {
    Address a = new Address();
    Person p1 = new Person();
    p1.address = a;

    Person p2 = new Person();
    p2.address = a;

    Workgroup workgroup = new Workgroup();
    workgroup.people.addAll(Arrays.asList(p1, p2));

    WorkgroupNestedErrorsEditorDriver driver = GWT.create(WorkgroupNestedErrorsEditorDriver.class);
    WorkgroupNestedErrorsEditor editor = new WorkgroupNestedErrorsEditor();
    driver.initialize(editor);
    driver.edit(workgroup);
    driver.flush();

    // Verify the nested editors received the errors
    List<EditorError> errors = editor.errors;
    assertNotNull(errors);
    assertEquals(0, errors.size());

    List<PersonEditorReceivesErrors> editors = editor.people.getEditors();
    for (int i = 0, j = editors.size(); i < j; i++) {
      PersonEditorReceivesErrors personEditor = editors.get(i);
      assertEquals(1, personEditor.errors.size());

      EditorError error = personEditor.errors.get(0);
      assertNotNull(error);
      assertEquals("people[" + i + "].address", error.getAbsolutePath());
      assertEquals("Hello Errors!", error.getMessage());
      assertEquals("address", error.getPath());
      assertNull(error.getUserData());
      assertSame(a, error.getValue());
      assertSame(personEditor.addressEditor, error.getEditor());
    }
  }

  public void testNoErrors() {
    Address a = new Address();
    Person p = new Person();
    p.address = a;

    PersonEditorDriver driver = GWT.create(PersonEditorDriver.class);
    PersonEditorReceivesErrors editor = new PersonEditorReceivesErrors();
    driver.initialize(editor);
    driver.edit(p);
    driver.flush();
    assertEquals(0, editor.errors.size());
  }

  public void testSimpleError() {
    PersonEditorReceivesErrors editor = new PersonEditorReceivesErrors();
    editor.addressEditor = new AddressEditorWithErrors();

    Address a = new Address();
    Person p = new Person();
    p.address = a;

    PersonEditorDriver driver = GWT.create(PersonEditorDriver.class);
    driver.initialize(editor);
    driver.edit(p);
    driver.flush();

    List<EditorError> list = editor.errors;
    assertNotNull(list);
    assertEquals(1, list.size());

    EditorError error = list.get(0);
    assertNotNull(error);
    assertEquals("address", error.getAbsolutePath());
    assertEquals("Hello Errors!", error.getMessage());
    assertEquals("address", error.getPath());
    assertNull(error.getUserData());
    assertSame(a, error.getValue());
    assertSame(editor.addressEditor, error.getEditor());
  }
}
