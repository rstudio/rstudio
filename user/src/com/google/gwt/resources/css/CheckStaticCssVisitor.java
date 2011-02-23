/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.resources.css;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.resources.css.ast.CssNode;
import com.google.gwt.resources.css.ast.CssStylesheet;
import com.google.gwt.resources.css.ast.CssVisitor;

import java.util.List;

/**
* Check a stylesheet to ensure that all of its components are
* statically-evaluable.
*/
public class CheckStaticCssVisitor extends CssVisitor {

 /**
  * A fast-fail check to determine if a stylesheet is statically-evaluable.
  */
 public static boolean isStatic(CssStylesheet sheet) {
   return new CheckStaticCssVisitor(TreeLogger.NULL, true).execImpl(sheet);
 }

 /**
  * Returns <code>true</code> if the stylsheet is statically-evaluable. Nodes
  * that are not statically-evaluable will be reported to the associated
  * logger.
  */
 public static boolean report(TreeLogger logger, CssStylesheet sheet) {
   return new CheckStaticCssVisitor(logger, false).execImpl(sheet);
 }

 private boolean error;
 private final boolean fastFail;
 private final TreeLogger logger;

 private CheckStaticCssVisitor(TreeLogger logger, boolean fastFail) {
   this.logger = logger.branch(TreeLogger.DEBUG,
       "Checking external stylesheet for dynamic content");
   this.fastFail = fastFail;
 }

 @Override
 protected void doAccept(List<? extends CssNode> list) {
   for (CssNode node : list) {
     if (error && fastFail) {
       return;
     }
     doAccept(node);
   }
 }

 @Override
 protected <T extends CssNode> T doAccept(T node) {
   if (!node.isStatic()) {
     error(node);
   }

   if (error && fastFail) {
     // Just stop
     return node;
   } else {
     return super.doAccept(node);
   }
 }

 @Override
 protected void doAcceptWithInsertRemove(List<? extends CssNode> list) {
   doAccept(list);
 }

 void error(CssNode node) {
   logger.log(TreeLogger.ERROR, "The CSS node " + node.toString()
       + " cannot be statically evaluated");
   error = true;
 }

 private boolean execImpl(CssStylesheet sheet) {
   accept(sheet);
   return !error;
 }
}
