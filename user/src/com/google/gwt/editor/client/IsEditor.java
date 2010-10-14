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
 * that do not implement the {@link Editor} contract directly. The primary
 * advantage of the IsEditor interface is that is allows composition of behavior
 * without the need to implement delegate methods for every interface
 * implemented by the common editor logic.
 * <p>
 * For example, an editor Widget that supports adding and removing elements from
 * a list might wish to re-use the provided
 * {@link com.google.gwt.editor.client.adapters.ListEditor ListEditor}
 * controller. It might be roughly built as:
 * 
 * <pre>
 * class MyListEditor extends Composite implements IsEditor&lt;ListEditor&lt;Foo, FooEditor>> {
 *   private ListEditor&lt;Foo, FooEditor> controller = ListEditor.of(new FooEditorSource());
 *   public ListEditor&lt;Foo, FooEditor> asEditor() {return controller;}
 *   void onAddButtonClicked() { controller.getList().add(new Foo()); }
 *   void onClearButtonClicked() { controller.getList().clear(); }
 * }
 * </pre>
 * By implementing only the one <code>asEditor()</code> method, the
 * <code>MyListEditor</code> type is able to incorporate the
 * <code>ListEditor</code> behavior without needing to write delegate methods
 * for every method in <code>ListEditor</code>.
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
   * 
   * @return an {@link Editor} of type E
   */
  E asEditor();
}
