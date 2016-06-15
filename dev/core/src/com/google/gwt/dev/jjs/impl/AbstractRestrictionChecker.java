/*
 * Copyright 2016 Google Inc.
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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.jjs.HasSourceInfo;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMember;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Ordering;
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.thirdparty.guava.common.collect.TreeMultimap;

import java.util.Set;
import java.util.TreeSet;

/**
 * Abstract base class for error checking in the full AST.
 */
public abstract class AbstractRestrictionChecker {
  private Multimap<String, String> errorsByFilename
      = TreeMultimap.create(Ordering.natural(), AbstractTreeLogger.LOG_LINE_COMPARATOR);
  private Multimap<String, String> warningsByFilename
      = TreeMultimap.create(Ordering.natural(), AbstractTreeLogger.LOG_LINE_COMPARATOR);
  private Set<String> suggestionMessages = Sets.newLinkedHashSet();

  protected static String getDescription(HasSourceInfo hasSourceInfo) {
    if (hasSourceInfo instanceof JDeclaredType) {
      return getTypeDescription((JDeclaredType) hasSourceInfo);
    } else {
      return getMemberDescription((JMember) hasSourceInfo);
    }
  }

  protected static String getMemberDescription(JMember member) {
    if (member instanceof JField) {
      return String.format("'%s'", JjsUtils.getReadableDescription(member));
    }
    JMethod method = (JMethod) member;
    if ((method.isSyntheticAccidentalOverride() || method.isSynthetic())
        // Some synthetic methods are created by JDT, it is not safe to assume
        // that they will always be overriding and crash the compiler.
        && !method.getOverriddenMethods().isEmpty()) {
      JMethod overridenMethod = Iterables.getFirst(method.getOverriddenMethods(), null);
      return String.format("'%s' (exposed by '%s')",
          JjsUtils.getReadableDescription(overridenMethod),
          JjsUtils.getReadableDescription(method.getEnclosingType()));
    }
    return String.format("'%s'", JjsUtils.getReadableDescription(method));
  }

  private static String getTypeDescription(JDeclaredType type) {
    return String.format("'%s'", JjsUtils.getReadableDescription(type));
  }

  protected void logError(String format, JType type) {
    logError(type, format, JjsUtils.getReadableDescription(type));
  }

  protected void logError(HasSourceInfo hasSourceInfo, String format, Object... args) {
    errorsByFilename.put(hasSourceInfo.getSourceInfo().getFileName(),
        String.format("Line %d: ", hasSourceInfo.getSourceInfo().getStartLine())
            + String.format(format, args));
  }

  protected void logWarning(HasSourceInfo hasSourceInfo, String format, Object... args) {
    warningsByFilename.put(hasSourceInfo.getSourceInfo().getFileName(),
        String.format("Line %d: ", hasSourceInfo.getSourceInfo().getStartLine())
            + String.format(format, args));
  }

  protected void logSuggestion(String format, Object... args) {
    suggestionMessages.add(String.format(format, args));
  }

  protected boolean reportErrorsAndWarnings(TreeLogger logger) {
    TreeSet<String> filenamesToReport = Sets.newTreeSet(
        Iterables.concat(errorsByFilename.keySet(), warningsByFilename.keySet()));
    for (String fileName : filenamesToReport) {
      boolean hasErrors = !errorsByFilename.get(fileName).isEmpty();
      TreeLogger branch = logger.branch(
         Type.INFO, (hasErrors ? "Errors" : "Warnings") + " in " + fileName);
      for (String message : errorsByFilename.get(fileName)) {
        branch.log(Type.ERROR, message);
      }
      for (String message :warningsByFilename.get(fileName)) {
        branch.log(Type.WARN, message);
      }
    }
    for (String message : suggestionMessages) {
      logger.log(Type.WARN, message);
    }
    return !errorsByFilename.isEmpty();
  }
}
