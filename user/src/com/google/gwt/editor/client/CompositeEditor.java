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
 * An interface that indicates that a given Editor is composed of an unknown
 * number of sub-Editors all of the same type.
 * <p>
 * For example, the {@link com.google.gwt.editor.client.adapters.ListEditor
 * ListEditor} type is a
 * <code>CompositeEditor&lt;List&lt;T>, T, E extends Editor&lt;T>></code>; that
 * is, ListEditor will accept a List&lt;T> and will edit some unknown number of
 * <code>T</code>'s using the Editor type <code>E</code>. Another example might
 * be:
 * 
 * <pre>
 * class WorkgroupEditor implements CompositeEditor&lt;Workgroup, Person, PersonSummaryEditor>{
 *   public void setValue(Workgroup workgroup) {
 *     // Assuming Workgroup implements Iterable&lt;Person>
 *     for (Person p : workgroup) {
 *       PersonSummaryEditor editor = new PersonSummaryEditor();
 *       // Attach editor to DOM
 *       somePanel.add(editor);
 *       // Let the generated code drive the sub-editor
 *       editorChain.attach(p, editor);
 *     }
 *   }
 * }
 * </pre>
 * 
 * @param <T> the base type being edited
 * @param <C> the component type to be edited
 * @param <E> the type of Editor that will edit the component type
 */
public interface CompositeEditor<T, C, E extends Editor<C>> extends
    ValueAwareEditor<T> {
  /**
   * Allows instances of the component type to be attached to the Editor
   * framework.
   * 
   * @param <C> the type of object to be edited
   * @param <E> the type of Editor
   * @see com.google.gwt.editor.client.testing.MockEditorChain
   */
  public interface EditorChain<C, E extends Editor<C>> {
    /**
     * Editors attached to the chain will be automatically flushed as if they
     * were a statically-defined sub-Editor.
     * 
     * @param object the object to edit
     * @param subEditor the Editor to populate
     */
    void attach(C object, E subEditor);

    /**
     * Detach a sub-Editor from the editor chain.
     * 
     * @param subEditor an Editor previously passed into {@link #attach}
     */
    void detach(E subEditor);

    /**
     * Retrieves the value associated with the editor.
     * 
     * @param subEditor an Editor previously passed into {@link #attach}
     * @return the value associated with the editor
     */
    C getValue(E subEditor);
  }

  /**
   * Returns an canonical sub-editor instance that will be used by the driver
   * for computing all edited paths.
   *
   * @return an instance of the Editor type
   */
  E createEditorForTraversal();

  /**
   * Used to implement {@link EditorDelegate#getPath()} for the component
   * Editors.
   * 
   * @param subEditor an instance of the Editor type previously passed into
   *          {@link EditorChain#attach}
   * @return the path element as a String
   */
  String getPathElement(E subEditor);

  /**
   * Called by the Editor framework to provide the {@link EditorChain}.
   *
   * @param chain an {@link EditorChain} instance
   */
  void setEditorChain(EditorChain<C, E> chain);
}
