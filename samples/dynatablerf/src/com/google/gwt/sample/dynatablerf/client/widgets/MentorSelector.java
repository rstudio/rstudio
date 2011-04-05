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
package com.google.gwt.sample.dynatablerf.client.widgets;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.client.adapters.OptionalFieldEditor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.sample.dynatablerf.shared.DynaTableRequestFactory;
import com.google.gwt.sample.dynatablerf.shared.PersonProxy;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.requestfactory.shared.Receiver;

/**
 * This demonstrates how an editor can be constructed to handle optional fields.
 * The Person domain object's mentor property is initially <code>null</code>.
 * This type delegates editing control to an instance of the
 * {@link OptionalValueEditor} adapter class.
 */
public class MentorSelector extends Composite implements
    IsEditor<OptionalFieldEditor<PersonProxy, NameLabel>> {

  interface Binder extends UiBinder<Widget, MentorSelector> {
  }

  @UiField
  Button choose;

  @UiField
  Button clear;

  @UiField
  NameLabel nameLabel;

  private final OptionalFieldEditor<PersonProxy, NameLabel> editor;
  private final DynaTableRequestFactory factory;

  public MentorSelector(DynaTableRequestFactory factory) {
    this.factory = factory;
    initWidget(GWT.<Binder> create(Binder.class).createAndBindUi(this));
    editor = OptionalFieldEditor.of(nameLabel);
  }

  public OptionalFieldEditor<PersonProxy, NameLabel> asEditor() {
    return editor;
  }

  public void setEnabled(boolean enabled) {
    choose.setEnabled(enabled);
    clear.setEnabled(enabled);
  }

  @Override
  protected void onUnload() {
    nameLabel.cancelSubscription();
  }

  @UiHandler("choose")
  void onChoose(ClickEvent event) {
    setEnabled(false);
    factory.schoolCalendarRequest().getRandomPerson().to(
        new Receiver<PersonProxy>() {
          @Override
          public void onSuccess(PersonProxy response) {
            setValue(response);
            setEnabled(true);
          }
        }).fire();
  }

  @UiHandler("clear")
  void onClear(ClickEvent event) {
    setValue(null);
  }

  /**
   * This method is not called by the Editor framework.
   */
  private void setValue(PersonProxy person) {
    editor.setValue(person);
    nameLabel.setVisible(person != null);
  }
}
