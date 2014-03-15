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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.TreeLogger;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link CompilationUnitInvalidator}.
 */
public class CompilationUnitInvalidatorTest extends TestCase {

  public void testRetainValidUnits() {
    /*
     * bad0 is a bad unit in validClasses
     * bad1 is directly a bad unit
     * bad2 depends on bad0
     * bad3 depends on bad1
     * bad4 has a missing dependency
     * bad5 depends on good0, good1, bad3
     * bad6 depends on bad5
     *
     * good0 is a good member of validClasses
     * good1 depends on good0
     * good2 depends on good1
     * good3 depends on good0, good1, good2
     */

    CompilationUnit bad0 = getUnitWithApiRefs(true, "bad0");
    CompilationUnit bad1 = getUnitWithApiRefs(true, "bad1");
    CompilationUnit bad2 = getUnitWithApiRefs(false, "bad2", "bad0");
    CompilationUnit bad3 = getUnitWithApiRefs(false, "bad3", "bad1");
    CompilationUnit bad4 = getUnitWithApiRefs(false, "bad4", "missing0");
    CompilationUnit bad5 = getUnitWithApiRefs(false,
        "bad5", "good0", "good1", "bad3");
    CompilationUnit bad6 = getUnitWithApiRefs(false, "bad6", "bad5");

    CompilationUnit good0 = getUnitWithApiRefs(false, "good0");
    CompilationUnit good1 = getUnitWithApiRefs(false, "good1", "good0");
    CompilationUnit good2 = getUnitWithApiRefs(false, "good2", "good1");
    CompilationUnit good3 = getUnitWithApiRefs(false, "good3",
        "good0", "good1", "good2");

    Collection<CompilationUnit> units = new ArrayList<CompilationUnit>();
    units.addAll(Arrays.asList(
        bad1, bad2, bad3, bad4, bad5, bad6,
        good1, good2, good3));

    Map<String, CompiledClass> validClasses = new HashMap<String, CompiledClass>();
    validClasses.put("bad0", bad0.getCompiledClasses().iterator().next());
    validClasses.put("good0", good0.getCompiledClasses().iterator().next());

    // At least some of the members of units also appear within validClasses
    // By putting bad2 here, this test ensures that bad3 (and dependents)
    // will be removed even though bad2 is in validClasses.
    validClasses.put("bad2", bad2.getCompiledClasses().iterator().next());
    validClasses.put("good1", good1.getCompiledClasses().iterator().next());

    // Keep a copy so that we can check that the one passed-in isn't mutated
    Map<String, CompiledClass> knownValidClasses =
        new HashMap<String, CompiledClass>(validClasses);

    // Invoke the method under test
    CompilationUnitInvalidator.retainValidUnits(TreeLogger.NULL, units, validClasses);

    // Check that validClasses is not mutated
    assertEquals(knownValidClasses, validClasses);

    // Check that units is mutated
    assertEquals(Arrays.asList(good1, good2, good3), units);
  }

  private static CompilationUnit getUnitWithApiRefs(
      final boolean isError, final String simpleName, final String... apiRefs) {
    return new MockCompilationUnit(simpleName, simpleName) {
      @Override
      public boolean isError() {
        return isError;
      }

      @Override
      public Collection<CompiledClass> getCompiledClasses() {
        CompiledClass cc = new CompiledClass(new byte[1], null, false, simpleName, simpleName);
        cc.initUnit(this);
        return Collections.<CompiledClass>singletonList(cc);
      }

      @Override
      public Dependencies getDependencies() {
        return new Dependencies() {
          @Override
          public List<String> getApiRefs() {
            return Arrays.asList(apiRefs);
          }
        };
      }
    };
  }
}
