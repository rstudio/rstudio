/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.core.ext.soyc;

import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.thirdparty.debugging.sourcemap.FilePosition;
import com.google.gwt.thirdparty.debugging.sourcemap.SourceMapConsumerV3;
import com.google.gwt.thirdparty.debugging.sourcemap.SourceMapConsumerV3.EntryVisitor;
import com.google.gwt.thirdparty.debugging.sourcemap.SourceMapGeneratorV3;
import com.google.gwt.thirdparty.debugging.sourcemap.SourceMapParseException;
import com.google.gwt.thirdparty.guava.common.base.Joiner;

import junit.framework.TestCase;

import java.io.IOException;

/**
 * Verifies that we consolidate source mappings.
 */
public class SourceMappingWriterTest extends TestCase {
  private static final String JAVA_FILENAME = "Hello.java";

  private SourceMapGeneratorV3 generator;
  private SourceMappingWriter writer;
  private String javascript;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    generator = new SourceMapGeneratorV3();
    writer = new SourceMappingWriter(generator);
  }

  public void testNoMappings() throws Exception {
    checkMappings();
  }

  public void testOneMapping() throws Exception {
    javascript = "foo.bar";
    addMapping("foo", 123);
    checkMappings("0:0-3 -> 123");
  }

  public void testMappingsToSeparateLines() throws Exception {
    javascript = "foo.bar";
    addMapping("foo", 123);
    addMapping(".bar", 456);
    checkMappings(
        "0:0-3 -> 123",
        "0:3-7 -> 456"
    );
  }

  public void testMappingsToSameLine() throws Exception {
    javascript = "foo.bar";
    addMapping("foo", 123);
    addMapping(".bar", 123);
    checkMappings(
        "0:0-7 -> 123"
    );
  }

  public void testOverlappingMappings() throws Exception {
    javascript = "foo.bar";
    addMapping("foo.bar", 123);
    addMapping("foo", 123);
    addMapping("bar", 123);
    checkMappings(
        "0:0-7 -> 123"
    );
  }

  public void testInlinedExpression() throws Exception {
    javascript = "foo.bar.baz";
    addMapping("foo.bar.baz", 123);
    addMapping(".bar", 456);
    checkMappings(
        "0:0-3 -> 123",
        "0:3-7 -> 456",
        "0:7-11 -> 123"
    );
  }

  private void addMapping(String substring, int javaStartLine) {
    SourceOrigin sourceInfo = SourceOrigin.create(javaStartLine, JAVA_FILENAME);
    writer.addMapping(findRange(substring, sourceInfo), null);
  }

  /**
   * Returns the appropriate range for the given substring of the "javascript" field.
   */
  private Range findRange(String substring, SourceOrigin sourceInfo) {
    assertFalse("multiline strings not implemented", substring.contains("\n"));

    int startPos = javascript.indexOf(substring);
    assertTrue("can't find javascript substring: " + substring, startPos >= 0);

    int startLine = 0;
    int startChar = 0;
    for (int c = 0; c < startPos; c++) {
      if (javascript.charAt(c) == '\n') {
        startLine++;
        startChar = 0;
      } else {
        startChar++;
      }
    }

    return new Range(startPos, startPos + substring.length(),
        startLine, startChar, startLine, startChar + substring.length(), sourceInfo);
  }

  private void checkMappings(String... lines) throws IOException, SourceMapParseException {
    String expected = "Mappings:\n";
    if (lines.length > 0) {
      expected += Joiner.on("\n").join(lines) + "\n";
    }
    assertEquals(expected, dumpMappings());
  }

  private String dumpMappings() throws IOException, SourceMapParseException {
    writer.flush();

    // Workaround: SourceMapConsumerV3 omits the last mapping. Add one more mapping until
    // this is fixed: https://code.google.com/p/closure-compiler/issues/detail?id=1311
    generator.addMapping("Dummy", null, new FilePosition(0, 0),
        new FilePosition(99, 0), new FilePosition(99, 1));

    StringBuilder generated = new StringBuilder();
    generator.appendTo(generated, "test");

    SourceMapConsumerV3 consumer = new SourceMapConsumerV3();
    consumer.parse(generated.toString());

    final StringBuilder out = new StringBuilder();
    out.append("Mappings:\n");

    consumer.visitMappings(new EntryVisitor() {
      @Override
      public void visit(String javaFile, String javaName, FilePosition javaStart,
          FilePosition jsStart, FilePosition jsEnd) {

        if (javaFile.equals("Dummy")) {
          return; // Ignore dummy mapping (in case the bug is fixed.)
        }

        // JavaScript range
        out.append(jsStart.getLine());
        out.append(":");
        out.append(jsStart.getColumn());
        out.append("-");
        if (jsEnd.getLine() != jsStart.getLine()) {
          out.append(jsEnd.getLine());
          out.append(":");
        }
        out.append(jsEnd.getColumn());

        out.append(" -> ");

        // Java target
        if (!javaFile.equals(JAVA_FILENAME)) {
          out.append(javaFile);
          out.append(":");
        }
        // print Java line using one-based line numbers to avoid confusion
        out.append(javaStart.getLine() + 1);
        if (javaStart.getColumn() != 0) {
          out.append(":");
          out.append(javaStart.getColumn());
        }
        if (javaName != null) {
          out.append(" (");
          out.append(javaName);
          out.append(")");
        }

        out.append("\n");
      }
    });

    return out.toString();
  }
}
