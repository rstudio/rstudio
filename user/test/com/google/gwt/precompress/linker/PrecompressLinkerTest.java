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
package com.google.gwt.precompress.linker;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.ConfigurationProperty;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.EmittedArtifact.Visibility;
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.SyntheticArtifact;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Tests {@link PrecompressLinker}.
 */
public class PrecompressLinkerTest extends TestCase {
  private static class MockConfigurationProperty implements
      ConfigurationProperty, Comparable<MockConfigurationProperty> {
    private boolean hasMultipleValues;
    private String name;
    private List<String> values = new ArrayList<String>();

    public MockConfigurationProperty(String name, boolean hasMultipleValues) {
      this.name = name;
      this.hasMultipleValues = hasMultipleValues;
    }

    public int compareTo(MockConfigurationProperty o) {
      return getName().compareTo(o.getName());
    }

    public String getName() {
      return name;
    }

    @Deprecated
    public String getValue() {
      return values.get(0);
    }

    public List<String> getValues() {
      return values;
    }

    public boolean hasMultipleValues() {
      return hasMultipleValues;
    }

    public void setValue(String value) {
      values.clear();
      values.add(value);
    }
  }

  private class MockLinkerContext implements LinkerContext {
    public SortedSet<ConfigurationProperty> getConfigurationProperties() {
      return new TreeSet<ConfigurationProperty>(Arrays.asList(
          propLeaveOriginals, propPathRegexes));
    }

    public String getModuleFunctionName() {
      return "MockModule";
    }

    public long getModuleLastModified() {
      return 0;
    }

    public String getModuleName() {
      return "MockModule";
    }

    public SortedSet<SelectionProperty> getProperties() {
      return new TreeSet<SelectionProperty>();
    }

    public boolean isOutputCompact() {
      return true;
    }

    public String optimizeJavaScript(TreeLogger logger, String jsProgram) {
      return jsProgram;
    }
  }

  private static void assertEqualBytes(byte[] expected, byte[] actual) {
    assertEquals(expected.length, actual.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], actual[i]);
    }
  }

  private static byte[] compress(byte[] content) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      GZIPOutputStream gzip = new GZIPOutputStream(baos);
      InputStream in = new ByteArrayInputStream(content);

      byte[] buf = new byte[10000];
      int n;
      while ((n = in.read(buf)) > 0) {
        gzip.write(buf, 0, n);
      }
      gzip.close();

      return baos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(
          "Unexpected IO exception from memory operations");
    }
  }

  private static byte[] contents(EmittedArtifact art)
      throws UnableToCompleteException, IOException {
    InputStream input = art.getContents(TreeLogger.NULL);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    byte[] buf = new byte[10000];
    int n;
    while ((n = input.read(buf)) > 0) {
      baos.write(buf, 0, n);
    }

    return baos.toByteArray();
  }

  private static byte[] decompress(byte[] compressed) throws IOException {
    GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(
        compressed));
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buf = new byte[10000];
    int n;
    while ((n = gzip.read(buf)) > 0) {
      baos.write(buf, 0, n);
    }

    return baos.toByteArray();
  }

  private static SyntheticArtifact emit(String path, byte[] content) {
    return new SyntheticArtifact(PrecompressLinker.class, path, content);
  }

  private static SyntheticArtifact emit(String path, String contents) {
    try {
      return emit(path, contents.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  private static SyntheticArtifact emitPrivate(String string, String contents) {
    SyntheticArtifact art = emit(string, contents);
    art.setVisibility(Visibility.Private);
    return art;
  }

  private static EmittedArtifact findArtifact(ArtifactSet artifacts, String path) {
    for (EmittedArtifact art : artifacts.find(EmittedArtifact.class)) {
      if (art.getPartialPath().equals(path)) {
        return art;
      }
    }

    return null;
  }

  /**
   * Return a highly compressible string.
   */
  private static String fooFileContents() {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < 1000; i++) {
      buf.append("another identical line\n");
    }
    return buf.toString();
  }

  private static byte[] uncompressibleContent() {
    try {
      byte[] content = fooFileContents().getBytes("UTF-8");
      while (true) {
        byte[] updated = compress(content);
        if (updated.length >= content.length) {
          return content;
        }
        content = updated;
      }
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  private ArtifactSet artifacts;
  private LinkerContext context = new MockLinkerContext();

  private MockConfigurationProperty propLeaveOriginals;

  private MockConfigurationProperty propPathRegexes;

  /**
   * Test that foo.js gets compressed to foo.js.gz, and bar.js is left alone.
   */
  public void testBasics() throws UnableToCompleteException, IOException {
    ArtifactSet updated = linkArtifacts();

    EmittedArtifact foo = findArtifact(updated, "foo.js");
    assertNotNull(foo);

    EmittedArtifact fooGz = findArtifact(updated, "foo.js.gz");
    assertNotNull(fooGz);
    assertEqualBytes(contents(foo), decompress(contents(fooGz)));

    EmittedArtifact barGz = findArtifact(updated, "bar.js.gz");
    assertNull("bar.js is private and should not have been compressed", barGz);

    EmittedArtifact uncompressibleGz = findArtifact(updated,
        "uncompressible.js.gz");
    assertNull(
        "uncompressible.js is not compressible and should have been left alone",
        uncompressibleGz);
  }

  /**
   * Test that the blacklist takes effect.
   */
  public void testBlackList() throws UnableToCompleteException {
    propPathRegexes.values.add("-foo\\.js");
    ArtifactSet updated = linkArtifacts();

    // foo.txt is not in the list of patterns, so don't compress
    EmittedArtifact stuffGz = findArtifact(updated, "stuff.txt.gz");
    assertNull("stuff.txt should not have been compressed", stuffGz);

    // foo.js matches two regexes; the last should win
    EmittedArtifact fooGz = findArtifact(updated, "foo.js.gz");
    assertNull("foo.js should not have been compressed", fooGz);
  }

  /**
   * Tests that if precompress.leave.original if false, the originals are
   * removed.
   */
  public void testRemovingOriginals() throws UnableToCompleteException {
    propLeaveOriginals.setValue("false");
    ArtifactSet updated = linkArtifacts();
    EmittedArtifact foo = findArtifact(updated, "foo.js");
    assertNull("foo.js should have been removed", foo);
  }

  @Override
  protected void setUp() {
    // add some artifacts to test with
    artifacts = new ArtifactSet();
    artifacts.add(emit("foo.js", fooFileContents()));
    artifacts.add(emitPrivate("bar.js", fooFileContents()));
    artifacts.add(emit("uncompressible.js", uncompressibleContent()));
    artifacts.add(emit("stuff.txt", fooFileContents()));
    artifacts.add(emit("data.xml", fooFileContents()));
    artifacts.freeze();

    propLeaveOriginals = new MockConfigurationProperty(
        "precompress.leave.originals", false);
    propLeaveOriginals.setValue("true");

    propPathRegexes = new MockConfigurationProperty("precompress.path.regexes",
        true);
    propPathRegexes.values.add(".*\\.html");
    propPathRegexes.values.add(".*\\.js");
    propPathRegexes.values.add(".*\\.css");
  }

  private ArtifactSet linkArtifacts() throws UnableToCompleteException {
    return new PrecompressLinker().link(TreeLogger.NULL, context, artifacts,
        true);
  }
}
