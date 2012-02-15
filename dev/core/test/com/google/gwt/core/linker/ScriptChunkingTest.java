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

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ConfigurationProperty;
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.StatementRanges;
import com.google.gwt.core.ext.linker.impl.SelectionScriptLinker;
import com.google.gwt.core.ext.linker.impl.StandardStatementRanges;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Tests the script chunking in the {@link SelectionScriptLinker}.
 */
public class ScriptChunkingTest extends TestCase {

  private static class MockLinkerContext implements LinkerContext {

    @Override
    public SortedSet<ConfigurationProperty> getConfigurationProperties() {
      return new TreeSet<ConfigurationProperty>();
    }

    @Override
    public String getModuleFunctionName() {
      return "mockFunc";
    }

    @Override
    public long getModuleLastModified() {
      return 0;
    }

    @Override
    public String getModuleName() {
      return "mock";
    }

    @Override
    public SortedSet<SelectionProperty> getProperties() {
      SelectionProperty mockSourceMapProperty = new SelectionProperty() {

        @Override
        public String getName() {
          return SelectionScriptLinker.USE_SOURCE_MAPS_PROPERTY;
        }

        @Override
        public String getFallbackValue() {
          return "";
        }

        @Override
        public SortedSet<String> getPossibleValues() {
          return new TreeSet<String>(Arrays.asList("true", "false"));
        }

        @Override
        public String getPropertyProvider(TreeLogger logger,
            SortedSet<ConfigurationProperty> configProperties) {
          return null;
        }

        @Override
        public boolean isDerived() {
          return false;
        }

        @Override
        public String tryGetValue() {
          return "false";
        }
      };

      Comparator<SelectionProperty> comparator = new Comparator<SelectionProperty>() {
        @Override
        public int compare(SelectionProperty first, SelectionProperty second) {
          return first.getName().compareTo(second.getName());
        }
      };
      
      TreeSet<SelectionProperty> result = new TreeSet<SelectionProperty>(comparator);
      result.add(mockSourceMapProperty);
      return result;
    }

    @Override
    public boolean isOutputCompact() {
      return false;
    }

    @Override
    public String optimizeJavaScript(TreeLogger logger, String jsProgram)
        throws UnableToCompleteException {
      return "";
    }
  }

  /**
   * A class for building up JavaScript that has statements and non-statement code interleaved.
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

    String split = SelectionScriptLinker.splitPrimaryJavaScript(builder.getRanges(),
        builder.getJavaScript(), stmt1.length() + stmt2.length(),
        IFrameLinker.SCRIPT_CHUNK_SEPARATOR, new MockLinkerContext());
    assertEquals(stmt1 + stmt2 + IFrameLinker.SCRIPT_CHUNK_SEPARATOR + stmt3
        + ';' + stmt4, split);
  }


  /**
   * Test that with the default chunk separator (""), splitting is a no-op.
   */
  public void testEmptyStringSeparator() {
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

    String split = SelectionScriptLinker.splitPrimaryJavaScript(builder.getRanges(),
        builder.getJavaScript(), stmt1.length() + stmt2.length(), "", new MockLinkerContext());
    assertEquals(stmt1 + stmt2 + stmt3 + ';' + stmt4, split);
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

    String split = SelectionScriptLinker.splitPrimaryJavaScript(builder.getRanges(),
        builder.getJavaScript(), 10000, IFrameLinker.SCRIPT_CHUNK_SEPARATOR,
        new MockLinkerContext());
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

    String split = SelectionScriptLinker.splitPrimaryJavaScript(builder.getRanges(),
        builder.getJavaScript(), -1, IFrameLinker.SCRIPT_CHUNK_SEPARATOR, new MockLinkerContext());
    assertEquals(builder.getJavaScript(), split);
  }

  /**
   * Test with statement ranges not present, which should disable the chunking.
   */
  public void testNullStatementRanges() {
    ScriptWithRangesBuilder builder = new ScriptWithRangesBuilder();
    builder.addNonStatement("{");
    builder.addNonStatement("{");
    builder.addStatement("x=1;");
    builder.addStatement("function x(){x = 2}\n");
    builder.addStatement("x=3");
    builder.addNonStatement("}\n{");
    builder.addStatement("x=5");
    builder.addNonStatement("}");

    String split = SelectionScriptLinker.splitPrimaryJavaScript(null,
        builder.getJavaScript(), 5, IFrameLinker.SCRIPT_CHUNK_SEPARATOR, new MockLinkerContext());
    assertEquals(builder.getJavaScript(), split);
  }
}
