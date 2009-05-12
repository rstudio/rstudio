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
package com.google.gwt.core.ext.linker;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Provides basic information about symbols in the generated JavaScript.
 * 
 * @see CompilationResult#getSymbolMap()
 */
public interface SymbolData extends Serializable {
  /*
   * NB: This class is intended to provide enough data to synthesize
   * StackTraceElements, however we don't want to use STE in our API in the case
   * that we want to provide additional data in the future.
   * 
   * Note also that this class does not provide the name of the symbol it is
   * describing, mainly because the JS compilation process results in multiple
   * symbols that are mapped onto the same SymbolData (e.g. MakeCallsStatic).
   */

  /**
   * A Comparator for use when presenting the data to humans. This Comparator
   * orders SymbolData objects by their class names or JSNI idents.
   */
  class ClassIdentComparator implements Comparator<SymbolData>, Serializable {
    public int compare(SymbolData o1, SymbolData o2) {
      String s1 = o1.isClass() ? o1.getClassName() : o1.getJsniIdent();
      String s2 = o2.isClass() ? o2.getClassName() : o2.getJsniIdent();
      return s1.compareTo(s2);
    }
  }

  /**
   * Returns the name of the type or enclosing type if the symbol is a method or
   * field.
   */
  String getClassName();

  /**
   * Returns a JSNI-like identifier for the symbol if it a method or field,
   * otherwise <code>null</code>.
   */
  String getJsniIdent();

  /**
   * Returns the name of the member if the symbol is a method or field.
   */
  String getMemberName();

  /**
   * Returns the line number on which the symbol was originally declared or
   * <code>-1</code> if the line number is unknown.
   */
  int getSourceLine();

  /**
   * Returns a URI string representing the location of the source. This method
   * will return <code>null</code> if the symbol was derived from a transient
   * or unknown source.
   */
  String getSourceUri();

  /**
   * Returns the JavaScript symbol this data maps to.
   */
  String getSymbolName();

  /**
   * Returns <code>true</code> if the symbol represents a class.
   */
  boolean isClass();

  /**
   * Returns <code>true</code> if the symbol represents a field.
   */
  boolean isField();

  /**
   * Returns <code>true</code> if the symbol represents a method.
   */
  boolean isMethod();
}
