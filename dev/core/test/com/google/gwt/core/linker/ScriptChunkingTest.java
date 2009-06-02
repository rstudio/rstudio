/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.core.linker;

import com.google.gwt.core.ext.linker.StatementRanges;
import com.google.gwt.core.ext.linker.impl.StandardStatementRanges;

import junit.framework.TestCase;

import java.util.ArrayList;

/**
 * Tests the script chunking in the {@link IFrameLinker}.
 */
public class ScriptChunkingTest extends TestCase {
  /**
   * A class for building up JavaScript that has statements and non-statement
   * code interleaved.
   */
  private static class ScriptWithRangesBuilder {
    private final ArrayList<Integer> ends = new ArrayList<Integer>();
    private final StringBuffer script = new StringBuffer();
    private final ArrayList<Integer> starts = new ArrayList<Integer>();

    public void addNonStatement(String string) {
      script.append(string);
    }

    public void addStatement(String string) {
      starts.add(script.length());
      script.append(string);
      ends.add(script.length());
    }

    public String getJavaScript() {
      return script.toString();
    }

    public StatementRanges getRanges() {
      return new StandardStatementRanges(starts, ends);
    }
  }

  public void testBasics() {
    ScriptWithRangesBuilder builder = new ScriptWithRangesBuilder();
    builder.addNonStatement("{");
    String stmt1 = "x=1;";
    builder.addStatement(stmt1);
    String stmt2 = "function x(){x = 2}\n";
    builder.addStatement(stmt2);
    String stmt3 = "x=3";
    /*
     * This one has no terminator, so the chunker must add one
     */
    builder.addStatement(stmt3);
    builder.addNonStatement("}\n{");
    String stmt4 = "x=5";
    builder.addStatement(stmt4);
    builder.addNonStatement("}");

    String split = IFrameLinker.splitPrimaryJavaScript(builder.getRanges(),
        builder.getJavaScript(), stmt1.length() + stmt2.length());
    assertEquals(stmt1 + stmt2 + IFrameLinker.SCRIPT_CHUNK_SEPARATOR + stmt3
        + ';' + stmt4, split);
  }

  /**
   * Test a chunk size large enough that no splitting happens.
   */
  public void testLongChunkSize() {
    ScriptWithRangesBuilder builder = new ScriptWithRangesBuilder();
    builder.addNonStatement("{");
    builder.addNonStatement("{");
    String stmt1 = "x=1;";
    builder.addStatement(stmt1);
    String stmt2 = "function x(){x = 2}\n";
    builder.addStatement(stmt2);
    String stmt3 = "x=3";
    builder.addStatement(stmt3);
    builder.addNonStatement("}\n{");
    String stmt4 = "x=5";
    builder.addStatement(stmt4);
    builder.addNonStatement("}");

    String split = IFrameLinker.splitPrimaryJavaScript(builder.getRanges(),
        builder.getJavaScript(), 10000);
    assertEquals(stmt1 + stmt2 + stmt3 + ';' + stmt4, split);
  }

  /**
   * Test with chunking disabled.
   */
  public void testNegativeChunkSize() {
    ScriptWithRangesBuilder builder = new ScriptWithRangesBuilder();
    builder.addNonStatement("{");
    builder.addNonStatement("{");
    builder.addStatement("x=1;");
    builder.addStatement("function x(){x = 2}\n");
    builder.addStatement("x=3");
    builder.addNonStatement("}\n{");
    builder.addStatement("x=5");
    builder.addNonStatement("}");

    String split = IFrameLinker.splitPrimaryJavaScript(builder.getRanges(),
        builder.getJavaScript(), -1);
    assertEquals(builder.getJavaScript(), split);
  }
}
