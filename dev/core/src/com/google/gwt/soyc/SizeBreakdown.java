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
package com.google.gwt.soyc;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * A size breakdown of one code collection.
 */
public class SizeBreakdown {
  public Map<String, Integer> classToSize = new HashMap<String, Integer>();
  public Map<String, Integer> methodToSize = new HashMap<String, Integer>();

  public HashMap<String, CodeCollection> nameToCodeColl = new HashMap<String, CodeCollection>();

  public Map<String, LiteralsCollection> nameToLitColl = new TreeMap<String, LiteralsCollection>();
  public Map<String, Integer> packageToSize = new HashMap<String, Integer>();
  public int sizeAllCode;
  private final String description;

  private final String id;

  public SizeBreakdown(String description, String id) {
    this.description = description;
    this.id = id;

    initializeLiteralsCollection(nameToLitColl);
    initializeNameToCodeCollection(nameToCodeColl);
  }
  /**
   * A short but human-readable description of this code collection.
   */
  public String getDescription() {
    return description;
  }

  /**
   * An identifier for this code collection suitable for use within file names.
   */
  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return getId();
  }

  private void initializeLiteralsCollection(
      Map<String, LiteralsCollection> nameToLitColl) {
    nameToLitColl.put("long", new LiteralsCollection());
    nameToLitColl.put("null", new LiteralsCollection());
    nameToLitColl.put("class", new LiteralsCollection());
    nameToLitColl.put("int", new LiteralsCollection());
    nameToLitColl.put("string", new LiteralsCollection());
    nameToLitColl.put("number", new LiteralsCollection());
    nameToLitColl.put("boolean", new LiteralsCollection());
    nameToLitColl.put("double", new LiteralsCollection());
    nameToLitColl.put("char", new LiteralsCollection());
    nameToLitColl.put("undefined", new LiteralsCollection());
    nameToLitColl.put("float", new LiteralsCollection());
  }

  private void initializeNameToCodeCollection(
      HashMap<String, CodeCollection> nameToCodeColl) {
    nameToCodeColl.put("allOther", new CodeCollection());
    nameToCodeColl.put("widget", new CodeCollection());
    nameToCodeColl.put("rpcUser", new CodeCollection());
    nameToCodeColl.put("rpcGen", new CodeCollection());
    nameToCodeColl.put("rpcGwt", new CodeCollection());
    nameToCodeColl.put("gwtLang", new CodeCollection());
    nameToCodeColl.put("jre", new CodeCollection());
  }
}
