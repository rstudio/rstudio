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
import static com.google.gwt.sample.dynatablerf.client.StringConstants.DESCRIPTION;
import static com.google.gwt.sample.dynatablerf.client.StringConstants.NAME;
import static com.google.gwt.sample.dynatablerf.client.StringConstants.NOTE;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.PrimitiveValueEditor;
import com.google.gwt.editor.client.impl.RequestFactoryEditorDriverImpl;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestObject;
import com.google.gwt.requestfactory.shared.SyncResult;
import com.google.gwt.sample.dynatablerf.shared.AddressProxy;
import com.google.gwt.sample.dynatablerf.shared.DynaTableRequestFactory;
import com.google.gwt.sample.dynatablerf.shared.PersonProxy;
import com.google.gwt.sample.dynatablerf.shared.PersonProxyChanged;

import java.util.Set;

import javax.validation.ConstraintViolation;

/**
 * A generated EditorDelegate that can be used in a composite fashion.
 */
class PersonEditorDelegate extends
    AbstractRequestFactoryDriver<DynaTableRequestFactory, PersonProxy> {
  private RequestFactoryEditorDriverImpl<DynaTableRequestFactory, AddressProxy> addressDelegate;

  @Override
  public void flush() {
    // Depth-first flush
    if (addressDelegate != null) {
      addressDelegate.flush();
    }

    super.flush();

    {
      @SuppressWarnings("unchecked")
      PrimitiveValueEditor<String> nameEditor = (PrimitiveValueEditor<String>) editor.getEditorForPath(NAME);
      if (nameEditor != null) {
        object.setName(nameEditor.getValue());
      }
    }
    {
      @SuppressWarnings("unchecked")
      PrimitiveValueEditor<String> descriptionEditor = (PrimitiveValueEditor<String>) editor.getEditorForPath(DESCRIPTION);
      if (descriptionEditor != null) {
        object.setDescription(descriptionEditor.getValue());
      }
    }
    {
      @SuppressWarnings("unchecked")
      PrimitiveValueEditor<String> noteEditor = (PrimitiveValueEditor<String>) editor.getEditorForPath(NOTE);
      if (noteEditor != null) {
        object.setNote(noteEditor.getValue());
      }
    }
  }

  @Override
  public void initialize(EventBus eventBus, DynaTableRequestFactory factory,
      String pathSoFar, PersonProxy object, Editor<PersonProxy> editor,
      RequestObject<?> editRequest) {
    super.initialize(eventBus, factory, pathSoFar, object, editor, editRequest);

    // Attach sub-editors
    {
      @SuppressWarnings("unchecked")
      Editor<AddressProxy> addressEditor = (Editor<AddressProxy>) editor.getEditorForPath(ADDRESS);
      if (addressEditor != null) {
        addressDelegate = new AddressEditorDelegate();
        addressDelegate.initialize(eventBus, factory, appendPath(ADDRESS),
            object.getAddress(), addressEditor, editRequest);
      }
    }
    pushValues(object);
  }

  @Override
  public HandlerRegistration subscribe() {
    if (eventBus == null) {
      return null;
    }

    return eventBus.addHandler(PersonProxyChanged.TYPE,
        new PersonProxyChanged.Handler() {
          public void onPersonChanged(PersonProxyChanged event) {
            // TODO: This can't work with newly-created objects 
            if (object.getId().equals(event.getRecord().getId())) {
              factory.personRequest().findPerson(object.getId()).fire(
                  new Receiver<PersonProxy>() {
                    public void onSuccess(PersonProxy response,
                        Set<SyncResult> syncResults) {
                      pushValues(response);
                    }
                  });
            }
          }
        });
  }

  @Override
  public Set<ConstraintViolation<PersonProxy>> validate(PersonProxy object) {
    return null;
  }

  private void pushValues(PersonProxy object) {
    {
      @SuppressWarnings("unchecked")
      PrimitiveValueEditor<String> nameEditor = (PrimitiveValueEditor<String>) editor.getEditorForPath(NAME);
      if (nameEditor != null) {
        nameEditor.setValue(object.getName());
        // Delegate for trivial editors?
      }
    }
    {
      @SuppressWarnings("unchecked")
      PrimitiveValueEditor<String> descriptionEditor = (PrimitiveValueEditor<String>) editor.getEditorForPath(DESCRIPTION);
      if (descriptionEditor != null) {
        descriptionEditor.setValue(object.getDescription());
        // Delegate for trivial editors?
      }
    }
    {
      @SuppressWarnings("unchecked")
      PrimitiveValueEditor<String> noteEditor = (PrimitiveValueEditor<String>) editor.getEditorForPath(NOTE);
      if (noteEditor != null) {
        noteEditor.setValue(object.getNote());
        // Delegate for trivial editors?
      }
    }
  }
}