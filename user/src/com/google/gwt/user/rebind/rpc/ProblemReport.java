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
package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.typeinfo.JClassType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A collection of reported problems; these are accumulated during the
 * SerializableTypeOracleBuilder's isSerializable analysis, and what to do about
 * the problems is decided only later.
 */
public class ProblemReport {

  /**
   * Priority of problems.  {@link #FATAL}  problems will fail a build that
   * would otherwise have succeeded, for example because of a bad custom
   * serializer used only as a subclass of a superclass with other viable
   * subtypes.  {@link #DEFAULT} problems might or might not be fatal,
   * depending on overall results accumulated later.  {@link #AUXILIARY}
   * problems are not fatal, and often not even problems by themselves, but
   * diagnostics related to default problems (e.g. type filtration, which
   * might suppress an intended-to-serialize class).
   */
  public enum Priority { FATAL, DEFAULT, AUXILIARY}

  private Map<JClassType, List<String>> allProblems;
  private Map<JClassType, List<String>> auxiliaries;
  private Map<JClassType, List<String>> fatalProblems;
  private JClassType contextType;

  /**
   * Creates a new, empty, context-less ProblemReport.
   */
  public ProblemReport() {
    Comparator<JClassType> comparator = new Comparator<JClassType>() {
        public int compare(JClassType o1, JClassType o2) {
          assert o1 != null;
          assert o2 != null;
          return o1.getParameterizedQualifiedSourceName().compareTo(
              o2.getParameterizedQualifiedSourceName());
        }
      };
    allProblems = new TreeMap<JClassType, List<String>>(comparator);
    auxiliaries = new TreeMap<JClassType, List<String>>(comparator);
    fatalProblems = new TreeMap<JClassType, List<String>>(comparator);
    contextType = null;
  }

  /**
   * Adds a problem for a given type.  This also sorts the problems into
   * collections by priority.
   *
   * @param type the problematic type
   * @param message the description of the problem
   * @param priority priority of the problem.
   * @param extraLines additional continuation lines for the message, usually
   *    for additional explanations.
   */
  public void add(JClassType type, String message, Priority priority, 
      String... extraLines) {
    String contextString = "";
    if (contextType != null) {
      contextString = " (reached via " +
          contextType.getParameterizedQualifiedSourceName() + ")";
    }
    message = message + contextString;
    for (String line : extraLines) {
      message = message + "\n   " + line;
    }
    if (priority == Priority.AUXILIARY) {
      addToMap(type, message, auxiliaries);
      return;
    }

    // both FATAL and DEFAULT problems go in allProblems...
    addToMap(type, message, allProblems);

    // only FATAL problems go in fatalProblems...
    if (priority == Priority.FATAL) {
      addToMap(type, message, fatalProblems);
    }
  }

  /**
   * Returns list of auxiliary "problems" logged against a given type.
   *
   * @param type type to fetch problems for
   * @return {@code null} if no auxiliaries were logged.  Otherwise, a list
   *   of strings describing messages, including the context in which the
   *   problem was found.
   */
  public List<String> getAuxiliaryMessagesForType(JClassType type) {
    List<String> list = auxiliaries.get(type);
    if (list == null) {
      list = new ArrayList<String>(0);
    }
    return list;
  }

  /**
   * Returns list of problems logged against a given type.
   *
   * @param type type to fetch problems for
   * @return {@code null} if no problems were logged.  Otherwise, a list
   *   of strings describing problems, including the context in which the
   *   problem was found.
   */
  public List<String> getProblemsForType(JClassType type) {
    List<String> list = allProblems.get(type);
    if (list == null) {
      list = new ArrayList<String>(0);
    }
    return list;
  }

  /**
   * Returns the number of types against which problems were reported.
   *
   * @return number of problematic types
   */
  public int getProblemTypeCount() {
    return allProblems.size();
  }

  /**
   * Were any problems reported as "fatal"?
   */
  public boolean hasFatalProblems() {
    return !fatalProblems.isEmpty();
  }

  /**
   * Reports all problems to the logger supplied, at the log level supplied.
   * The problems are assured of being reported in lexographic order of
   * type names.
   *
   * @param logger logger to receive problem reports
   * @param problemLevel severity level at which to report problems.
   * @param auxLevel severity level at which to report any auxiliary messages.
   */
  public void report(TreeLogger logger, TreeLogger.Type problemLevel,
      TreeLogger.Type auxLevel) {
    doReport(logger, auxLevel, auxiliaries);
    doReport(logger, problemLevel, allProblems);
  }

  /**
   * Reports only urgent problems to the logger supplied, at the log level
   * supplied.  The problems are assured of being reported in lexographic
   * order of type names.
   *
   * @param logger logger to receive problem reports
   * @param level severity level at which to report problems.
   */
  public void reportFatalProblems(TreeLogger logger, TreeLogger.Type level) {
    doReport(logger, level, fatalProblems);
  }

  /**
   * Sets the context type currently being analyzed.  Problems found will
   * include reference to this context, until reset with another call to this
   * method.  Context may be cancelled with a {@code null} value here.
   *
   * @param newContext the type under analysis
   */
  public void setContextType(JClassType newContext) {
    contextType = newContext;
  }

  /**
   * Adds an entry to one of the problem maps.
   *
   * @param type the type to add
   * @param message the message to add for {@code type}
   * @param map the map to add to
   */
  private void addToMap(JClassType type, String message,
      Map<JClassType, List<String>> map) {
    List<String> entry = map.get(type);
    if (entry == null) {
      entry = new ArrayList<String>();
      map.put(type, entry);
    }
    entry.add(message);
  }

  /**
   * Logs all of the problems from one of the problem maps.
   *
   * @param logger the logger to log to
   * @param level the level for messages
   * @param problems the problems to log
   */
  private void doReport(TreeLogger logger, Type level,
      Map<JClassType, List<String>> problems) {
    for (JClassType type : problems.keySet()) {
      for (String message : problems.get(type)) {
        logger.log(level, message);
      }
    }
  }
}
