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
package com.google.gwt.requestfactory.client;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.shared.RequestContext;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.Violation;

import java.util.List;

import javax.validation.ConstraintViolation;

/**
 * The interface that links RequestFactory and the Editor framework together.
 * Used for configuration and lifecycle control. Expected that this will be
 * created with
 * 
 * <pre>
 * interface MyRFED extends RequestFactoryEditorDriver&lt;MyObjectProxy, MyObjectEditor> {}
 * MyRFED instance = GWT.create(MyRFED.class);
 * {
 * instance.initialize(.....);
 * myRequest.with(instance.getPaths());
 * 
 * // Fire the request, in the callback
 * instance.edit(retrievedRecord);
 * // Control when the request is sent
 * instance.flush().fire(new Receiver {...});
 * }
 * </pre>
 * 
 * <p>
 * 
 * @param <P> the type of Proxy being edited
 * @param <E> the type of Editor that will edit the Record
 * @see {@link com.google.gwt.requestfactory.client.testing.MockRequestFactoryEditorDriver}
 */
public interface RequestFactoryEditorDriver<P, E extends Editor<? super P>> {
  /**
   * Start driving the Editor and its sub-editors with data for display-only
   * mode.
   * 
   * @param proxy a Proxy of type P
   */
  void display(P proxy);

  /**
   * Start driving the Editor and its sub-editors with data. A
   * {@link RequestContext} is required to provide context for the changes to
   * the proxy (see {@link RequestContext#edit}. Note that this driver will not
   * fire the request.
   * 
   * @param proxy the proxy to be edited
   * @param request the request context that will accumulate edits and is
   *          returned form {@link #flush}
   */
  void edit(P proxy, RequestContext request);

  /**
   * Ensures that the Editor passed into {@link #initialize} and its
   * sub-editors, if any, have synced their UI state by invoking flushing them
   * in a depth-first manner.
   * 
   * @return the RequestContext passed into {@link #edit}
   * @throws IllegalStateException if {@link #edit(Object, RequestContext)} has
   *           not been called with a non-null {@link RequestContext}
   */
  RequestContext flush();

  /**
   * Returns any unconsumed {@link EditorError EditorErrors} from the last call
   * to {@link #flush()}.
   * 
   * @return a List of {@link EditorError} instances
   */
  List<EditorError> getErrors();

  /**
   * Returns a new array containing the request paths.
   * 
   * @return an array of Strings
   */
  String[] getPaths();

  /**
   * Indicates if the last call to {@link #flush()} resulted in any errors.
   * 
   * @return {@code} true if errors are present
   */
  boolean hasErrors();

  /**
   * Overload of {@link #initialize(RequestFactory, Editor)} to allow a modified
   * {@link EventBus} to be monitored for subscription services.
   * 
   * @param eventBus the {@link EventBus}
   * @param requestFactory a {@link RequestFactory} instance
   * @param editor an {@link Editor} of type E
   * 
   * @see com.google.gwt.editor.client.EditorDelegate#subscribe
   * @see com.google.gwt.event.shared.ResettableEventBus
   */
  void initialize(EventBus eventBus, RequestFactory requestFactory, E editor);

  /**
   * Initializes a driver with the editor it will run, and a RequestFactory to
   * use for subscription services.
   * 
   * @param requestFactory a {@link RequestFactory} instance
   * @param editor an {@link Editor} of type E
   * 
   * @see com.google.gwt.editor.client.EditorDelegate#subscribe
   */
  void initialize(RequestFactory requestFactory, E editor);

  /**
   * Initializes a driver that will not be able to support subscriptions. Calls
   * to {@link com.google.gwt.editor.client.EditorDelegate#subscribe()} will do
   * nothing.
   * 
   * @param editor an {@link Editor} of type E
   */
  void initialize(E editor);

  /**
   * Returns {@code true} if any of the Editors in the hierarchy have been
   * modified relative to the last value passed into {@link #edit(Object)}.
   * 
   * @see com.google.gwt.editor.client.EditorDelegate#setDirty(boolean)
   */
  boolean isDirty();

  /**
   * Show {@link ConstraintViolation ConstraintViolations} generated through a
   * JSR 303 Validator. The violations will be converted into
   * {@link EditorError} objects whose {@link EditorError#getUserData()
   * getUserData()} method can be used to access the original
   * ConstraintViolation object.
   * 
   * @param violations an Iterable over {@link ConstraintViolation} instances
   * @return <code>true</code> if there were any unconsumed EditorErrors which
   *         can be retrieved from {@link #getErrors()}
   */
  boolean setConstraintViolations(Iterable<ConstraintViolation<?>> violations);

  /**
   * Show Violations returned from an attempt to submit a request. The
   * violations will be converted into {@link EditorError} objects whose
   * {@link EditorError#getUserData() getUserData()} method can be used to
   * access the original Violation object.
   * 
   * @param violations an Iterable over {@link Violation} instances
   * @return <code>true</code> if there were any unconsumed EditorErrors which
   *         can be retrieved from {@link #getErrors()}
   */
  boolean setViolations(Iterable<Violation> violations);
}
