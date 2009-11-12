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
package com.google.gwt.dev.javac;

import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.Util;

import org.eclipse.jdt.core.compiler.CategorizedProblem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Builds a {@link CompilationUnit}.
 */
public abstract class CompilationUnitBuilder {

  static class GeneratedCompilationUnitBuilder extends CompilationUnitBuilder {
    private final GeneratedUnit generatedUnit;

    public GeneratedCompilationUnitBuilder(GeneratedUnit generatedUnit) {
      this.generatedUnit = generatedUnit;
    }

    @Override
    public ContentId getContentId() {
      return new ContentId(getTypeName(), generatedUnit.getStrongHash());
    }

    @Override
    public String getLocation() {
      return getLocationFor(generatedUnit);
    }

    @Override
    public String getTypeName() {
      return generatedUnit.getTypeName();
    }

    @Override
    protected String doGetSource() {
      return generatedUnit.getSource();
    }

    @Override
    protected CompilationUnit makeUnit(List<CompiledClass> compiledClasses,
        Set<ContentId> dependencies,
        Collection<? extends JsniMethod> jsniMethods,
        CategorizedProblem[] problems) {
      return new GeneratedCompilationUnit(generatedUnit, compiledClasses,
          dependencies, jsniMethods, problems);
    }

    @Override
    boolean isGenerated() {
      return true;
    }
  }

  static class ResourceCompilationUnitBuilder extends CompilationUnitBuilder {
    /**
     * Not valid until source has been read.
     */
    private ContentId contentId;

    private long lastModifed = -1;

    private final Resource resource;

    private final String typeName;

    public ResourceCompilationUnitBuilder(Resource resource) {
      this(Shared.toTypeName(resource.getPath()), resource);
    }

    public ResourceCompilationUnitBuilder(String typeName, Resource resource) {
      this.resource = resource;
      this.typeName = typeName;
      assert typeName.equals(Shared.toTypeName(resource.getPath()));
    }

    @Override
    public ContentId getContentId() {
      return contentId;
    }

    public long getLastModifed() {
      if (lastModifed < 0) {
        return resource.getLastModified();
      } else {
        // Value when the source was actually read.
        return lastModifed;
      }
    }

    @Override
    public String getLocation() {
      return resource.getLocation();
    }

    public Resource getResource() {
      return resource;
    }

    @Override
    public String getTypeName() {
      return typeName;
    }

    @Override
    protected String doGetSource() {
      /*
       * Pin the mod date first to be conservative, we'd rather a unit be seen
       * as too stale than too fresh.
       */
      lastModifed = resource.getLastModified();
      InputStream in = resource.openContents();
      ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
      try {
        Util.copy(in, out);
      } catch (IOException e) {
        throw new RuntimeException("Unexpected error reading resource '"
            + resource + "'", e);
      }
      byte[] content = out.toByteArray();
      contentId = new ContentId(getTypeName(), Util.computeStrongName(content));
      return Util.toString(content);
    }

    @Override
    protected CompilationUnit makeUnit(List<CompiledClass> compiledClasses,
        Set<ContentId> dependencies,
        Collection<? extends JsniMethod> jsniMethods,
        CategorizedProblem[] problems) {
      return new SourceFileCompilationUnit(getResource(), contentId,
          compiledClasses, dependencies, jsniMethods, problems);
    }
  }

  private static final class GeneratedCompilationUnit extends
      CompilationUnitImpl {
    private final GeneratedUnit generatedUnit;

    public GeneratedCompilationUnit(GeneratedUnit generatedUnit,
        List<CompiledClass> compiledClasses, Set<ContentId> dependencies,
        Collection<? extends JsniMethod> jsniMethods,
        CategorizedProblem[] problems) {
      super(compiledClasses, dependencies, jsniMethods, problems);
      this.generatedUnit = generatedUnit;
    }

    @Override
    public String getDisplayLocation() {
      return getLocationFor(generatedUnit);
    }

    @Override
    public long getLastModified() {
      return generatedUnit.creationTime();
    }

    @Override
    public String getSource() {
      return generatedUnit.getSource();
    }

    @Override
    public String getTypeName() {
      return generatedUnit.getTypeName();
    }

    @Override
    public boolean isGenerated() {
      return true;
    }

    @Override
    public boolean isSuperSource() {
      return false;
    }

    @Override
    ContentId getContentId() {
      return new ContentId(getTypeName(), generatedUnit.getStrongHash());
    }
  }

  public static CompilationUnitBuilder create(GeneratedUnit generatedUnit) {
    return new GeneratedCompilationUnitBuilder(generatedUnit);
  }

  public static CompilationUnitBuilder create(Resource resource) {
    return new ResourceCompilationUnitBuilder(resource);
  }

  public static String makeContentId(String typeName, String strongHash) {
    return typeName + ':' + strongHash;
  }

  static String getLocationFor(GeneratedUnit generatedUnit) {
    String location = generatedUnit.optionalFileLocation();
    if (location != null) {
      return location;
    }
    return "generated://" + generatedUnit.getStrongHash() + "/"
        + Shared.toPath(generatedUnit.getTypeName());
  }

  /**
   * Caches source until JSNI methods can be collected.
   */
  private transient String source;

  protected CompilationUnitBuilder() {
  }

  public CompilationUnit build(List<CompiledClass> compiledClasses,
      Set<ContentId> dependencies,
      Collection<? extends JsniMethod> jsniMethods,
      CategorizedProblem[] problems) {
    // Free the source now.
    source = null;
    return makeUnit(compiledClasses, dependencies, jsniMethods, problems);
  }

  public abstract ContentId getContentId();

  public abstract String getLocation();

  public String getSource() {
    if (source == null) {
      source = doGetSource();
    }
    return source;
  }

  public abstract String getTypeName();

  @Override
  public final String toString() {
    return getLocation();
  }

  protected abstract String doGetSource();

  protected abstract CompilationUnit makeUnit(
      List<CompiledClass> compiledClasses, Set<ContentId> dependencies,
      Collection<? extends JsniMethod> jsniMethods, CategorizedProblem[] errors);

  /**
   * This only matters for {@link ArtificialRescueChecker}.
   */
  boolean isGenerated() {
    return false;
  }
}
