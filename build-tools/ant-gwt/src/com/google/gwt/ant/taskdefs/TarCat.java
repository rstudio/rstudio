/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Tar;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.apache.tools.tar.TarOutputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

/**
 * An extension to the Ant Tar task that supports slurping in other tar files
 * without loss of information (such as permissions or symlinks). It behaves in
 * all respects like the basic Tar task, but adds the nested element
 * &lt;includetar&gt; which declares another tar file whose <i>contents</i>
 * should be added to the file being created.
 * 
 * In addition to preserving permissions and symlinks no matter what the host
 * operating system is, there are performance advantages to this approach.
 * Bypassing the file system cuts the disk activity to 50% or less. The
 * intermediate files normally generated require data the size of the tar itself
 * to be both written and read, not to mention the overhead of creating the
 * individual files, which will generally have to be deleted later. Furthurmore,
 * since the source and target are often zipped, the savings can be well over
 * 50%.
 * 
 * Example use:
 * 
 * <pre>
 * <taskdef name="tar.cat"
 *     classname="com.google.gwt.ant.taskdefs.TarCat"
 *     classpath="${gwt.build.lib}/ant-gwt.jar" />
 * <tar.cat destfile="foo.tar.gz" compression="gzip" longfile="gnu">
 *   <!-- all normal tar attributes and elements supported -->
 *   <tarfileset dir="foo/src">
 *     <include name="*.class" />
 *   </tarfileset>
 *   <!-- tar.cat adds the ability to directly slurp in other tar files -->
 *   <includetar src="bar.tar.gz" compression="gzip" prefix="bar/" />
 * </tar.cat>
 * </pre>
 */
public class TarCat extends Tar {

  /**
   * This is a tar file that should be included into a tar operation.
   */
  public static class IncludeTar {
    /**
     * The compression method to use to access the included tar file.
     */
    private UntarCompressionMethod compression = new UntarCompressionMethod();

    /**
     * An association from a super Tar to a derived TarCat.
     */
    private TarFileSet wrapper;

    /**
     * Constructs a new IncludeTar.
     * 
     * @param wrapper the association from a super Tar to a derived TarExt
     */
    public IncludeTar(TarFileSet wrapper) {
      this.wrapper = wrapper;
    }

    /**
     * Set decompression algorithm to use; default=none.
     * 
     * Allowable values are
     * <ul>
     * <li>none - no compression
     * <li>gzip - Gzip compression
     * <li>bzip2 - Bzip2 compression
     * </ul>
     * 
     * @param method compression method
     */
    public void setCompression(UntarCompressionMethod method) {
      compression = method;
    }

    /**
     * If the prefix attribute is set, all files in the fileset are prefixed
     * with that path in the archive. optional.
     * 
     * @param prefix the path prefix.
     */
    public void setPrefix(String prefix) {
      wrapper.setPrefix(prefix);
    }

    /**
     * Set the name/location of a tar file to add to a tar operation.
     * 
     * @param tarFile the tar file to read
     */
    public void setSrc(File tarFile) {
      wrapper.setFile(tarFile);
    }
  }

  /**
   * Straight copy from
   * {@link org.apache.tools.ant.taskdefs.Untar.UntarCompressionMethod} due to
   * access restrictions.
   */
  public static final class UntarCompressionMethod extends EnumeratedAttribute {

    private static final String BZIP2 = "bzip2";
    private static final String GZIP = "gzip";
    private static final String NONE = "none";

    public UntarCompressionMethod() {
      super();
      setValue(NONE);
    }

    public String[] getValues() {
      return new String[] {NONE, GZIP, BZIP2};
    }

    private InputStream decompress(final File file, final InputStream istream)
        throws IOException, BuildException {
      final String value = getValue();
      if (GZIP.equals(value)) {
        return new GZIPInputStream(istream);
      } else {
        if (BZIP2.equals(value)) {
          final char[] magic = new char[] {'B', 'Z'};
          for (int i = 0; i < magic.length; i++) {
            if (istream.read() != magic[i]) {
              throw new BuildException("Invalid bz2 file." + file.toString());
            }
          }
          return new CBZip2InputStream(istream);
        }
      }
      return istream;
    }
  }

  /**
   * The set of tars to include in this tar operation.
   */
  Vector includeTars = new Vector();

  /**
   * A set of tarfileset wrappers mapped to includeTars.
   */
  Vector includeTarWrappers = new Vector();

  /**
   * Creates a TarExt task instance.
   */
  public TarCat() {
  }

  /**
   * Add a new tar to include in this tar operation.
   */
  public IncludeTar createIncludeTar() {
    /*
     * Create a dummy tarfileset to hold our own includeTars and add it to the
     * super class. This is how we get the super class to call us back during
     * execution.
     */
    TarFileSet wrapper = super.createTarFileSet();
    IncludeTar includeTar = new IncludeTar(wrapper);
    includeTars.addElement(includeTar);
    includeTarWrappers.add(wrapper);
    return includeTar;
  }

  protected void tarFile(File file, TarOutputStream tOut, String vPath,
      TarFileSet tarFileSet) throws IOException {
    // See if it's one of ours
    int index = includeTarWrappers.indexOf(tarFileSet);
    if (index < 0) {
      super.tarFile(file, tOut, vPath, tarFileSet);
      return;
    }
    IncludeTar includeTar = (IncludeTar) includeTars.get(index);
    TarInputStream tIn = null;
    try {
      tIn = new TarInputStream(includeTar.compression.decompress(file,
          new BufferedInputStream(new FileInputStream(file))));
      TarEntry te = null;
      while ((te = tIn.getNextEntry()) != null) {
        vPath = te.getName();

        // don't add "" to the archive
        if (vPath.length() <= 0) {
          continue;
        }

        if (te.isDirectory() && !vPath.endsWith("/")) {
          vPath += "/";
        }

        String prefix = tarFileSet.getPrefix();
        // '/' is appended for compatibility with the zip task.
        if (prefix.length() > 0 && !prefix.endsWith("/")) {
          prefix = prefix + "/";
        }
        vPath = prefix + vPath;

        te.setName(vPath);
        tOut.putNextEntry(te);

        if (te.getSize() > 0) {
          byte[] buffer = new byte[8 * 1024];
          while (true) {
            int count = tIn.read(buffer, 0, buffer.length);
            if (count < 0) {
              break;
            }
            tOut.write(buffer, 0, count);
          }
        }
        tOut.closeEntry();
      }

    } catch (IOException ioe) {
      throw new BuildException("Error while expanding " + file.getPath(), ioe,
          getLocation());
    } finally {
      if (tIn != null) {
        try {
          tIn.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }
  }
}
