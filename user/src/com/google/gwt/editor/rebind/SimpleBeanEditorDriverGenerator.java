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
package com.google.gwt.editor.rebind;

import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.editor.client.impl.AbstractSimpleBeanEditorDriver;
import com.google.gwt.editor.client.impl.SimpleBeanEditorDelegate;
import com.google.gwt.editor.rebind.model.EditorData;
import com.google.gwt.user.rebind.SourceWriter;

/**
 * Generates implementations of {@link SimpleBeanEditorDriver}.
 */
public class SimpleBeanEditorDriverGenerator extends
    AbstractEditorDriverGenerator {

  @Override
  protected Class<?> getDriverInterfaceType() {
    return SimpleBeanEditorDriver.class;
  }

  @Override
  protected Class<?> getDriverSuperclassType() {
    return AbstractSimpleBeanEditorDriver.class;
  }

  @Override
  protected Class<?> getEditorDelegateType() {
    return SimpleBeanEditorDelegate.class;
  }

  @Override
  protected String mutableObjectExpression(String sourceObjectExpression) {
    return sourceObjectExpression;
  }

  @Override
  protected void writeDelegateInitialization(SourceWriter sw, EditorData d) {
    sw.println("%1$sDelegate.initialize(appendPath(\"%1$s\"),"
        + " getObject()%2$s.%3$s()," + " editor.%4$s);", d.getPropertyName(),
        d.getBeanOwnerExpression(), d.getGetterName(), d.getSimpleExpression());
  }
}
