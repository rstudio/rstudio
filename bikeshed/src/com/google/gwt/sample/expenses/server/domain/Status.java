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
package com.google.gwt.sample.expenses.server.domain;

/**
 * Expense report life cycle states.
 */
public enum Status {
  Draft, // -> submitted
  Submitted,  // -> approved_manager -> declined
  Approved_Manager,  // -> approved_accounting
  Approved_Accounting,  // -> paid -> declined
  Paid,  // -> finish
  Declined // -> draft
}
