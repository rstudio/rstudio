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
package com.google.web.bindery.requestfactory.gwt.rebind;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.editor.rebind.AbstractEditorDriverGenerator;
import com.google.gwt.editor.rebind.model.EditorData;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryEditorDriver;
import com.google.web.bindery.requestfactory.gwt.client.impl.AbstractRequestFactoryEditorDriver;
import com.google.web.bindery.requestfactory.gwt.client.impl.RequestFactoryEditorDelegate;
import com.google.web.bindery.requestfactory.shared.BaseProxy;

/**
 * Generates implementations of RFEDs.
 */
public class RequestFactoryEditorDriverGenerator extends AbstractEditorDriverGenerator {

  private JClassType baseProxyType;

  @Override
  public String generate(TreeLogger logger, GeneratorContext context, String typeName)
      throws UnableToCompleteException {
    baseProxyType = context.getTypeOracle().findType(BaseProxy.class.getCanonicalName());
    return super.generate(logger, context, typeName);
  }

  @Override
  protected Class<?> getDriverInterfaceType() {
    return RequestFactoryEditorDriver.class;
  }

  @Override
  protected Class<?> getDriverSuperclassType() {
    return AbstractRequestFactoryEditorDriver.class;
  }

  @Override
  protected Class<?> getEditorDelegateType() {
    return RequestFactoryEditorDelegate.class;
  }

  @Override
  protected String mutableObjectExpression(EditorData data, String sourceObjectExpression) {
    if (baseProxyType.isAssignableFrom(data.getPropertyOwnerType())) {
      return String.format("((%s) request.edit((%s)))", data.getPropertyOwnerType()
          .getQualifiedSourceName(), sourceObjectExpression);
    } else {
      return sourceObjectExpression;
    }
  }
}
