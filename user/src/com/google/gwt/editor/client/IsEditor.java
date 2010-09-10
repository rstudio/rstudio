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

/**
 * Extended by view objects that wish to participate in an Editor hierarchy, but
 * that do not implement the {@link Editor} contract directly.
 * <p>
 * It is legal for a type to implement both Editor and IsEditor. In this case,
 * the Editor returned from {@link #asEditor()} will be a co-Editor of the
 * IsEditor instance.
 * 
 * @param <E> the type of Editor the view object will provide
 * @see CompositeEditor
 */
public interface IsEditor<E extends Editor<?>> {
  /**
   * Returns the Editor encapsulated by the view object.
   */
  E asEditor();
}
