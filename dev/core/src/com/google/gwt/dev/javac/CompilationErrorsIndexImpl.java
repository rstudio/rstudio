/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.javac;

import com.google.gwt.thirdparty.guava.common.base.Objects;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.SetMultimap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A mutable and serializable CompilationErrorsIndex.
 * <p>
 * JDT compilation errors for a current compile can be accumulated here, saved into libraries, and
 * reloaded later from libraries to enable accurate and detailed compilation error cause traces.
 */
public class CompilationErrorsIndexImpl implements CompilationErrorsIndex, Serializable {

  private static SetMultimap<String, String> readStringListMap(ObjectInputStream objectInputStream)
      throws IOException {
    HashMultimap<String, String> stringListMap = HashMultimap.<String, String> create();
    int keyCount = objectInputStream.readInt();
    for (int i = 0; i < keyCount; i++) {
      String key = objectInputStream.readUTF();
      int listLength = objectInputStream.readInt();
      for (int j = 0; j < listLength; j++) {
        stringListMap.put(key, objectInputStream.readUTF());
      }
    }
    return stringListMap;
  }

  private static Map<String, String> readStringMap(ObjectInputStream objectInputStream)
      throws IOException {
    Map<String, String> stringMap = Maps.newHashMap();
    int fileNameMappingCount = objectInputStream.readInt();
    for (int i = 0; i < fileNameMappingCount; i++) {
      String typeSourceName = objectInputStream.readUTF();
      String fileName = objectInputStream.readUTF();
      stringMap.put(typeSourceName, fileName);
    }
    return stringMap;
  }

  private static void writeStringListMap(ObjectOutputStream objectOutputStream,
      Map<String, Collection<String>> stringMap) throws IOException {
    objectOutputStream.writeInt(stringMap.size());
    for (Entry<String, Collection<String>> entry : stringMap.entrySet()) {
      objectOutputStream.writeUTF(entry.getKey());
      Collection<String> strings = entry.getValue();
      objectOutputStream.writeInt(strings.size());
      for (String string : strings) {
        objectOutputStream.writeUTF(string);
      }
    }
  }

  private static void writeStringMap(ObjectOutputStream objectOutputStream,
      Map<String, String> stringMap) throws IOException {
    objectOutputStream.writeInt(stringMap.size());
    for (Entry<String, String> entry : stringMap.entrySet()) {
      objectOutputStream.writeUTF(entry.getKey());
      objectOutputStream.writeUTF(entry.getValue());
    }
  }

  private SetMultimap<String, String> compilationErrorsByTypeSourceName =
      HashMultimap.<String, String> create();
  private Map<String, String> fileNamesByTypeSourceName = Maps.newHashMap();
  private SetMultimap<String, String> referencesByTypeSourceName =
      HashMultimap.<String, String> create();

  @Override
  public void add(String typeSourceName, String fileName, List<String> typeReferences,
      List<String> compilationErrors) {
    fileNamesByTypeSourceName.put(typeSourceName, fileName);
    compilationErrorsByTypeSourceName.putAll(typeSourceName, compilationErrors);
    referencesByTypeSourceName.putAll(typeSourceName, typeReferences);
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof CompilationErrorsIndexImpl) {
      CompilationErrorsIndexImpl that = (CompilationErrorsIndexImpl) object;
      return
          Objects.equal(this.fileNamesByTypeSourceName, that.fileNamesByTypeSourceName) && Objects
              .equal(this.compilationErrorsByTypeSourceName, that.compilationErrorsByTypeSourceName)
          && Objects.equal(this.referencesByTypeSourceName, that.referencesByTypeSourceName);
    }
    return false;
  }

  @Override
  public Set<String> getCompileErrors(String typeSourceName) {
    return compilationErrorsByTypeSourceName.get(typeSourceName);
  }

  @Override
  public String getFileName(String typeSourceName) {
    return fileNamesByTypeSourceName.get(typeSourceName);
  }

  @Override
  public Set<String> getTypeReferences(String typeSourceName) {
    return referencesByTypeSourceName.get(typeSourceName);
  }

  @Override
  public boolean hasCompileErrors(String typeSourceName) {
    return compilationErrorsByTypeSourceName.containsKey(typeSourceName);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(fileNamesByTypeSourceName, compilationErrorsByTypeSourceName,
        referencesByTypeSourceName);
  }

  @Override
  public boolean hasTypeReferences(String typeSourceName) {
    return referencesByTypeSourceName.containsKey(typeSourceName);
  }

  private void readObject(ObjectInputStream objectInputStream) throws IOException {
    fileNamesByTypeSourceName = readStringMap(objectInputStream);
    compilationErrorsByTypeSourceName = readStringListMap(objectInputStream);
    referencesByTypeSourceName = readStringListMap(objectInputStream);
  }

  private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
    writeStringMap(objectOutputStream, fileNamesByTypeSourceName);
    writeStringListMap(objectOutputStream, compilationErrorsByTypeSourceName.asMap());
    writeStringListMap(objectOutputStream, referencesByTypeSourceName.asMap());
  }
}
