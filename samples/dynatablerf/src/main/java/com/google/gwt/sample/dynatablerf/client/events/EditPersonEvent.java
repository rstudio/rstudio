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
package com.google.gwt.sample.dynatablerf.client.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.sample.dynatablerf.shared.PersonProxy;
import com.google.web.bindery.requestfactory.shared.RequestContext;

/**
 * Begin editing a person.
 * 
 * TODO: Make this an Activity.
 */
public class EditPersonEvent extends GwtEvent<EditPersonEvent.Handler> {
  public static final Type<Handler> TYPE = new Type<Handler>();

  /**
   * Handles {@link EditPersonEvent}.
   */
  public interface Handler extends EventHandler {
    void startEdit(PersonProxy person, RequestContext requestContext);
  }

  private final PersonProxy person;
  private final RequestContext request;

  public EditPersonEvent(PersonProxy person) {
    this(person, null);
  }

  public EditPersonEvent(PersonProxy person, RequestContext request) {
    this.person = person;
    this.request = request;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.startEdit(person, request);
  }

  @Override
  public com.google.gwt.event.shared.GwtEvent.Type<Handler> getAssociatedType() {
    return TYPE;
  }
}
