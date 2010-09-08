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
package com.google.gwt.validation.rebind;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.user.rebind.AbstractSourceCreator;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;

/**
 * Abstract Class for Creating source files.
 * <p>
 * This class is not thread safe.
 */
public abstract class AbstractCreator extends AbstractSourceCreator {

  protected final GeneratorContext context;

  protected final TreeLogger logger;

  protected final JClassType validatorType;

  public AbstractCreator(GeneratorContext context, TreeLogger logger,
      JClassType validatorType) {
    super();
    this.context = context;
    this.logger = branch(logger, "Creating " + validatorType);
    this.validatorType = validatorType;
  }

  public final String create() {
    SourceWriter sourceWriter = getSourceWriter(logger, context);
    if (sourceWriter != null) {
      writeClassBody(sourceWriter);
      sourceWriter.commit(logger);
    }
    return getQaulifiedName();
  }

  protected abstract void compose(ClassSourceFileComposerFactory composerFactory);

  protected final String getPackage() {
    JPackage serviceIntfPkg = validatorType.getPackage();
    String packageName = serviceIntfPkg == null ? "" : serviceIntfPkg.getName();
    return packageName;
  }

  protected abstract void writeClassBody(SourceWriter sourceWriter);

  private final String getQaulifiedName() {
    String packageName = getPackage();
    return (packageName == "" ? "" : packageName + ".") + getSimpleName();
  }

  private final String getSimpleName() {
    return validatorType.getSimpleSourceName() + "Impl";
  }

  private final SourceWriter getSourceWriter(TreeLogger logger,
      GeneratorContext ctx) {
    String packageName = getPackage();
    String simpleName = getSimpleName();
    PrintWriter printWriter = ctx.tryCreate(logger, packageName, simpleName);
    if (printWriter == null) {
      return null;
    }

    ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(
        packageName, simpleName);
    compose(composerFactory);
    SourceWriter sourceWriter = composerFactory.createSourceWriter(ctx,
        printWriter);
    return sourceWriter;
  }
}