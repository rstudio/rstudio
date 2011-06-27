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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.Util;
import com.google.gwt.util.tools.Utility;

import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A grab bag of utility functions useful for dealing with java files.
 */
public class Shared {

  public static final int MOD_ABSTRACT = 0x00000001;
  public static final int MOD_FINAL = 0x00000002;
  public static final int MOD_NATIVE = 0x00000004;
  public static final int MOD_PRIVATE = 0x00000008;
  public static final int MOD_PROTECTED = 0x00000010;
  public static final int MOD_PUBLIC = 0x00000020;
  public static final int MOD_STATIC = 0x00000040;
  public static final int MOD_TRANSIENT = 0x00000080;
  public static final int MOD_VOLATILE = 0x00000100;
  public static final JClassType[] NO_JCLASSES = new JClassType[0];
  public static final JConstructor[] NO_JCTORS = new JConstructor[0];
  public static final JField[] NO_JFIELDS = new JField[0];
  public static final JMethod[] NO_JMETHODS = new JMethod[0];
  public static final JPackage[] NO_JPACKAGES = new JPackage[0];
  public static final JParameter[] NO_JPARAMS = new JParameter[0];
  public static final JType[] NO_JTYPES = new JType[0];
  public static final String[][] NO_STRING_ARR_ARR = new String[0][];
  public static final String[] NO_STRINGS = new String[0];

  public static int bindingToModifierBits(FieldBinding binding) {
    int bits = 0;
    bits |= (binding.isPublic() ? MOD_PUBLIC : 0);
    bits |= (binding.isPrivate() ? MOD_PRIVATE : 0);
    bits |= (binding.isProtected() ? MOD_PROTECTED : 0);
    bits |= (binding.isStatic() ? MOD_STATIC : 0);
    bits |= (binding.isTransient() ? MOD_TRANSIENT : 0);
    bits |= (binding.isFinal() ? MOD_FINAL : 0);
    bits |= (binding.isVolatile() ? MOD_VOLATILE : 0);
    return bits;
  }

  public static int bindingToModifierBits(MethodBinding binding) {
    int bits = 0;
    bits |= (binding.isPublic() ? MOD_PUBLIC : 0);
    bits |= (binding.isPrivate() ? MOD_PRIVATE : 0);
    bits |= (binding.isProtected() ? MOD_PROTECTED : 0);
    bits |= (binding.isStatic() ? MOD_STATIC : 0);
    bits |= (binding.isFinal() ? MOD_FINAL : 0);
    bits |= (binding.isNative() ? MOD_NATIVE : 0);
    bits |= (binding.isAbstract() ? MOD_ABSTRACT : 0);
    return bits;
  }

  public static int bindingToModifierBits(ReferenceBinding binding) {
    int bits = 0;
    bits |= (binding.isPublic() ? MOD_PUBLIC : 0);
    bits |= (binding.isPrivate() ? MOD_PRIVATE : 0);
    bits |= (binding.isProtected() ? MOD_PROTECTED : 0);
    bits |= (binding.isStatic() ? MOD_STATIC : 0);
    bits |= (binding.isFinal() ? MOD_FINAL : 0);
    bits |= (binding.isAbstract() ? MOD_ABSTRACT : 0);
    return bits;
  }

  public static String getPackageName(String qualifiedTypeName) {
    int pos = qualifiedTypeName.lastIndexOf('.');
    return (pos < 0) ? "" : qualifiedTypeName.substring(0, pos);
  }

  public static String getPackageNameFromBinary(String binaryName) {
    int pos = binaryName.lastIndexOf('/');
    return (pos < 0) ? "" : binaryName.substring(0, pos).replace('/', '.');
  }

  public static String getShortName(String qualifiedTypeName) {
    int pos = qualifiedTypeName.lastIndexOf('.');
    return (pos < 0) ? qualifiedTypeName : qualifiedTypeName.substring(pos + 1);
  }

  public static String getSlashedPackageFrom(String internalName) {
    int pos = internalName.lastIndexOf('/');
    return (pos < 0) ? "" : internalName.substring(0, pos);
  }

  public static String getTypeName(Resource sourceFile) {
    String path = sourceFile.getPath();
    assert (path.endsWith(".java"));
    path = path.substring(0, path.lastIndexOf('.'));
    return path.replace('/', '.');
  }

  public static String makeTypeName(String packageName, String shortName) {
    if (packageName.length() == 0) {
      return shortName;
    } else {
      return packageName + '.' + shortName;
    }
  }

  public static String readContent(InputStream content) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buf = new byte[1024];
      for (int readCount = content.read(buf); readCount > 0; readCount = content.read(buf)) {
        out.write(buf, 0, readCount);
      }
      return Util.toString(out.toByteArray());
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      Utility.close(content);
    }
  }

  public static String readSource(Resource sourceFile) throws IOException {
    InputStream contents = sourceFile.openContents();
    return Util.readStreamAsString(contents);
  }

  public static String toInternalName(String path) {
    assert (path.endsWith(".java"));
    return path.substring(0, path.lastIndexOf('.'));
  }

  public static String toPath(String qualifiedTypeName) {
    return qualifiedTypeName.replace('.', '/') + ".java";
  }

  public static String toTypeName(String path) {
    assert (path.endsWith(".java"));
    path = path.substring(0, path.lastIndexOf('.'));
    return path.replace('/', '.');
  }
}
