/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.core.ext.linker;

import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.Util;
import com.google.gwt.util.tools.Utility;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An artifact that will be emitted into the output. All EmittedArtifacts
 * contained in the {@link ArtifactSet} at the end of the Linking process will
 * be emitted by the compiler into the module's output directory. This type may
 * be extended by Linker providers to provide alternative implementations of
 * {@link #getContents(TreeLogger)}.
 * 
 * TODO(bobv): provide a timestamp so we can make the time on output files match
 * that of input files?
 */
public abstract class EmittedArtifact extends Artifact<EmittedArtifact> {

  /**
   * Describes the visibility of an artifact.
   */
  public enum Visibility {

    /**
     * A public artifact is something that may be served to clients.
     */
    Public,
    
    /**
     * A private artifact is something that is only used during the build
     * process.
     */
    Private {
      @Override
      public boolean matches(Visibility visibility) {
        switch (visibility) {
          case LegacyDeploy:
          case Private:
            return true;
          default:
            return false;
        }
      }
    },
    
    /**
     * A deploy artifact is deployed to the server but is never served to the
     * client.
     */
    Deploy {
      @Override
      public boolean matches(Visibility visibility) {
        switch (visibility) {
          case Deploy:
          case LegacyDeploy:
            return true;
          default:
            return false;
        }
      }
    },
    
    /**
     * For legacy use only - used for artifacts that were previously marked as
     * private because they should not be delivered to the client, but actually
     * should be visible to the server.  These artifacts will now be treated as
     * both Private and Deploy, so that existing build tools that expect to find
     * them in the output directory for Private artifacts will find them.
     * 
     * New code should use Deploy instead.
     */
    LegacyDeploy {
      @Override
      public boolean matches(Visibility visibility) {
        switch (visibility) {
          case Deploy:
          case LegacyDeploy:
          case Private:
            return true;
          default:
            return false;
        }
      }
    };
    
    /**
     * Returns true if this visibility matches the requested visibility level,
     * dealing with the fact that {@link #LegacyDeploy} matches both
     * {@link #Private} and {@link #Deploy}.
     * 
     * @param visibility
     * @return true if this visibility matches the requested level
     */
    public boolean matches(Visibility visibility) {
      return this == visibility;
    }
  }

  private final String partialPath;

  /**
   * This is mutable because it has no effect on identity.
   */
  private Visibility visibility;

  protected EmittedArtifact(Class<? extends Linker> linker, String partialPath) {
    super(linker);
    assert partialPath != null;
    this.partialPath = partialPath;
    visibility = Visibility.Public;
  }

  /**
   * Provides access to the contents of the EmittedResource.
   */
  public abstract InputStream getContents(TreeLogger logger)
      throws UnableToCompleteException;

  /**
   * Returns the time, measured in milliseconds from the epoch, at which the
   * Artifact was last modified. This will be used to set the last-modified
   * timestamp on the files written to disk.
   * <p>
   * The default implementation always returns the current time. Subclasses
   * should override this method to provide a type-appropriate value.
   * 
   * @return the time at which the Artifact was last modified
   */
  public long getLastModified() {
    return System.currentTimeMillis();
  }

  /**
   * Returns the partial path within the output directory of the
   * EmittedArtifact.
   */
  public final String getPartialPath() {
    return partialPath;
  }

  /**
   * @return the visibility
   */
  public Visibility getVisibility() {
    return visibility;
  }

  @Override
  public final int hashCode() {
    return getPartialPath().hashCode();
  }

  /**
   * Returns whether or not the data contained in the EmittedArtifact should be
   * written into the module output directory or into an auxiliary directory.
   * <p>
   * EmittedArtifacts that return <code>true</code> for this method will not
   * be emitted into the normal module output location, but will instead be
   * written into a directory that is a sibling to the module output directory.
   * The partial path of the EmittedArtifact will be prepended with the
   * short-name of the Linker type that created the EmittedArtifact.
   * <p>
   * Private EmittedArtifacts are intended for resources that generally should
   * not be deployed to the server in the same location as the module
   * compilation artifacts.
   * 
   * @deprecated use {@link #getVisibility()} instead
   */
  @Deprecated
  public boolean isPrivate() {
    return visibility == Visibility.Private;
  }

  /**
   * Sets the private attribute of the EmittedResource.
   * 
   * @param isPrivate true if this artifact is private
   *
   * @deprecated use {@link #setVisibility(Visibility)} instead
   */
  @Deprecated
  public void setPrivate(boolean isPrivate) {
    this.visibility = isPrivate ? Visibility.Private : Visibility.Public;
  }

  /**
   * @param visibility the visibility to set
   */
  public void setVisibility(Visibility visibility) {
    this.visibility = visibility;
  }

  @Override
  public String toString() {
    return getPartialPath();
  }

  /**
   * Provides access to the contents of the EmittedResource.
   */
  public void writeTo(TreeLogger logger, OutputStream out)
      throws UnableToCompleteException {
    try {
      InputStream in = getContents(logger);
      Util.copyNoClose(in, out);
      Utility.close(in);
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to read or write stream", e);
      throw new UnableToCompleteException();
    }
  }

  @Override
  protected final int compareToComparableArtifact(EmittedArtifact o) {
    return getPartialPath().compareTo(o.getPartialPath());
  }

  @Override
  protected final Class<EmittedArtifact> getComparableArtifactType() {
    return EmittedArtifact.class;
  }
}
