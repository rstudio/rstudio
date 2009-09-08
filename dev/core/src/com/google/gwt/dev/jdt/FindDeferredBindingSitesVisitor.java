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
package com.google.gwt.dev.jdt;

import com.google.gwt.dev.javac.GWTProblem;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.Scope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Walks a
 * {@link org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration} to
 * find <code>GWT.create()</code> class so that we can eagerly complain about
 * deferred binding problems.
 */
public class FindDeferredBindingSitesVisitor extends ASTVisitor {

  /**
   * Information about the site at which a rebind request was found, used to
   * report problems.
   */
  public static class MessageSendSite {
    public final MessageSend messageSend;

    public final Scope scope;

    public MessageSendSite(MessageSend messageSend, Scope scope) {
      this.messageSend = messageSend;
      this.scope = scope;
    }
  }

  public static final String MAGIC_CLASS = "com.google.gwt.core.client.GWT";
  public static final String REBIND_MAGIC_METHOD = "create";
  public static final String ASYNC_MAGIC_METHOD = "runAsync";

  public static void reportRebindProblem(MessageSendSite site, String message) {
    MessageSend messageSend = site.messageSend;
    Scope scope = site.scope;
    // Safe since CUS.referenceContext is set in its constructor.
    CompilationUnitDeclaration cud = scope.compilationUnitScope().referenceContext;
    GWTProblem.recordInCud(messageSend, cud, message, null);
  }

  private final Map<String, MessageSendSite> results = new HashMap<String, MessageSendSite>();

  private final List<MessageSendSite> runAsyncCalls = new ArrayList<MessageSendSite>();

  @Override
  public void endVisit(MessageSend messageSend, BlockScope scope) {
    if (messageSend.binding == null) {
      // Some sort of problem.
      return;
    }

    String methodName = String.valueOf(messageSend.selector);
    boolean rebindMagicMethod = methodName.equals(REBIND_MAGIC_METHOD);
    boolean asyncMagicMethod = methodName.equals(ASYNC_MAGIC_METHOD);
    if (!(rebindMagicMethod || asyncMagicMethod)) {
      // Not the create() method or the runAsync() method.
      return;
    }

    char[][] targetClass = messageSend.binding.declaringClass.compoundName;
    String targetClassName = CharOperation.toString(targetClass);
    if (!targetClassName.equals(MAGIC_CLASS)) {
      // Not being called on the Rebind class.
      return;
    }

    MessageSendSite site = new MessageSendSite(messageSend, scope);

    Expression[] args = messageSend.arguments;
    if (rebindMagicMethod) {
      if (args.length != 1) {
        reportRebindProblem(site,
            "GWT.create() should take exactly one argument");
        return;
      }

      if (!(args[0] instanceof ClassLiteralAccess)) {
        reportRebindProblem(site,
            "Only class literals may be used as arguments to GWT.create()");
        return;
      }
    } else {
      assert asyncMagicMethod;
      if (args.length != 1 && args.length != 2) {
        reportRebindProblem(site,
            "GWT.runAsync() should take one or two arguments");
        return;
      }
      if (args.length == 2) {
        if (!(args[0] instanceof ClassLiteralAccess)) {
          reportRebindProblem(site,
              "Only class literals may be used to name a call to GWT.runAsync()");
          return;
        }
      }
    }

    if (asyncMagicMethod) {
      runAsyncCalls.add(new MessageSendSite(messageSend, scope));
      return;
    }

    ClassLiteralAccess cla = (ClassLiteralAccess) args[0];
    String typeName = String.valueOf(cla.targetType.readableName());

    if (!results.containsKey(typeName)) {
      results.put(typeName, site);
    }
  }

  /**
   * Return the calls to GWT.runAsync() that were seen.
   */
  public List<MessageSendSite> getRunAsyncSites() {
    return runAsyncCalls;
  }

  public Map<String, MessageSendSite> getSites() {
    return Collections.unmodifiableMap(results);
  }
}
