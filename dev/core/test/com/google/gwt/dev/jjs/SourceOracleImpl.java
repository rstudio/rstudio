// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;
import com.google.gwt.dev.jdt.SourceOracle;
import com.google.gwt.dev.jdt.URLCompilationUnitProvider;

import java.net.URL;

final class SourceOracleImpl implements SourceOracle {

  public CompilationUnitProvider findCompilationUnit(TreeLogger logger,
      String sourceTypeName) {
    ClassLoader cl = getClass().getClassLoader();
    URL url = cl.getResource(sourceTypeName.replace('.', '/') + ".java");
    if (url != null) {
      String packageName = "";
      int i = sourceTypeName.lastIndexOf('.');
      if (i != -1) {
        packageName = sourceTypeName.substring(0, i);
      }
      return new URLCompilationUnitProvider(url, packageName);
    } else {
      return null;
    }
  }

  public boolean isPackage(String maybePackage) {
    ClassLoader cl = getClass().getClassLoader();
    URL url = cl.getResource(maybePackage.replace('.', '/').concat("/"));
    if (url != null)
      return true;
    else
      return false;
  }

}
