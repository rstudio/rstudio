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
package com.google.web.bindery.requestfactory.gwt.client;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorDriver;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.RequestFactory;

/**
 * The interface that links RequestFactory and the Editor framework together.
 * <p>
 * Instances of this interface are created with
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
 * @see HasRequestContext
 * @see com.google.web.bindery.requestfactory.gwt.client.testing.MockRequestFactoryEditorDriver
 *      MockRequestFactoryEditorDriver
 */
public interface RequestFactoryEditorDriver<P, E extends Editor<? super P>> extends
    EditorDriver<RequestContext> {
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
   * Update the object being edited with the current state of the Editor.
   * 
   * @return the RequestContext passed into
   *         {@link #edit(Object, RequestContext)}
   * @throws IllegalStateException if {@link #edit(Object, RequestContext)} has
   *           not been called
   */
  RequestContext flush();

  /**
   * Returns a new array containing the request paths.
   * 
   * @return an array of Strings
   */
  String[] getPaths();

  /**
   * Initializes a driver that will not be able to support subscriptions. Calls
   * to {@link com.google.gwt.editor.client.EditorDelegate#subscribe()} will do
   * nothing.
   * 
   * @param editor an {@link Editor} of type E
   */
  void initialize(E editor);

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
   * Show Violations returned from an attempt to submit a request. The
   * violations will be converted into
   * {@link com.google.gwt.editor.client.EditorError EditorError} objects whose
   * {@link com.google.gwt.editor.client.EditorError#getUserData()
   * getUserData()} method can be used to access the original Violation object.
   * 
   * @param violations an Iterable over
   *          {@link com.google.web.bindery.requestfactory.shared.Violation}
   *          instances
   * @return <code>true</code> if there were any unconsumed EditorErrors which
   *         can be retrieved from {@link #getErrors()}
   * @deprecated Users should switch to
   *             {@link #setConstraintViolations(Iterable)}
   */
  @Deprecated
  boolean setViolations(Iterable<com.google.web.bindery.requestfactory.shared.Violation> violations);
}
