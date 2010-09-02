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
package com.google.gwt.requestfactory.rebind;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.editor.rebind.AbstractEditorDriverGenerator;
import com.google.gwt.editor.rebind.model.EditorData;
import com.google.gwt.editor.rebind.model.EditorModel;
import com.google.gwt.requestfactory.client.RequestFactoryEditorDriver;
import com.google.gwt.requestfactory.client.impl.AbstractRequestFactoryEditorDriver;
import com.google.gwt.requestfactory.client.impl.RequestFactoryEditorDelegate;
import com.google.gwt.user.rebind.SourceWriter;

import java.util.List;

/**
 * Generates implementations of RFEDs.
 */
public class RequestFactoryEditorDriverGenerator extends
    AbstractEditorDriverGenerator {
  @Override
  protected Class<?> getDriverSuperclassType() {
    return AbstractRequestFactoryEditorDriver.class;
  }

  @Override
  protected Class<?> getDriverInterfaceType() {
    return RequestFactoryEditorDriver.class;
  }

  @Override
  protected Class<?> getEditorDelegateType() {
    return RequestFactoryEditorDelegate.class;
  }

  @Override
  protected String mutableObjectExpression(String sourceObjectExpression) {
    return String.format("request.edit(%s)", sourceObjectExpression);
  }

  @Override
  protected void writeAdditionalContent(TreeLogger logger,
      GeneratorContext context, EditorModel model, SourceWriter sw) {
    sw.println("protected void traverseEditors(%s<String> paths) {",
        List.class.getName());
    sw.println("  %s.traverseEditor(getEditor(), \"\", paths);",
        getEditorDelegate(model.getProxyType(), model.getEditorType()));
    sw.println("}");
  }

  @Override
  protected void writeDelegateInitialization(SourceWriter sw, EditorData d) {
    sw.println("%1$sDelegate.initialize(eventBus, factory, "
        + "appendPath(\"%1$s\"), getObject()%2$s.%3$s(),"
        + " editor.%4$s, request);", d.getPropertyName(),
        d.getBeanOwnerExpression(), d.getGetterName(), d.getSimpleExpression());
  }
}
