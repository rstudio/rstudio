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
package com.google.gwt.editor.client.testing;

import com.google.gwt.editor.client.CompositeEditor;

import java.util.HashMap;
import java.util.Map;

/**
 * A Mock implementation of
 * {@link com.google.gwt.editor.client.CompositeEditor.EditorChain
 * CompositeEditor.EditorChain}.
 * 
 * @param <C> the type being edited
 */
public class MockEditorChain<C> implements
    CompositeEditor.EditorChain<C, FakeLeafValueEditor<C>> {
  private Map<FakeLeafValueEditor<C>, Boolean> attached = new HashMap<FakeLeafValueEditor<C>, Boolean>();

  public void attach(C object, FakeLeafValueEditor<C> subEditor) {
    subEditor.setValue(object);
    attached.put(subEditor, true);
  }

  public void detach(FakeLeafValueEditor<C> subEditor) {
    subEditor.setValue(null);
    attached.put(subEditor, false);
  }

  public C getValue(FakeLeafValueEditor<C> subEditor) {
    return subEditor.getValue();
  }

  public boolean isAttached(FakeLeafValueEditor<C> subEditor) {
    return attached.containsKey(subEditor) && attached.get(subEditor);
  }
}
