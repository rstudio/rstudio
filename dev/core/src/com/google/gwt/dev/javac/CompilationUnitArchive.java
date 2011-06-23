/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.dev.javac;

import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class represents a file that contains {@link CachedCompilationUnit}s
 * that make up a module.
 * 
 * Intended to work with other CompilationUnitArchives on the class path to
 * allow a project to be compiled incrementally. Therefore, one file may not
 * contain all dependent compilation units. To get all dependent compilation
 * units, the program will need to load all archives, plus any files not
 * contained in any archive.
 *
 * No mater how the archive is created, when serialized, the output file 
 * should be deterministic.
 */
public class CompilationUnitArchive implements Serializable {

  public static CompilationUnitArchive createFromFile(File location) throws IOException,
      ClassNotFoundException {
    return createFromStream(new FileInputStream(location));
  }

  public static CompilationUnitArchive createFromStream(InputStream stream) throws IOException,
      ClassNotFoundException {
    ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(stream));
    CompilationUnitArchive result = (CompilationUnitArchive) ois.readObject();
    ois.close();
    return result;
  }

  public static CompilationUnitArchive createFromURL(URL location) throws IOException,
      ClassNotFoundException {
    return createFromStream(location.openConnection().getInputStream());
  }

  private final String topModuleName;
  private transient Map<String, CachedCompilationUnit> units;

  /**
   * Create an archive object.  Note that data is retained in memory only until
   * the {@link #writeToFile(File)} method is invoked.
   * 
   * @param topModuleName The name of the module used to compile this archive.
   *          That is, the original parameter passed to
   *          {@link com.google.gwt.dev.CompileModule}.
   */
  public CompilationUnitArchive(String topModuleName) {
    units = new TreeMap<String, CachedCompilationUnit>();
    this.topModuleName = topModuleName;
  }

  /**
   * Add a compilation unit to the archive.
   */
  public void addUnit(CompilationUnit unit) {
    units.put(unit.getResourcePath(), unit.asCachedCompilationUnit());
  }

  public CachedCompilationUnit findUnit(String resourcePath) {
    return units.get(resourcePath);
  }

  /**
   * The name of the module used to compile this archive. 
   */
  public String getTopModuleName() {
    return topModuleName;
  }

  /**
   * Retrieve all units stored in this archive.
   */
  public Map<String, CachedCompilationUnit> getUnits() {
    return ImmutableMap.copyOf(units);
  }

  /**
   * Persists the units currently stored in the archive to the specified file.  The file
   * is immediately closed. 
   */
  public void writeToFile(File location) throws IOException {
    ObjectOutputStream oos =
        new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(location)));
    oos.writeObject(this);
    oos.close();
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    units = new TreeMap<String, CachedCompilationUnit>();
    CachedCompilationUnit unitsIn[] = (CachedCompilationUnit[]) stream.readObject();
    for (CachedCompilationUnit unit : unitsIn) {
      assert unit != null;
      addUnit(unit);
    }
  }

  // CachedCompilationUnits are serialized as a sorted array in order to make sure the
  // output format is deterministic.
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    CachedCompilationUnit unitsOut[] = units.values().toArray(new CachedCompilationUnit[units.size()]);
    Arrays.sort(unitsOut, CachedCompilationUnit.COMPARATOR);
    stream.writeObject(unitsOut);
  }
}
