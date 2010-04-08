/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.sample.expenses.gwt.request;

import java.util.HashSet;
import java.util.Set;

/**
 * Gives implementors of {@link KeyProcessor} access to all instances of
 * {@link ExpensesKey} without hardcoding them.
 */
public class ExpensesKeyProcessor {

  /**
   * Implemented by objects interested in all {@link ExpensesKey} instances.
   */
  public interface KeyProcessor {
    void processKey(ExpensesKey<?> key);
  }

  private static Set<ExpensesKey<?>> instance;

  public static void processAll(KeyProcessor processor) {
    for (ExpensesKey<?> key : get()) {
      processor.processKey(key);
    }
  }

  private static Set<ExpensesKey<?>> get() {
    if (instance == null) {
      instance = new HashSet<ExpensesKey<?>>();
      instance.add(ReportKey.get());
      instance.add(EmployeeKey.get());
    }
    return instance;
  }
}
