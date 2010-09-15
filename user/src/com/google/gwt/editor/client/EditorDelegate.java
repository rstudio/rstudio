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

import com.google.gwt.event.shared.HandlerRegistration;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be changed. Use it at your own risk.
 * </span>
 * </p>
 * Binds an individual Editor to the backing service. Every Editor has a peer
 * EditorDelegate. If an Editor implements the {@link ValueAwareEditor}
 * interface, the EditorDriver will make the delegate available through the
 * {@link ValueAwareEditor#setDelegate} method.
 * 
 * @param <T> the type of object the delegate can accept
 * @see com.google.gwt.editor.client.testing.MockEditorDelegate
 */
public interface EditorDelegate<T> {
  /**
   * If a {@link ValueAwareEditor} chooses to modify the object passed into
   * {@link ValueAwareEditor#setValue} directly, as opposed to manipulating its
   * sub-Editors, the Editor must only call setter methods on the object
   * returned from this method. This allows the backing service to optimize the
   * read-only use case.
   */
  T ensureMutable(T object);

  /**
   * Returns the Editor's path, relative to the root object.
   */
  String getPath();

  /**
   * This method should be called from {@link ValueAwareEditor#flush()} or
   * {@link LeafValueEditor#getValue()} to record an error that will be reported
   * to the nearest super-Editor that implements the {@link HasEditorErrors}
   * interface.
   * 
   * @param message a textual description of the error
   * @param value the value to be returned by {@link EditorError#getValue()} or
   *          <code>null</code> if the value currently associated with the
   *          Editor should be used
   * @param userData an arbitrary object, possibly <code>null</code>, that can
   *          be retrieved with {@link EditorError#getUserData()}
   */
  void recordError(String message, Object value, Object userData);

  /**
   * Register for notifications if object being edited is updated. Not all
   * backends support subscriptions and will return <code>null</code>.
   * <p>
   * The notification will occur via {@link ValueAwareEditor#onPropertyChange}
   * if the backend supports in-place property updates, otherwise updates will
   * be passed via {@link ValueAwareEditor#setValue}.
   * 
   * @return a HandlerRegistration to unsubscribe from the notifications or
   *         <code>null</code> if the delegate does not support subscription
   */
  HandlerRegistration subscribe();
}
