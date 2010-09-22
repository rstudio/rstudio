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
import com.google.gwt.requestfactory.shared.Request;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.Violation;

import java.util.List;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
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
 * @param <P> the type of Proxy being edited
 * @param <E> the type of Editor that will edit the Record
 * @see com.google.gwt.requestfactory.client.testing.MockRequestFactoryEditorDriver
 */
public interface RequestFactoryEditorDriver<P, E extends Editor<? super P>> {
  /**
   * Initialize the Editor and its sub-editors with data for display-only mode.
   */
  void display(P proxy);

  /**
   * Initialize the Editor and its sub-editors with data.
   */
  void edit(P proxy, Request<?> saveRequest);

  /**
   * Ensures that the Editor passed into {@link #initialize} and its
   * sub-editors, if any, have synced their UI state by invoking flushing them
   * in a depth-first manner.
   * 
   * @return the Request passed into {@link #edit}
   * @throws IllegalStateException if {@link #edit(Object, Request)} has
   *           not been called with a non-null {@link Request}
   */
  <T> Request<T> flush();

  /**
   * Returns any unconsumed EditorErrors from the last call to {@link #flush()}.
   */
  List<EditorError> getErrors();

  /**
   * Returns a new array.
   */
  String[] getPaths();

  /**
   * Indicates if the last call to {@link #flush()} resulted in any errors.
   */
  boolean hasErrors();

  /**
   * Overload of {@link #initialize(RequestFactory, Editor)} to allow
   * a modified {@link EventBus} to be used.
   * 
   * @see {@link com.google.gwt.event.shared.ResettableEventBus}
   */
  void initialize(EventBus eventBus, RequestFactory requestFactory, E editor);
  
  /**
   * This or {@link #initialize(EventBus, RequestFactory, Editor)} must be
   * called before {@link #edit(Object, Request)}.
   */
  void initialize(RequestFactory requestFactory, E editor);

  /**
   * Show Violations returned from an attempt to submit a request. The
   * violations will be converted into {@link EditorError} objects whose
   * {@link EditorError#getUserData() getUserData()} method can be used to
   * access the original Violation object.
   * 
   * @return <code>true</code> if there were any unconsumed EditorErrors which
   *         can be retrieved from {@link #getErrors()}
   */
  boolean setViolations(Iterable<Violation> errors);
}
