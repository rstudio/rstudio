/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.cfg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.cfg.test.caseinsensitive.GOO;
import com.google.gwt.dev.cfg.test.caseinsensitive.Good;
import com.google.gwt.dev.cfg.test.casesensitive.CaseSensitive_A_Foo;
import com.google.gwt.dev.cfg.test.casesensitive.CaseSensitive_a_Bar;
import com.google.gwt.dev.cfg.test.excludes.Excludes_Exclude1;
import com.google.gwt.dev.cfg.test.excludes.Excludes_Exclude2;
import com.google.gwt.dev.cfg.test.excludes.Excludes_Exclude3;
import com.google.gwt.dev.cfg.test.excludes.Excludes_Include1;
import com.google.gwt.dev.cfg.test.excludes.Excludes_Include2;
import com.google.gwt.dev.cfg.test.excludes.Excludes_Include3;
import com.google.gwt.dev.cfg.test.includeexclude.IncludeExclude_Exclude1;
import com.google.gwt.dev.cfg.test.includeexclude.IncludeExclude_Exclude2;
import com.google.gwt.dev.cfg.test.includeexclude.IncludeExclude_Exclude3;
import com.google.gwt.dev.cfg.test.includeexclude.IncludeExclude_Include1;
import com.google.gwt.dev.cfg.test.includeexclude.IncludeExclude_Include2;
import com.google.gwt.dev.cfg.test.includes.Includes_Exclude1;
import com.google.gwt.dev.cfg.test.includes.Includes_Exclude2;
import com.google.gwt.dev.cfg.test.includes.Includes_Exclude3;
import com.google.gwt.dev.cfg.test.includes.Includes_Include1;
import com.google.gwt.dev.cfg.test.includes.Includes_Include2;
import com.google.gwt.dev.cfg.test.includes.Includes_Include3;
import com.google.gwt.dev.cfg.test.recursive.bar.Recursive_Excluded1;
import com.google.gwt.dev.cfg.test.recursive.good.Recursive_Include1;
import com.google.gwt.dev.cfg.test.recursive.good.bar.Recursive_Excluded2;
import com.google.gwt.dev.cfg.test.recursive.good.bar.good.Recursive_Include2;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.io.PrintWriter;

/**
 * Common test code for testing the various permutations of GWT module's
 * &lt;source&gt; and &lt;super-source&gt; source tags, specifically their
 * ant-like inclusion support.
 */
public abstract class TestSuperAndSourceTags extends TestCase {
  private static TreeLogger getRootLogger() {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(new PrintWriter(
        System.err, true));
    logger.setMaxDetail(TreeLogger.ERROR);
    return logger;
  }

  private final ModuleDef moduleDef;

  public TestSuperAndSourceTags() throws UnableToCompleteException {
    // Module has the same name as this class.
    String moduleName = getClass().getCanonicalName();
    moduleDef = ModuleDefLoader.loadFromClassPath(getRootLogger(), moduleName);
  }

  /**
   * Returns the logical path for a class. This method is implemented by the
   * subclasses because source and super-source compute logical paths
   * differently.
   */
  protected abstract String getLogicalPath(Class<?> clazz);

  /**
   * Validate that the source or super-source tags .
   */
  protected void validateTags() {
    // Test case insensitive
    validateIncluded(Good.class);
    validateIncluded(GOO.class);

    // Test case sensitive
    validateIncluded(CaseSensitive_A_Foo.class);
    validateExcluded(CaseSensitive_a_Bar.class);

    // Test excludes
    validateExcluded(Excludes_Exclude1.class);
    validateExcluded(Excludes_Exclude2.class);
    validateExcluded(Excludes_Exclude3.class);
    validateIncluded(Excludes_Include1.class);
    validateIncluded(Excludes_Include2.class);
    validateIncluded(Excludes_Include3.class);

    // Test include and exclude
    validateExcluded(IncludeExclude_Exclude1.class);
    validateExcluded(IncludeExclude_Exclude2.class);
    validateExcluded(IncludeExclude_Exclude3.class);
    validateIncluded(IncludeExclude_Include1.class);
    validateIncluded(IncludeExclude_Include2.class);

    // Test includes
    validateExcluded(Includes_Exclude1.class);
    validateExcluded(Includes_Exclude2.class);
    validateExcluded(Includes_Exclude3.class);
    validateIncluded(Includes_Include1.class);
    validateIncluded(Includes_Include2.class);
    validateIncluded(Includes_Include3.class);

    // Test recursive behavior
    validateExcluded(Recursive_Excluded1.class);
    validateExcluded(Recursive_Excluded2.class);
    validateIncluded(Recursive_Include1.class);
    validateIncluded(Recursive_Include2.class);
  }

  private void validateExcluded(Class<?> clazz) {
    assertNull(moduleDef.findSourceFile(toPath(clazz)));
  }

  private void validateIncluded(Class<?> clazz) {
    assertNotNull(moduleDef.findSourceFile(toPath(clazz)));
  }

  private String toPath(Class<?> clazz) {
    return getLogicalPath(clazz).replace('.', '/') + ".java";
  }
}
