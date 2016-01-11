/*
 * Copyright 2015 Google Inc.
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

import com.google.gwt.dev.jjs.ast.HasJsInfo.JsMemberType;
import com.google.gwt.dev.jjs.ast.JMember;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.thirdparty.guava.common.base.Predicate;

/**
 * General predicates for Java AST nodes.
 */
public class JjsPredicates {
  public static final Predicate<JMethod> IS_JS_CONSTRUCTOR =
      new Predicate<JMethod>() {
        @Override
        public boolean apply(JMethod method) {
          return method.isConstructor() && method.getJsMemberType() == JsMemberType.CONSTRUCTOR;
        }
      };
  public static Predicate<JMember> IS_SYNTHETIC =
      new Predicate<JMember>() {
        @Override
        public boolean apply(JMember method) {
          return method.isSynthetic();
        }
      };

  public static Predicate<JMember> NEEDS_DYNAMIC_DISPATCH =
      new Predicate<JMember>() {
        @Override
        public boolean apply(JMember method) {
          return method.needsDynamicDispatch();
        }
      };

  private JjsPredicates() {
  }
}