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
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * An immutable implementation of SymbolData.
 */
public class StandardSymbolData implements SymbolData {

  public static StandardSymbolData forClass(String className, String uriString,
      int lineNumber, String typeId) {
    return new StandardSymbolData(className, null, null, uriString, lineNumber,
        typeId);
  }

  public static StandardSymbolData forMember(String className,
      String memberName, String methodSig, String uriString, int lineNumber) {
    return new StandardSymbolData(className, memberName, methodSig, uriString,
        lineNumber, null);
  }

  public static String toUriString(String fileName) {
    File f = new File(fileName);
    if (f.exists()) {
      return f.toURI().toASCIIString();
    } else {
      try {
        return new URI(fileName).toASCIIString();
      } catch (URISyntaxException e) {
        return null;
      }
    }
  }

  private String className;
  private int fragmentNumber = -1;
  private String memberName;
  private String methodSig;
  private int sourceLine;
  private String sourceUri;
  private String symbolName;
  private String typeId;

  private StandardSymbolData(String className, String memberName,
      String methodSig, String sourceUri, int sourceLine, String typeId) {
    assert className != null && className.length() > 0 : "className";
    assert memberName != null || methodSig == null : "methodSig without memberName";
    assert sourceLine >= -1 : "sourceLine: " + sourceLine;

    this.className = className;
    this.memberName = memberName;
    this.methodSig = methodSig;
    this.sourceUri = sourceUri;
    this.sourceLine = sourceLine;
    this.typeId = typeId;
  }

  @Override
  public String getClassName() {
    return className;
  }

  @Override
  public int getFragmentNumber() {
    return fragmentNumber;
  }

  @Override
  public String getJsniIdent() {
    if (memberName == null) {
      return null;
    }
    if (methodSig == null) {
      return className + "::" + memberName;
    }
    return className + "::" + memberName + methodSig;
  }

  @Override
  public String getMemberName() {
    return memberName;
  }
  @Override
  public String getRuntimeTypeId() {
    return typeId;
  }

  @Override
  public int getSourceLine() {
    return sourceLine;
  }

  @Override
  public String getSourceUri() {
    return sourceUri;
  }

  @Override
  public String getSymbolName() {
    return symbolName;
  }

  @Override
  public boolean isClass() {
    return memberName == null;
  }

  @Override
  public boolean isField() {
    return memberName != null && methodSig == null;
  }

  @Override
  public boolean isMethod() {
    return methodSig != null;
  }

  public void setFragmentNumber(int fragNum) {
    fragmentNumber = fragNum;
  }

  public void setSymbolName(String symbolName) {
    this.symbolName = symbolName;
  }

  @Override
  public String toString() {
    return isClass() ? className : getJsniIdent();
  }

  private void readObject(ObjectInputStream in) throws IOException,
      ClassNotFoundException {
    className = (String) in.readObject();
    switch (in.read()) {
      case 0:
        break;
      case 1:
        memberName = in.readUTF();
        break;
      case 2:
        memberName = in.readUTF();
        methodSig = in.readUTF();
        break;
      default:
        throw new InvalidObjectException("Unexpected input");
    }
    sourceLine = in.readInt();
    sourceUri = (String) in.readObject();
    symbolName = in.readUTF();
    typeId = (String) in.readObject();
    fragmentNumber = in.readInt();
  }

  /**
   * Implemented by hand for speed (over using reflection) because there are so
   * many of these. Note that {@link #className} and {@link #sourceUri} are done
   * as writeObject because the actual String instances are very likely to be
   * shared among many instances of this class, so the same objects can be
   * reused in the stream. The other String fields are done as UTF because it's
   * slightly faster and the String objects are unlikely to be shared among
   * instances.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    out.writeObject(className);
    if (isClass()) {
      out.write(0);
    } else if (isField()) {
      out.write(1);
      out.writeUTF(memberName);
    } else {
      out.write(2);
      out.writeUTF(memberName);
      out.writeUTF(methodSig);
    }
    out.writeInt(sourceLine);
    out.writeObject(sourceUri);
    out.writeUTF(symbolName);
    out.writeObject(typeId);
    out.writeInt(fragmentNumber);
  }

}
