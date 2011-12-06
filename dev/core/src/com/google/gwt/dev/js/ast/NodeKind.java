/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.dev.js.ast;

/**
 * NodeKind used to simplify direct AST iteration.
 */
public enum NodeKind {
  ARRAY_ACCESS, ARRAY, BINARY_OP, BLOCK, BOOLEAN, BREAK, CASE, CATCH, CONDITIONAL, CONTINUE, DEBUGGER, DEFAULT, DO, EMPTY, EXPR_STMT, FOR, FOR_IN, FUNCTION, IF, INVOKE, LABEL, NAME_REF, NAME_OF, NEW, NULL, NUMBER, OBJECT, PARAMETER, POSTFIX_OP, PREFIX_OP, PROGRAM, PROGRAM_FRAGMENT, PROPERTY_INIT, REGEXP, RETURN, SEED_ID_OF, STRING, SWITCH, THIS, THROW, TRY, VARS, VAR, WHILE
}
