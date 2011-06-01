// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.gwt.sample.core.linker;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.ConfigurationProperty;
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.SyntheticArtifact;
import com.google.gwt.core.ext.linker.impl.SelectionInformation;
import com.google.gwt.sample.core.linker.SimpleAppCacheLinker;

import junit.framework.TestCase;

import java.io.InputStream;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Tests {@link SimpleAppCacheLinker}
 */
public class SimpleAppCacheLinkerTest extends TestCase {
  private ArtifactSet artifacts;
  private TreeLogger logger;
  
  @Override
  public void setUp() {
    artifacts = new ArtifactSet();
    artifacts.add(new SelectionInformation("foo", 0, new TreeMap<String, String>()));
    logger = TreeLogger.NULL;
  }

  public void testAddCachableArtifacts() throws UnableToCompleteException {
    SimpleAppCacheLinker linker = new SimpleAppCacheLinker();

    // Some cacheable artifact
    artifacts.add(new SyntheticArtifact(SimpleAppCacheLinker.class, "foo.bar", new byte[0]));

    ArtifactSet result = linker.link(logger, new MockLinkerContext(), artifacts, false);

    assertEquals(3, result.size());
    assertHasOneManifest(result);
    assertTrue(getManifestContents(result).contains("foo.bar"));
  }

  public void testNoNonCachableArtifacts() throws UnableToCompleteException {
    SimpleAppCacheLinker linker = new SimpleAppCacheLinker();

    // Some non-cacheable artifacts
    artifacts.add(new SyntheticArtifact(SimpleAppCacheLinker.class, "foo.symbolMap", new byte[0]));
    artifacts.add(new SyntheticArtifact(SimpleAppCacheLinker.class, "foo.xml.gz", new byte[0]));
    artifacts.add(new SyntheticArtifact(SimpleAppCacheLinker.class, "foo.rpc.log", new byte[0]));
    artifacts.add(new SyntheticArtifact(SimpleAppCacheLinker.class, "foo.gwt.rpc", new byte[0]));
    artifacts.add(new SyntheticArtifact(SimpleAppCacheLinker.class, "rpcPolicyManifest.bar", new byte[0]));

    ArtifactSet result = linker.link(TreeLogger.NULL, new MockLinkerContext(), artifacts, false);

    assertEquals(7, result.size());
    assertHasOneManifest(result);
    assertFalse(getManifestContents(result).contains("symbolMap"));
    assertFalse(getManifestContents(result).contains("xml.gz"));
    assertFalse(getManifestContents(result).contains("rpc.log"));
    assertFalse(getManifestContents(result).contains("gwt.rpc"));
    assertFalse(getManifestContents(result).contains("rpcPolicyManifest"));
  }

  public void testAddStaticFiles() throws UnableToCompleteException {
    SimpleAppCacheLinker linker = new OneStaticFileAppCacheLinker();

    ArtifactSet result = linker.link(logger, new MockLinkerContext(), artifacts, false);

    assertEquals(2, result.size());
    assertHasOneManifest(result);
    assertTrue(getManifestContents(result).contains("aStaticFile"));
  }

  public void testEmptyManifestDevMode() throws UnableToCompleteException {
    // No SelectionInformation artifact
    artifacts = new ArtifactSet();
    
    SimpleAppCacheLinker linker = new SimpleAppCacheLinker();

    // Some cacheable artifact
    artifacts.add(new SyntheticArtifact(SimpleAppCacheLinker.class, "foo.bar", new byte[0]));

    ArtifactSet result = linker.link(logger, new MockLinkerContext(), artifacts, false);

    assertHasOneManifest(result);
    assertFalse(getManifestContents(result).contains("foo.bar"));
  }

  public void testManifestOnlyOnLastPass() throws UnableToCompleteException {
    SimpleAppCacheLinker linker = new SimpleAppCacheLinker();

    ArtifactSet result = linker.link(logger, new MockLinkerContext(), artifacts, true);

    assertEquals(artifacts, result);
    
    result = linker.link(logger, new MockLinkerContext(), artifacts, false);
    
    assertEquals(2, result.size());
    assertHasOneManifest(result);
  }
  
  private void assertHasOneManifest(ArtifactSet artifacts) {
    int manifestCount = 0;
    for (SyntheticArtifact artifact : artifacts.find(SyntheticArtifact.class)) {
      if ("appcache.nocache.manifest".equals(artifact.getPartialPath())) {
        assertEquals("appcache.nocache.manifest", artifact.getPartialPath());
        manifestCount++;
      }
    }
    assertEquals(1, manifestCount);
  }
  
  private SyntheticArtifact getManifest(ArtifactSet artifacts) {
    for (SyntheticArtifact artifact : artifacts.find(SyntheticArtifact.class)) {
      if ("appcache.nocache.manifest".equals(artifact.getPartialPath())) {
        assertEquals("appcache.nocache.manifest", artifact.getPartialPath());
        return artifact;
      }
    }
    fail("Manifest not found");
    return null;
  }
  
  private String getManifestContents(ArtifactSet artifacts) throws UnableToCompleteException {
    return getArtifactContents(getManifest(artifacts));
  }

  private String getArtifactContents(SyntheticArtifact artifact) throws UnableToCompleteException {
    InputStream is = artifact.getContents(logger);
    String contents = new Scanner(is).useDelimiter("\\A").next();
    return contents;
  }

  public static class OneStaticFileAppCacheLinker extends SimpleAppCacheLinker {
    
    @Override
    protected String[] otherCachedFiles() {
      return new String[] {"aStaticFile"};
    }
  }
  
  private static class MockLinkerContext implements LinkerContext {

    public SortedSet<ConfigurationProperty> getConfigurationProperties() {
      return new TreeSet<ConfigurationProperty>();
    }

    public String getModuleFunctionName() {
      return null;
    }

    public long getModuleLastModified() {
      return 0;
    }

    public String getModuleName() {
      return null;
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
}
