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
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.CssResource.Import;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.rebind.model.ImplicitClientBundle;
import com.google.gwt.uibinder.rebind.model.ImplicitCssResource;
import com.google.gwt.uibinder.rebind.model.ImplicitDataResource;
import com.google.gwt.uibinder.rebind.model.ImplicitImageResource;

import java.util.Collection;
import java.util.Set;

/**
 * Writes source implementing an {@link ImplicitClientBundle}.
 */
public class BundleWriter {

  private final ImplicitClientBundle bundleClass;
  private final IndentedWriter writer;
  private final PrintWriterManager writerManager;
  private final TypeOracle types;
  private final MortalLogger logger;

  private final JClassType clientBundleType;
  private final JClassType dataResourceType;
  private final JClassType imageOptionType;
  private final JClassType imageResourceType;
  private final JClassType repeatStyleType;
  private final JClassType importAnnotationType;

  public BundleWriter(ImplicitClientBundle bundleClass,
      PrintWriterManager writerManager, TypeOracle types, MortalLogger logger) {
    this.bundleClass = bundleClass;
    this.writer = new IndentedWriter(
        writerManager.makePrintWriterFor(bundleClass.getClassName()));
    this.writerManager = writerManager;
    this.types = types;
    this.logger = logger;

    clientBundleType = types.findType(ClientBundle.class.getName());
    dataResourceType = types.findType(DataResource.class.getCanonicalName());
    imageOptionType = types.findType(ImageOptions.class.getCanonicalName());
    imageResourceType = types.findType(ImageResource.class.getCanonicalName());
    repeatStyleType = types.findType(RepeatStyle.class.getCanonicalName());
    importAnnotationType = types.findType(Import.class.getCanonicalName());
  }

  public void write() throws UnableToCompleteException {
    writeBundleClass();
    for (ImplicitCssResource css : bundleClass.getCssMethods()) {
      new CssResourceWriter(css, types,
          writerManager.makePrintWriterFor(css.getClassName()),
          logger).write();
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
    writer.write("import %s;", clientBundleType.getQualifiedSourceName());
    writer.write("import %s;", dataResourceType.getQualifiedSourceName());
    writer.write("import %s;", imageResourceType.getQualifiedSourceName());
    writer.write("import %s;", imageOptionType.getQualifiedSourceName());
    writer.write("import %s;", importAnnotationType.getQualifiedSourceName());
    writer.newline();

    // Open interface
    writer.write("public interface %s extends ClientBundle {",
        bundleClass.getClassName());
    writer.indent();

    // Write css methods
    for (ImplicitCssResource css : bundleClass.getCssMethods()) {
      writeCssSource(css);
      writeCssImports(css);
      writer.write("%s %s();", css.getClassName(), css.getName());
      writer.newline();
    }

    // Write data methods
    for (ImplicitDataResource data : bundleClass.getDataMethods()) {
      writer.write("@Source(\"%s\")", data.getSource());
      writer.write("%s %s();", dataResourceType.getName(), data.getName());
      writer.newline();
    }

    writeImageMethods();

    // Close interface.
    writer.outdent();
    writer.write("}");
  }

  private void writeCssImports(ImplicitCssResource css) {
    Set<JClassType> importTypes = css.getImports();
    int numImports = importTypes.size();
    if (numImports > 0) {
      if (numImports == 1) {
        writer.write("@Import(%s.class)",
            importTypes.iterator().next().getQualifiedSourceName());
      } else {
        StringBuffer b = new StringBuffer();
        for (JClassType importType : importTypes) {
          if (b.length() > 0) {
            b.append(", ");
          }
          b.append(importType.getQualifiedSourceName()).append(".class");
        }
        writer.write("@Import({%s})", b);
      }
    }
  }

  private void writeCssSource(ImplicitCssResource css) {
    Collection<String> sources = css.getSource();
    if (sources.size() == 1) {
      writer.write("@Source(\"%s\")", sources.iterator().next());
    } else {
      StringBuffer b = new StringBuffer();
      for (String s : sources) {
        if (b.length() > 0) {
          b.append(", ");
        }
        b.append('"').append(s).append('"');
      }
      writer.write("@Source({%s})", b);
    }
  }

  private void writeImageMethods() {
    for (ImplicitImageResource image : bundleClass.getImageMethods()) {
      if (null != image.getSource()) {
        writer.write("@Source(\"%s\")", image.getSource());
      }

      writeImageOptionsAnnotation(image.getFlipRtl(), image.getRepeatStyle());
      writer.write("%s %s();", imageResourceType.getName(), image.getName());
    }
  }

  private void writeImageOptionsAnnotation(Boolean flipRtl,
      RepeatStyle repeatStyle) {
    if (flipRtl != null || repeatStyle != null) {
      StringBuilder b = new StringBuilder("@ImageOptions(");
      if (null != flipRtl) {
        b.append("flipRtl=").append(flipRtl);
        if (repeatStyle != null) {
          b.append(", ");
        }
      }
      if (repeatStyle != null) {
        b.append(String.format("repeatStyle=%s.%s", repeatStyleType.getName(),
            repeatStyle.toString()));
      }
      b.append(")");
      writer.write(b.toString());
    }
  }
}
