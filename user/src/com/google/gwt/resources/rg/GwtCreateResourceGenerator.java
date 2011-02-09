/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.resources.rg;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.resources.client.GwtCreateResource.ClassType;
import com.google.gwt.resources.ext.AbstractResourceGenerator;
import com.google.gwt.resources.ext.ResourceContext;
import com.google.gwt.resources.ext.SupportsGeneratorResultCaching;

/**
 * Provides implementations of GwtCreateResource.
 */
public final class GwtCreateResourceGenerator extends AbstractResourceGenerator 
    implements SupportsGeneratorResultCaching {

  @Override
  public String createAssignment(TreeLogger logger, ResourceContext context,
      JMethod method) throws UnableToCompleteException {
    JParameterizedType returnType = method.getReturnType().isParameterized();
    assert returnType != null;

    JClassType args[] = returnType.getTypeArgs();
    assert args.length == 1;

    ClassType override = method.getAnnotation(ClassType.class);
    JClassType toCreate;
    if (override != null) {
      toCreate = context.getGeneratorContext().getTypeOracle().findType(
          override.value().getName().replace('$', '.'));
      assert toCreate != null;
    } else {
      toCreate = args[0];
    }

    JClassType gwtType = context.getGeneratorContext().getTypeOracle().findType(
        GWT.class.getName());
    assert gwtType != null;

    return "new " + returnType.getParameterizedQualifiedSourceName()
        + "() {\n public " + toCreate.getQualifiedSourceName()
        + " create() {\n return " + gwtType.getQualifiedSourceName()
        + ".create(" + toCreate.getQualifiedSourceName() + ".class);}\n"
        + "public String getName() { return \"" + method.getName() + "\";}}";
  }
}
