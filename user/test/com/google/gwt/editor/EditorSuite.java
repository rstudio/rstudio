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
package com.google.gwt.editor;

import com.google.gwt.editor.client.DirtyEditorTest;
import com.google.gwt.editor.client.EditorErrorTest;
import com.google.gwt.editor.client.SimpleBeanEditorTest;
import com.google.gwt.editor.client.adapters.HasDataEditorTest;
import com.google.gwt.editor.client.adapters.ListEditorWrapperTest;
import com.google.gwt.editor.client.impl.DelegateMapTest;
import com.google.gwt.editor.rebind.model.EditorModelTest;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * Tests of the Editor framework. These tests focus on core Editor behaviors,
 * rather than on integration with backing stores.
 */
public class EditorSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite(
        "Test suite for core Editor functions");
    suite.addTestSuite(DelegateMapTest.class);
    suite.addTestSuite(DirtyEditorTest.class);
    suite.addTestSuite(EditorModelTest.class);
    suite.addTestSuite(EditorErrorTest.class);
    suite.addTestSuite(HasDataEditorTest.class);
    suite.addTestSuite(ListEditorWrapperTest.class);
    suite.addTestSuite(SimpleBeanEditorTest.class);
    return suite;
  }
}
