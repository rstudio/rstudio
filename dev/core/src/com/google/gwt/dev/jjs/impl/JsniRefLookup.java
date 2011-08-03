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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.util.JsniRef;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * A utility class that can look up a {@link JsniRef} in a {@link JProgram}.
 * 
 * @deprecated find alternatives, only a couple of corner cases use this now.
 */
@Deprecated
public class JsniRefLookup {
  /**
   * A callback used to indicate the reason for a failed JSNI lookup.
   */
  public interface ErrorReporter {
    void reportError(String error);
  }

  /**
   * Look up a JSNI reference.
   * 
   * @param ref The reference to look up
   * @param program The program to look up the reference in
   * @param errorReporter A callback used to indicate the reason for a failed
   *          JSNI lookup
   * @return The item referred to, or <code>null</code> if it could not be
   *         found. If the return value is <code>null</code>,
   *         <code>errorReporter</code> will have been invoked.
   */
  public static JNode findJsniRefTarget(JsniRef ref, JProgram program,
      JsniRefLookup.ErrorReporter errorReporter) {
    String className = ref.className();
    JType type = null;
    if (!className.equals("null")) {
      type = program.getTypeFromJsniRef(className);
      if (type == null) {
        errorReporter.reportError("Unresolvable native reference to type '" + className + "'");
        return null;
      }
    }

    if (!ref.isMethod()) {
      // look for a field
      String fieldName = ref.memberName();
      if (type == null) {
        if (fieldName.equals("nullField")) {
          return program.getNullField();
        }

      } else if (fieldName.equals(JsniRef.CLASS)) {
        return type;

      } else if (type instanceof JPrimitiveType) {
        errorReporter.reportError("May not refer to fields on primitive types");
        return null;

      } else if (type instanceof JArrayType) {
        errorReporter.reportError("May not refer to fields on array types");
        return null;

      } else {
        for (JField field : ((JDeclaredType) type).getFields()) {
          if (field.getName().equals(fieldName)) {
            return field;
          }
        }
      }

      errorReporter.reportError("Unresolvable native reference to field '" + fieldName
          + "' in type '" + className + "'");
      return null;
    } else if (type instanceof JPrimitiveType) {
      errorReporter.reportError("May not refer to methods on primitive types");
      return null;
    } else {
      // look for a method
      LinkedHashMap<String, LinkedHashMap<String, JMethod>> matchesBySig =
          new LinkedHashMap<String, LinkedHashMap<String, JMethod>>();
      String methodName = ref.memberName();
      String jsniSig = ref.memberSignature();
      if (type == null) {
        if (jsniSig.equals("nullMethod()")) {
          return program.getNullMethod();
        }
      } else {
        findMostDerivedMembers(matchesBySig, (JDeclaredType) type, methodName, true);
        LinkedHashMap<String, JMethod> matches = matchesBySig.get(jsniSig);
        if (matches != null && matches.size() == 1) {
          /*
           * Backward compatibility: allow accessing bridge methods with full
           * qualification
           */
          return matches.values().iterator().next();
        }

        removeSyntheticMembers(matchesBySig);
        matches = matchesBySig.get(jsniSig);
        if (matches != null && matches.size() == 1) {
          return matches.values().iterator().next();
        }
      }

      // Not found; signal an error
      if (matchesBySig.isEmpty()) {
        errorReporter.reportError("Unresolvable native reference to method '" + methodName
            + "' in type '" + className + "'");
        return null;
      } else {
        StringBuilder suggestList = new StringBuilder();
        String comma = "";
        // use a TreeSet to sort the near matches
        TreeSet<String> almostMatchSigs = new TreeSet<String>();
        for (String sig : matchesBySig.keySet()) {
          if (matchesBySig.get(sig).size() == 1) {
            almostMatchSigs.add(sig);
          }
        }
        for (String almost : almostMatchSigs) {
          suggestList.append(comma + "'" + almost + "'");
          comma = ", ";
        }
        errorReporter.reportError("Unresolvable native reference to method '" + methodName
            + "' in type '" + className + "' (did you mean " + suggestList.toString() + "?)");
        return null;
      }
    }
  }

  /**
   * Add a member to the table of most derived members.
   * 
   * @param matchesBySig The table so far of most derived members
   * @param member The member to add to it
   * @param refSig The string used to refer to that member, possibly shortened
   * @param fullSig The fully qualified signature for that member
   */
  private static void addMember(LinkedHashMap<String, LinkedHashMap<String, JMethod>> matchesBySig,
      JMethod member, String refSig, String fullSig) {
    LinkedHashMap<String, JMethod> matchesByFullSig = matchesBySig.get(refSig);
    if (matchesByFullSig == null) {
      matchesByFullSig = new LinkedHashMap<String, JMethod>();
      matchesBySig.put(refSig, matchesByFullSig);
    }
    matchesByFullSig.put(fullSig, member);
  }

  /**
   * For each member with the given name, find the most derived members for each
   * JSNI reference that match it. For wildcard JSNI references, there will in
   * general be more than one match. This method does not ignore synthetic
   * methods.
   */
  private static void findMostDerivedMembers(
      LinkedHashMap<String, LinkedHashMap<String, JMethod>> matchesBySig, JDeclaredType targetType,
      String memberName, boolean addConstructorsAndPrivates) {
    /*
     * Analyze superclasses and interfaces first. More derived members will thus
     * be seen later.
     */
    if (targetType instanceof JClassType) {
      JClassType targetClass = (JClassType) targetType;
      if (targetClass.getSuperClass() != null) {
        findMostDerivedMembers(matchesBySig, targetClass.getSuperClass(), memberName, false);
      }
    }
    for (JDeclaredType intf : targetType.getImplements()) {
      findMostDerivedMembers(matchesBySig, intf, memberName, false);
    }

    // Get the methods on this class/interface.
    for (JMethod method : targetType.getMethods()) {
      if (method instanceof JConstructor && JsniRef.NEW.equals(memberName)) {
        if (!addConstructorsAndPrivates) {
          continue;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(JsniRef.NEW);
        sb.append("(");
        for (JParameter param : method.getParams()) {
          sb.append(param.getType().getJsniSignatureName());
        }
        sb.append(")");
        String fullSig = sb.toString();
        String wildcardSig = JsniRef.NEW + "(" + JsniRef.WILDCARD_PARAM_LIST + ")";
        addMember(matchesBySig, method, fullSig, fullSig);
        addMember(matchesBySig, method, wildcardSig, fullSig);
      } else if (method.getName().equals(memberName)) {
        if (!addConstructorsAndPrivates && method.isPrivate()) {
          continue;
        }
        String fullSig = JProgram.getJsniSig(method, false);
        String wildcardSig = method.getName() + "(" + JsniRef.WILDCARD_PARAM_LIST + ")";
        addMember(matchesBySig, method, fullSig, fullSig);
        addMember(matchesBySig, method, wildcardSig, fullSig);
      }
    }
  }

  private static void removeSyntheticMembers(
      LinkedHashMap<String, LinkedHashMap<String, JMethod>> matchesBySig) {
    for (LinkedHashMap<String, JMethod> matchesByFullSig : matchesBySig.values()) {
      Set<String> toRemove = new LinkedHashSet<String>();
      for (String fullSig : matchesByFullSig.keySet()) {
        JMethod member = matchesByFullSig.get(fullSig);
        if (member.isSynthetic()) {
          toRemove.add(fullSig);
        }
      }
      for (String fullSig : toRemove) {
        matchesByFullSig.remove(fullSig);
      }
    }
  }
}
