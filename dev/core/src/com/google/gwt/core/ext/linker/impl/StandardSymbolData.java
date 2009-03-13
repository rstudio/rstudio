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
package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.linker.SymbolData;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * An immutable implementation of SymbolData.
 */
public class StandardSymbolData implements SymbolData {

  public static SymbolData forClass(String className, String fileName,
      int lineNumber) {
    URI uri = inferUriFromFileName(fileName);
    return new StandardSymbolData(className, null, null, uri, lineNumber);
  }

  public static SymbolData forMember(String className, String memberName,
      String jsniIdent, String fileName, int lineNumber) {
    URI uri = inferUriFromFileName(fileName);
    return new StandardSymbolData(className, jsniIdent, memberName, uri,
        lineNumber);
  }

  private static URI inferUriFromFileName(String fileName) {
    File f = new File(fileName);
    if (f.exists()) {
      return f.toURI();
    } else {
      try {
        return new URI(fileName);
      } catch (URISyntaxException e) {
        return null;
      }
    }
  }

  private final String className;
  private final String jsniIdent;
  private final String memberName;
  private final int sourceLine;
  private final URI sourceUri;

  private StandardSymbolData(String className, String jsniIdent,
      String memberName, URI sourceUri, int sourceLine) {
    assert className != null && className.length() > 0 : "className";
    assert !(jsniIdent == null ^ memberName == null) : "jsniIdent ^ memberName";
    assert sourceLine >= -1 : "sourceLine: " + sourceLine;

    this.className = className;
    this.jsniIdent = jsniIdent;
    this.memberName = memberName;
    this.sourceUri = sourceUri;
    this.sourceLine = sourceLine;
  }

  public String getClassName() {
    return className;
  }

  public String getJsniIdent() {
    return jsniIdent;
  }

  public String getMemberName() {
    return memberName;
  }

  public int getSourceLine() {
    return sourceLine;
  }

  public URI getSourceUri() {
    return sourceUri;
  }

  public boolean isClass() {
    return memberName == null;
  }

  public boolean isField() {
    return jsniIdent != null && !jsniIdent.contains("(");
  }

  public boolean isMethod() {
    return jsniIdent != null && jsniIdent.contains("(");
  }

  @Override
  public String toString() {
    return jsniIdent != null ? jsniIdent : className;
  }
}
