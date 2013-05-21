/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.core.shared.impl;

import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.shared.SerializableThrowable;

/**
 * Helper to resolve the designated type for {@link SerializableThrowable}. If no class metadata
 * available then it resolves best effort class name by checking against some common exception
 * types.
 */
@GwtScriptOnly
public class ThrowableTypeResolver {

  public static void resolveDesignatedType(SerializableThrowable throwable, Throwable designated) {
    if (Class.isClassMetadataEnabled()) {
      throwable.setDesignatedType(designated.getClass().getName(), true);
    } else {
      String resolvedName;
      Class<?> resolvedType;
      try {
        throw designated;
      } catch (NullPointerException e) {
        resolvedName = "java.lang.NullPointerException";
        resolvedType = NullPointerException.class;
      } catch (JavaScriptException e) {
        resolvedName = "com.google.gwt.core.client.JavaScriptException";
        resolvedType = JavaScriptException.class;
      } catch (RuntimeException e) {
        resolvedName = "java.lang.RuntimeException";
        resolvedType = RuntimeException.class;
      } catch (Exception e) {
        resolvedName = "java.lang.Exception";
        resolvedType = Exception.class;
      } catch (Error e) {
        resolvedName = "java.lang.Error";
        resolvedType = Error.class;
      } catch (Throwable e) {
        resolvedName = "java.lang.Throwable";
        resolvedType = Throwable.class;
      }
      throwable.setDesignatedType(resolvedName, resolvedType == designated.getClass());
    }
  }
}
