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
package com.google.gwt.sample.expenses.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import java.util.List;

/**
 * Generate random records.
 */
@RemoteServiceRelativePath("dataGeneration")
public interface DataGenerationService extends RemoteService {
  
  long countEmployees();
  
  long countExpenses();
  
  long countReports();
  
  /**
   * Delete the entire datastore.
   */
  void delete();
  
  /**
   * Generate reports for approximately the given number of milliseconds.
   */
  void generate(int millis);
  
  /**
   * @return the number of Employees, Reports, and Expenses.
   */
  List<Integer> getCounts();
  
  void resetCounters();
}
