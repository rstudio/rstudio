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
package com.google.gwt.sample.dynatablerf.client.gen;

import static com.google.gwt.sample.dynatablerf.client.StringConstants.ADDRESS;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.RequestFactoryEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.shared.RequestObject;
import com.google.gwt.sample.dynatablerf.shared.DynaTableRequestFactory;
import com.google.gwt.sample.dynatablerf.shared.PersonProxy;

import java.util.ArrayList;
import java.util.List;

/**
 * Generated only for root objects that we want to edit, leaving access to its
 * composite objects as implementation details of the generated type. This is
 * the object would be returned by a call to GWT.create() somewhere.
 */
public class PersonRequestFactoryDriver implements
    RequestFactoryEditorDriver<DynaTableRequestFactory, PersonProxy> {

  private PersonEditorDelegate delegate;
  private Editor<PersonProxy> editor;
  private EventBus eventBus;
  private DynaTableRequestFactory factory;
  private String[] paths;
  private RequestObject<?> saveRequest;

  public void edit(PersonProxy object, RequestObject<?> saveRequest) {
    checkEditor();
    this.saveRequest = saveRequest;
    delegate = new PersonEditorDelegate();
    delegate.initialize(eventBus, factory, "", object, editor, saveRequest);
  }

  @SuppressWarnings("unchecked")
  public <T> RequestObject<T> flush() {
    checkDelegate();
    delegate.flush();
    return (RequestObject<T>) saveRequest;
  }

  public String[] getPaths() {
    return paths;
  }

  public void initialize(EventBus eventBus, DynaTableRequestFactory factory,
      Editor<PersonProxy> editor) {
    this.eventBus = eventBus;
    this.factory = factory;
    this.editor = editor;
    paths = traverseAndSubscribePaths();
  }

  private void checkDelegate() {
    if (delegate == null) {
      throw new IllegalStateException("Must call edit() first");
    }
  }

  private void checkEditor() {
    if (editor == null) {
      throw new IllegalStateException("Must call initialize() first");
    }
  }

  /**
   * Traverse paths known from Record property structure and see which ones the
   * editor cares about.
   */
  private String[] traverseAndSubscribePaths() {
    List<String> toReturn = new ArrayList<String>();
    /*
     * GENERATE check and with() call for all known properties and
     * sub-properties. Retain Editor.getPaths() and use for a quick
     * Set.retainAll() narrowing?
     */
    if (editor.getEditorForPath(ADDRESS) != null) {
      toReturn.add(ADDRESS);
      // GENERATE Need to descend into sub-editor
    }
    // More checks.
    return toReturn.toArray(new String[toReturn.size()]);
  }
}