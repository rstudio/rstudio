/*
 * Copyright 2006 Google Inc.
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
package com.google.doctool;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.MemberDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.SourcePosition;
import com.sun.javadoc.Tag;

/**
 * Methods related to resolving cross-references in source.
 */
public class LinkResolver {

  /**
   * Abstracts out mechanisms for finding different bits of doc.
   */
  public interface ExtraClassResolver {
    ClassDoc findClass(String className);
  }

  public static SourcePosition resolveLink(Tag tag) {
    return resolveLink(tag, null);
  }

  public static SourcePosition resolveLink(Tag tag,
      ExtraClassResolver classResolver) {
    String linkText = tag.text();
    String className;
    String methodSig;
    int pos = linkText.indexOf('#');
    if (pos >= 0) {
      className = linkText.substring(0, pos);
      methodSig = linkText.substring(pos + 1);
    } else {
      className = linkText;
      methodSig = null;
    }

    ClassDoc containingClass = null;
    Doc holder = tag.holder();
    if (holder instanceof ClassDoc) {
      containingClass = (ClassDoc) holder;
    } else if (holder instanceof MemberDoc) {
      containingClass = ((MemberDoc) holder).containingClass();
    }

    ClassDoc targetClass = null;
    if (className.length() == 0) {
      targetClass = containingClass;
    } else if (holder instanceof PackageDoc) {
      targetClass = ((PackageDoc) holder).findClass(className);
    } else {
      targetClass = containingClass.findClass(className);
    }

    if (targetClass == null) {
      if (classResolver != null) {
        targetClass = classResolver.findClass(className);
      }
      if (targetClass == null) {
        System.err.println(tag.position().toString()
            + ": unable to resolve class " + className + " for " + tag);
        System.exit(1);
      }
    }

    if (methodSig == null) {
      return targetClass.position();
    }

    String paramSig = methodSig.substring(methodSig.indexOf('(') + 1,
        methodSig.lastIndexOf(')'));
    String[] resolvedParamTypes;
    if (paramSig.length() > 0) {
      String[] unresolvedParamTypes = paramSig.split("\\s*,\\s*");
      resolvedParamTypes = new String[unresolvedParamTypes.length];
      for (int i = 0; i < unresolvedParamTypes.length; ++i) {
        ClassDoc paramType = containingClass.findClass(unresolvedParamTypes[i]);
        if (paramType == null && classResolver != null) {
          paramType = classResolver.findClass(unresolvedParamTypes[i]);
        }
        if (paramType == null) {
          System.err.println(tag.position().toString()
              + ": unable to resolve class " + unresolvedParamTypes[i]
              + " for " + tag);
          System.exit(1);
        }
        resolvedParamTypes[i] = paramType.qualifiedTypeName();
      }
    } else {
      resolvedParamTypes = new String[0];
    }

    String possibleOverloads = "";

    MethodDoc[] methods = targetClass.methods();
    outer : for (int i = 0; i < methods.length; ++i) {
      MethodDoc methodDoc = methods[i];
      if (methodSig.startsWith(methodDoc.name())) {
        possibleOverloads += "\n" + methodDoc.flatSignature();
        Parameter[] tryParams = methodDoc.parameters();
        if (resolvedParamTypes.length != tryParams.length) {
          // param count mismatch
          continue outer;
        }
        for (int j = 0; j < tryParams.length; ++j) {
          if (!tryParams[j].type().qualifiedTypeName().equals(
              resolvedParamTypes[j])) {
            // param type mismatch
            continue outer;
          }
        }
        return methodDoc.position();
      }
    }

    System.err.println(tag.position().toString()
        + ": unable to resolve method for " + tag);
    if (possibleOverloads.length() > 0) {
      System.err.println("Possible overloads: " + possibleOverloads);
    }
    System.exit(1);
    return null;
  }

}
