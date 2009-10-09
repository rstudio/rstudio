/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource.Strict;
import com.google.gwt.uibinder.rebind.model.ImplicitClientBundle;
import com.google.gwt.uibinder.rebind.model.ImplicitCssResource;

/**
 * Writes source implementing an {@link ImplicitClientBundle}.
 */
public class BundleWriter {

  private final ImplicitClientBundle bundleClass;
  private final IndentedWriter writer;
  private final PrintWriterManager writerManager;
  private final TypeOracle oracle;
  private final String clientBundleType;
  private final String strictAnnotationType;

  public BundleWriter(ImplicitClientBundle bundleClass,
      PrintWriterManager writerManager, TypeOracle oracle,
      PrintWriterManager printWriterProvider) {
    this.bundleClass = bundleClass;
    this.writer = new IndentedWriter(
        writerManager.makePrintWriterFor(bundleClass.getClassName()));
    this.writerManager = writerManager;
    this.oracle = oracle;

     clientBundleType = oracle.findType(ClientBundle.class.getName()).getQualifiedSourceName();
     strictAnnotationType = oracle.findType(Strict.class.getCanonicalName()).getQualifiedSourceName();
  }

  public void write() throws UnableToCompleteException {
    writeBundleClass();
    for (ImplicitCssResource css : bundleClass.getCssMethods()) {
      new CssResourceWriter(css, oracle,
          writerManager.makePrintWriterFor(css.getClassName())).write();
    }
  }

  private void writeBundleClass() {
    // Package declaration
    String packageName = bundleClass.getPackageName();
    if (packageName.length() > 0) {
      writer.write("package %1$s;", packageName);
      writer.newline();
    }

    // Imports
    writer.write("import %s;",
        clientBundleType);
    writer.write("import %s;", strictAnnotationType);
    writer.newline();

    // Open interface
    writer.write("public interface %s extends ClientBundle {",
        bundleClass.getClassName());
    writer.indent();

    // Write css methods
    for (ImplicitCssResource css : bundleClass.getCssMethods()) {
      writer.write("@Strict @Source(\"%s\")", css.getSource());
      writer.write("%s %s();", css.getClassName(), css.getName());
      writer.newline();
    }

    // Close interface.
    writer.outdent();
    writer.write("}");
  }
}
