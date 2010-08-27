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

import static com.google.gwt.sample.dynatablerf.client.StringConstants.CITY;
import static com.google.gwt.sample.dynatablerf.client.StringConstants.STATE;
import static com.google.gwt.sample.dynatablerf.client.StringConstants.STREET;
import static com.google.gwt.sample.dynatablerf.client.StringConstants.ZIP;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.PrimitiveValueEditor;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestObject;
import com.google.gwt.requestfactory.shared.SyncResult;
import com.google.gwt.sample.dynatablerf.shared.AddressProxy;
import com.google.gwt.sample.dynatablerf.shared.AddressProxyChanged;
import com.google.gwt.sample.dynatablerf.shared.DynaTableRequestFactory;

import java.util.Set;

import javax.validation.ConstraintViolation;

/**
 * A generated EditorDelegate that can be used in a composite fashion.
 */
class AddressEditorDelegate extends
    AbstractRequestFactoryDriver<DynaTableRequestFactory, AddressProxy> {

  @Override
  public void flush() {
    super.flush();

    {
      @SuppressWarnings("unchecked")
      PrimitiveValueEditor<String> valueEditor = (PrimitiveValueEditor<String>) editor.getEditorForPath(STREET);
      if (valueEditor != null) {
        object.setStreet(valueEditor.getValue());
      }
    }
    {
      @SuppressWarnings("unchecked")
      PrimitiveValueEditor<String> valueEditor = (PrimitiveValueEditor<String>) editor.getEditorForPath(CITY);
      if (valueEditor != null) {
        object.setCity(valueEditor.getValue());
      }
    }
    {
      @SuppressWarnings("unchecked")
      PrimitiveValueEditor<String> valueEditor = (PrimitiveValueEditor<String>) editor.getEditorForPath(STATE);
      if (valueEditor != null) {
        object.setState(valueEditor.getValue());
      }
    }
    {
      @SuppressWarnings("unchecked")
      PrimitiveValueEditor<Integer> valueEditor = (PrimitiveValueEditor<Integer>) editor.getEditorForPath(ZIP);
      if (valueEditor != null) {
        object.setZip(valueEditor.getValue());
      }
    }
  }

  @Override
  public void initialize(EventBus eventBus, DynaTableRequestFactory factory,
      String pathSoFar, AddressProxy object, Editor<AddressProxy> editor,
      RequestObject<?> editRequest) {
    super.initialize(eventBus, factory, pathSoFar, object, editor, editRequest);
    pushValues(object);
  }

  @Override
  public HandlerRegistration subscribe() {
    if (eventBus == null) {
      return null;
    }

    return eventBus.addHandler(AddressProxyChanged.TYPE,
        new AddressProxyChanged.Handler() {
          public void onAddressChanged(AddressProxyChanged event) {
            // TODO: This can't work with newly-created objects
            if (object.getId().equals(event.getRecord().getId())) {
              factory.addressRequest().findAddress(object.getId()).fire(
                  new Receiver<AddressProxy>() {
                    public void onSuccess(AddressProxy response,
                        Set<SyncResult> syncResults) {
                      pushValues(response);
                    }
                  });
            }
          }
        });
  }

  @Override
  public Set<ConstraintViolation<AddressProxy>> validate(AddressProxy object) {
    return null;
  }

  private void pushValues(AddressProxy object) {
    {
      @SuppressWarnings("unchecked")
      PrimitiveValueEditor<String> valueEditor = (PrimitiveValueEditor<String>) editor.getEditorForPath(STREET);
      if (valueEditor != null) {
        valueEditor.setValue(object.getStreet());
        // EditorDelegate for primitive values?
      }
    }
    {
      @SuppressWarnings("unchecked")
      PrimitiveValueEditor<String> valueEditor = (PrimitiveValueEditor<String>) editor.getEditorForPath(CITY);
      if (valueEditor != null) {
        valueEditor.setValue(object.getCity());
        // EditorDelegate for primitive values?
      }
    }
    {
      @SuppressWarnings("unchecked")
      PrimitiveValueEditor<String> valueEditor = (PrimitiveValueEditor<String>) editor.getEditorForPath(STATE);
      if (valueEditor != null) {
        valueEditor.setValue(object.getState());
        // EditorDelegate for primitive values?
      }
    }
    {
      @SuppressWarnings("unchecked")
      PrimitiveValueEditor<Integer> valueEditor = (PrimitiveValueEditor<Integer>) editor.getEditorForPath(ZIP);
      if (valueEditor != null) {
        valueEditor.setValue(object.getZip());
        // EditorDelegate for primitive values?
      }
    }
  }
}