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
package com.google.gwt.validation;

import com.google.gwt.validation.client.impl.ConstraintViolationImplTest;
import com.google.gwt.validation.client.impl.NodeImplTest;
import com.google.gwt.validation.client.impl.PathImplTest;
import com.google.gwt.validation.client.impl.metadata.ValidationGroupsMetadataTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * All validation client non GWT tests.
 */
public class ValidationClientJreSuite {

  public static Test suite() {
    TestSuite suite = new TestSuite(
        "Test suite for validation client code that does not require GWT.");
    suite.addTestSuite(PathImplTest.class);
    suite.addTestSuite(NodeImplTest.class);
    suite.addTestSuite(ValidationGroupsMetadataTest.class);
    suite.addTestSuite(ConstraintViolationImplTest.class);
    return suite;
  }
}
