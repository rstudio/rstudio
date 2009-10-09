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
package com.google.gwt.uibinder.rebind.model;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.resources.css.ExtractClassNamesVisitor;
import com.google.gwt.resources.css.GenerateCssAst;
import com.google.gwt.resources.css.ast.CssStylesheet;
import com.google.gwt.resources.ext.ResourceGeneratorUtil;
import com.google.gwt.uibinder.rebind.MortalLogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

/**
 * Models a method returning a CssResource on a generated ClientBundle.
 */
public class ImplicitCssResource {
  private final String packageName;
  private final String className;
  private final String name;
  private final String source;
  private final JClassType extendedInterface;
  private final String body;
  private final MortalLogger logger;
  private File generatedFile;

  public ImplicitCssResource(String packageName, String className, String name,
      String source, JClassType extendedInterface, String body,
      MortalLogger logger) {
    this.packageName = packageName;
    this.className = className;
    this.name = name;
    this.extendedInterface = extendedInterface;
    this.body = body;
    this.logger = logger;

    if (body.length() > 0) {
      assert "".equals(source); // Enforced for real by the parser

      /*
       * We're going to write the inline body to a temporary File and register
       * it with the CssResource world under the name in this.source, via
       * ResourceGeneratorUtil.addNamedFile(). When CssResourceGenerator sees
       * this name in an @Source annotation it will know to use the registered
       * file rather than load a resource.
       */
      source = String.format("uibinder:%s.%s.css", packageName, className);
    }
    this.source = source;
  }

  /**
   * @return the name of the CssResource interface
   */
  public String getClassName() {
    return className;
  }

  /**
   * @return the set of CSS classnames in the underlying .css files
   *
   * @throws UnableToCompleteException if the user has called for a .css file we
   *           can't find.
   */
  public Set<String> getCssClassNames() throws UnableToCompleteException {
    URL[] urls;

    if (body.length() == 0) {
      urls = getExternalCss();
    } else {
      try {
        urls = new URL[] {getGeneratedFile().toURL()};
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }

    CssStylesheet sheet = GenerateCssAst.exec(logger.getTreeLogger(), urls);
    return ExtractClassNamesVisitor.exec(sheet);
  }

  /**
   * @return the public interface that this CssResource implements
   */
  public JClassType getExtendedInterface() {
    return extendedInterface;
  }

  /**
   * @return the name of this resource. This is both its method name in the
   *         owning {@link ImplicitClientBundle} and its ui:field name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the package in which the generated CssResource interface should
   *         reside
   */
  public String getPackageName() {
    return packageName;
  }

  /**
   * @return the name of the .css file(s), separate by white space
   */
  public String getSource() {
    return source;
  }

  private URL[] getExternalCss() throws UnableToCompleteException {
    /*
     * TODO(rjrjr,bobv) refactor ResourceGeneratorUtil.findResources so we can
     * find them the same way ClientBundle does. For now, just look relative to
     * this package
     */

    ClassLoader classLoader = ImplicitCssResource.class.getClassLoader();
    String path = packageName.replace(".", "/");

    String[] sources = source.split(" ");
    URL[] urls = new URL[sources.length];
    int i = 0;

    for (String s : sources) {
      String resourcePath = path + '/' + s;
      URL found = classLoader.getResource(resourcePath);
      if (null == found) {
        logger.die("Unable to find resource: " + resourcePath);
      }
      urls[i++] = found;
    }
    return urls;
  }

  private File getGeneratedFile() {
    if (generatedFile == null) {
      try {
        File f = File.createTempFile(String.format("uiBinder_%s_%s",
            packageName, className), ".css");

        BufferedWriter out = new BufferedWriter(new FileWriter(f));
        out.write(body);
        out.close();
        generatedFile = f;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      ResourceGeneratorUtil.addNamedFile(getSource(), generatedFile);
    }
    return generatedFile;
  }
}
