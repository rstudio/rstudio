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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Models a method returning a CssResource on a generated ClientBundle.
 */
public class ImplicitCssResource {
  private final String packageName;
  private final String className;
  private final String name;
  private final List<String> sources;
  private final JClassType extendedInterface;
  private final String body;
  private final MortalLogger logger;
  private final Set<JClassType> imports;

  private File generatedFile;

  ImplicitCssResource(String packageName, String className, String name,
      String[] source, JClassType extendedInterface, String body,
      MortalLogger logger, HashSet<JClassType> importTypes) {
    this.packageName = packageName;
    this.className = className;
    this.name = name;
    this.extendedInterface = extendedInterface;
    this.body = body;
    this.logger = logger;
    this.imports = Collections.unmodifiableSet(importTypes);
    sources = Arrays.asList(source);
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
    List<URL> urls = getExternalCss();

    final File bodyFile = getGeneratedFile();
    if (bodyFile != null) {
      try {
        urls.add(bodyFile.toURI().toURL());
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }
    assert urls.size() > 0;

    CssStylesheet sheet = GenerateCssAst.exec(logger.getTreeLogger(),
        urls.toArray(new URL[urls.size()]));
    return ExtractClassNamesVisitor.exec(sheet,
        imports.toArray(new JClassType[imports.size()]));
  }

  /**
   * @return the public interface that this CssResource implements
   */
  public JClassType getExtendedInterface() {
    return extendedInterface;
  }

  public Set<JClassType> getImports() {
    return imports;
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

  public String getQualifiedSourceName() {
    if (packageName.length() == 0) {
      return name;
    }

    return String.format("%s.%s", packageName, className);
  }

  /**
   * @return the name of the .css file(s), separate by white space
   */
  public Collection<String> getSource() {
    if (body.length() == 0) {
      return Collections.unmodifiableCollection(sources);
    }

    List<String> rtn = new ArrayList<String>(sources);
    rtn.add(getBodyFileName());
    return rtn;
  }

  private String getBodyFileName() {
    return String.format("uibinder:%s.%s.css", packageName, className);
  }

  private List<URL> getExternalCss() throws UnableToCompleteException {
    /*
     * TODO(rjrjr,bobv) refactor ResourceGeneratorUtil.findResources so we can
     * find them the same way ClientBundle does. For now, just look relative to
     * this package
     */

    ClassLoader classLoader = ImplicitCssResource.class.getClassLoader();
    String path = packageName.replace(".", "/");

    List<URL> urls = new ArrayList<URL>();

    for (String s : sources) {
      String resourcePath = path + '/' + s;
      URL found = classLoader.getResource(resourcePath);
      if (null == found) {
        logger.die("Unable to find resource: " + resourcePath);
      }
      urls.add(found);
    }
    return urls;
  }

  private File getGeneratedFile() {
    if (body.length() == 0) {
      return null;
    }

    if (generatedFile == null) {
      try {
        File f = File.createTempFile(String.format("uiBinder_%s_%s",
            packageName, className), ".css");
        f.deleteOnExit();

        BufferedWriter out = new BufferedWriter(new FileWriter(f));
        out.write(body);
        out.close();
        generatedFile = f;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      ResourceGeneratorUtil.addNamedFile(getBodyFileName(), generatedFile);
    }
    return generatedFile;
  }
}
