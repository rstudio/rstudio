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

import java.util.List;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be changed. Use it at your own risk.
 * </span>
 * </p>
 * Automates editing of simple bean-like objects. The {@link EditorDelegate}
 * provided from this driver has a no-op implementation of
 * {@link EditorDelegate#subscribe()}.
 * 
 * <pre>
 * interface MyDriver extends SimpleBeanEditorDriver&lt;MyObject, MyObjectEditor> {}
 * MyDriver instance = GWT.create(MyDriver.class);
 * {
 * MyObjectEditor editor = new MyObjectEditor();
 * instance.initialize(editor);
 * // Do stuff 
 * instance.edit(myObjectInstance);
 * // Do more stuff 
 * instance.flush();
 * }
 * </pre>
 * 
 * @param <T> the type being edited
 * @param <E> the Editor for the type
 * @see com.google.gwt.editor.client.testing.MockSimpleBeanEditorDriver
 */
public interface SimpleBeanEditorDriver<T, E extends Editor<? super T>> {
  /**
   * Push the data in an object graph into the Editor given to
   * {@link #initialize}.
   * 
   * @param object the object providing input data
   * @throws IllegalStateException if {@link #initialize} has not been called
   */
  void edit(T object);

  /**
   * Update the object being edited with the current state of the Editor.
   * 
   * @return the object passed into {@link #edit}
   * @throws IllegalStateException if {@link #edit} has not been called
   */
  T flush();

  /**
   * Returns any unconsumed EditorErrors from the last call to {@link #flush()}.
   */
  List<EditorError> getErrors();

  /**
   * Indicates if the last call to {@link #flush()} resulted in any errors.
   */
  boolean hasErrors();

  /**
   * Initialize the editor driver.
   * 
   * @param editor the Editor to populate
   */
  void initialize(E editor);
}
