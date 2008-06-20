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
package com.google.gwt.tools.apichecker;

import com.google.gwt.core.ext.typeinfo.JAbstractMethod;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates an API method. Useful for set-operations.
 */
final class ApiMethod extends ApiAbstractMethod {

  ApiMethod(JAbstractMethod method, ApiClass apiClass) {
    super(method, apiClass);
  }

  @Override
  ApiChange checkReturnTypeCompatibility(ApiAbstractMethod newMethod)
      throws TypeNotPresentException {
    JType firstType, secondType;
    if (newMethod.getMethod() instanceof JMethod && method instanceof JMethod) {
      firstType = ((JMethod) method).getReturnType();
      secondType = ((JMethod) newMethod.getMethod()).getReturnType();
    } else {
      throw new AssertionError("Different types for method = "
          + method.getClass() + ", and newMethodObject = "
          + newMethod.getMethod().getClass() + ", signature = "
          + getApiSignature());
    }
    StringBuffer sb = new StringBuffer();
    if (firstType.getSimpleSourceName().indexOf("void") != -1) {
      return null;
    }
    boolean compatible =
        ApiDiffGenerator.isFirstTypeAssignableToSecond(secondType, firstType);
    if (compatible) {
      return null;
    }
    sb.append("Return type changed from ");
    sb.append(firstType.getQualifiedSourceName());
    sb.append(" to ");
    sb.append(secondType.getQualifiedSourceName());
    return new ApiChange(this, ApiChange.Status.RETURN_TYPE_ERROR,
        sb.toString());
  }

  /*
   * check for: (i) added 'final' or 'abstract', (ii) removed 'static', adding
   * the 'static' keyword is fine.
   * 
   * A private, static, or final method can't be made 'abstract' (Java language
   * specification).
   */
  @Override
  List<ApiChange.Status> getModifierChanges(final ApiAbstractMethod newMethod) {
    JMethod newjmethod = null;
    JMethod oldjmethod = null;

    if (newMethod.getMethod() instanceof JMethod && method instanceof JMethod) {
      newjmethod = (JMethod) newMethod.getMethod();
      oldjmethod = (JMethod) method;
    } else {
      throw new AssertionError("Different types for method = "
          + method.getClass() + " and newMethod = "
          + newMethod.getMethod().getClass() + ", signature = "
          + getApiSignature());
    }
    List<ApiChange.Status> statuses = new ArrayList<ApiChange.Status>();
    if (!oldjmethod.isFinal() && newjmethod.isFinal()) {
      statuses.add(ApiChange.Status.FINAL_ADDED);
    }
    if (!oldjmethod.isAbstract() && newjmethod.isAbstract()) {
      statuses.add(ApiChange.Status.ABSTRACT_ADDED);
    }
    if ((oldjmethod.isStatic() && !newjmethod.isStatic())) {
      statuses.add(ApiChange.Status.STATIC_REMOVED);
    }
    return statuses;
  }
}
/*
 * final class TestB { static protected int i = 5; }
 * 
 * class TestC { public int j = TestB.i + 10; }
 */
