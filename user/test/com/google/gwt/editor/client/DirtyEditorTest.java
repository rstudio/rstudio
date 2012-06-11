/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.editor.client.SimpleBeanEditorTest.PersonEditorWithDelegate;
import com.google.gwt.editor.client.SimpleBeanEditorTest.PersonEditorWithDelegateDriver;
import com.google.gwt.editor.client.SimpleBeanEditorTest.PersonEditorWithOptionalAddressDriver;
import com.google.gwt.editor.client.SimpleBeanEditorTest.PersonEditorWithOptionalAddressEditor;
import com.google.gwt.editor.client.adapters.EditorSource;
import com.google.gwt.editor.client.adapters.ListEditor;
import com.google.gwt.editor.client.adapters.OptionalFieldEditor;
import com.google.gwt.editor.client.adapters.SimpleEditor;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.Collections;
import java.util.List;

/**
 * 
 */
public class DirtyEditorTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.editor.Editor";
  }

  public void testDirty() {
    PersonEditor editor = new PersonEditor();
    PersonEditorDriver driver = GWT.create(PersonEditorDriver.class);
    driver.initialize(editor);
    driver.edit(person);

    // Freshly-initialized should not be dirty
    assertFalse(driver.isDirty());

    // Changing the Person object should not affect the dirty status
    person.setName("blah");
    assertFalse(driver.isDirty());

    editor.addressEditor.city.setValue("Foo");
    assertTrue(driver.isDirty());

    // Check that flushing doesn't clear the dirty state
    driver.flush();
    assertTrue(driver.isDirty());

    // Reset to original value
    editor.addressEditor.city.setValue("City");
    assertFalse(driver.isDirty());

    // Try a null value
    editor.managerName.setValue(null);
    assertTrue(driver.isDirty());
  }

  public void testDirtyWithDelegate() {
    PersonEditorWithDelegate editor = new PersonEditorWithDelegate();
    PersonEditorWithDelegateDriver driver = GWT.create(PersonEditorWithDelegateDriver.class);
    driver.initialize(editor);
    driver.edit(person);

    // Freshly-initialized should not be dirty
    assertFalse(driver.isDirty());

    // Use the delegate to toggle the state
    editor.delegate.setDirty(true);
    assertTrue(driver.isDirty());

    // Use the delegate to clear the state
    editor.delegate.setDirty(false);
    assertFalse(driver.isDirty());

    // Check that the delegate has no influence over values
    editor.addressEditor.city.setValue("edited");
    assertTrue(driver.isDirty());
    editor.delegate.setDirty(false);
    assertTrue(driver.isDirty());
    editor.delegate.setDirty(true);
    assertTrue(driver.isDirty());
  }

  public void testDirtyWithOptionalEditor() {
    person.address = null;

    AddressEditor addressEditor = new AddressEditor();
    PersonEditorWithOptionalAddressEditor editor =
        new PersonEditorWithOptionalAddressEditor(addressEditor);
    PersonEditorWithOptionalAddressDriver driver =
        GWT.create(PersonEditorWithOptionalAddressDriver.class);
    driver.initialize(editor);
    driver.edit(person);

    // Freshly-initialized should not be dirty
    assertFalse(driver.isDirty());

    // Change the instance being edited
    Address a = new Address();
    editor.address.setValue(a);
    assertTrue(driver.isDirty());

    // Check restoration works
    editor.address.setValue(null);
    assertFalse(driver.isDirty());
  }

  public void testEditResetsDirty() {
    PersonEditorWithDelegate editor = new PersonEditorWithDelegate();
    PersonEditorWithDelegateDriver driver = GWT.create(PersonEditorWithDelegateDriver.class);
    driver.initialize(editor);
    driver.edit(person);

    // Freshly-initialized should not be dirty
    assertFalse(driver.isDirty());

    editor.addressEditor.city.setValue("blah");
    assertTrue(driver.isDirty());

    driver.edit(person);
    assertFalse(driver.isDirty());

    editor.delegate.setDirty(true);
    assertTrue(driver.isDirty());
    driver.edit(person);
    assertFalse(driver.isDirty());
  }

  public void testEditResetsDirtyReplacement() {
    Person person2 = new Person();
    person2.setName("Pod");

    PersonEditorWithDelegate editor = new PersonEditorWithDelegate();
    PersonEditorWithDelegateDriver driver = GWT.create(PersonEditorWithDelegateDriver.class);
    driver.initialize(editor);
    driver.edit(person);

    editor.addressEditor.street.setValue("blah");
    assertTrue(driver.isDirty());

    driver.edit(person2);
    assertFalse(driver.isDirty());
  }

  class Workgroup {
    private String label;

    String getLabel() {
      return label;
    }

    void setLabel(String label) {
      this.label = label;
    }

    private List<Person> people;

    List<Person> getPeople() {
      return people;
    }

    void setPeople(List<Person> people) {
      this.people = people;
    }
  }
  class WorkgroupEditor implements Editor<Workgroup> {
    SimpleEditor<String> label = SimpleEditor.of();
    OptionalFieldEditor<List<Person>, ListEditor<Person, PersonEditor>> people =
        OptionalFieldEditor.of(ListEditor
            // The method type parameterization is needed by OpenJDK, please keep.
            .<Person, PersonEditor> of(new EditorSource<PersonEditor>() {
              @Override
              public PersonEditor create(int index) {
                return new PersonEditor();
              }
            }));
  }

  interface WorkgroupEditorDriver extends SimpleBeanEditorDriver<Workgroup, WorkgroupEditor> {
  }

  /**
   * CompositeEditors have an implementation complication due to the EditorChain needing to patch
   * the composite editors into the hierarchy.
   */
  public void testDirtyOptionalList() {
    WorkgroupEditorDriver driver = GWT.create(WorkgroupEditorDriver.class);
    WorkgroupEditor editor = new WorkgroupEditor();
    driver.initialize(editor);

    Workgroup wg = new Workgroup();
    driver.edit(wg);
    assertFalse(driver.isDirty());

    editor.people.setValue(Collections.singletonList(person));
    assertTrue(driver.isDirty());
  }

  Person person;
  Address personAddress;
  Person manager;
  long now;

  @Override
  protected void gwtSetUp() throws Exception {
    personAddress = new Address();
    personAddress.city = "City";
    personAddress.street = "Street";

    manager = new Person();
    manager.name = "Bill";

    person = new Person();
    person.address = personAddress;
    person.name = "Alice";
    person.manager = manager;
    person.localTime = now = System.currentTimeMillis();
  }
}
