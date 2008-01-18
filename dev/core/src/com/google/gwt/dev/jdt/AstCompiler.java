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
package com.google.gwt.dev.jdt;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;

import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A facade around the JDT compiler to make many repeated compiles as fast as
 * possible, where the result of each compile is a fully-resolved abstract
 * syntax tree that can be walked for detailed analysis.
 */
public class AstCompiler extends AbstractCompiler {

  /**
   * Manages the caching of <code>CompilationUnitDeclaration</code>.
   */
  private class CompilationUnitDeclarationCache {

    private final Map<String, Long> lastModified = new HashMap<String, Long>();

    private final Map<String, ArrayList<CompilationUnitDeclaration>> map = new HashMap<String, ArrayList<CompilationUnitDeclaration>>();

    public void remove(String newLoc) {
      map.remove(newLoc);
    }

    private void add(String location, CompilationUnitDeclaration item) {
      File file = new File(location);
      if (file.exists()) {
        lastModified.put(location, new Long(file.lastModified()));
      }
      if (!map.containsKey(location)) {
        map.put(location, new ArrayList<CompilationUnitDeclaration>());
      }
      get(location).add(item);
    }

    private boolean containsKey(String location) {
      return map.containsKey(location);
    }

    private List<CompilationUnitDeclaration> get(Object key) {
      return map.get(key);
    }

    private void removeAll(Collection<String> c) {
      map.keySet().removeAll(c);
    }
  }

  private final CompilationUnitDeclarationCache cachedResults = new CompilationUnitDeclarationCache();

  public AstCompiler(SourceOracle sourceOracle) {
    super(sourceOracle, false);
  }

  public CompilationUnitDeclaration[] getChangedCompilationUnitDeclarations(
      TreeLogger logger, ICompilationUnit[] units) {
    List<ICompilationUnit> allUnits = Arrays.asList(units);
    List<ICompilationUnitAdapter> newUnits = new ArrayList<ICompilationUnitAdapter>();

    // Check for newer units that need to be processed.
    for (Iterator<ICompilationUnit> iter = allUnits.iterator(); iter.hasNext();) {
      ICompilationUnitAdapter adapter = (ICompilationUnitAdapter) iter.next();
      CompilationUnitProvider cup = adapter.getCompilationUnitProvider();
      String location = cup.getLocation();
      if (!(cachedResults.containsKey(location))) {
        newUnits.add(adapter);
      }
    }
    ICompilationUnit[] toBeProcessed = new ICompilationUnit[newUnits.size()];
    newUnits.toArray(toBeProcessed);
    CompilationUnitDeclaration[] newCuds = compile(logger, toBeProcessed);

    // Put new cuds into cache.
    List<CompilationUnitDeclaration> cudResults = new ArrayList<CompilationUnitDeclaration>();
    for (int i = 0; i < newCuds.length; i++) {
      CompilationUnitDeclaration newCud = newCuds[i];
      String newLoc = String.valueOf(newCud.getFileName());
      cachedResults.remove(newLoc);
      cachedResults.add(newLoc, newCud);
      cudResults.add(newCud);
    }

    return cudResults.toArray(new CompilationUnitDeclaration[] {});
  }

  public void invalidateChangedFiles(Set<String> changedFiles,
      Set<String> typeNames) {
    cachedResults.removeAll(changedFiles);
    invalidateUnitsInFiles(changedFiles, typeNames);
  }
}
