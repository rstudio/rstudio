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
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * An extension to the Ant Tar task that supports slurping in other tar files
 * without loss of information (such as permissions or symlinks). It behaves in
 * all respects like the basic Tar task, but adds the nested element
 * {@code <includetar>} which declares another tar file whose <i>contents</i>
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
 * &lt;taskdef name="tar.cat"
 *     classname="com.google.gwt.ant.taskdefs.TarCat"
 *     classpath="${gwt.build.lib}/ant-gwt.jar" /&gt;
 * &lt;tar.cat destfile="foo.tar.gz" compression="gzip" longfile="gnu"&gt;
 *   &lt;!-- all normal tar attributes and elements supported --&gt;
 *   &lt;tarfileset dir="foo/src"&gt;
 *     &lt;include name="*.class" /&gt;
 *   &lt;/tarfileset&gt;
 *   &lt;!-- tar.cat adds the ability to directly slurp in other tar files --&gt;
 *   &lt;includetar src="bar.tar.gz" compression="gzip" prefix="bar/" /&gt;
 * &lt;/tar.cat&gt;
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
     * This instance's peer object that is a member of {@link TarCat}'s
     * superclass.
     */
    private TarFileSet peer;

    /**
     * Constructs a new IncludeTar.
     * 
     * @param peer this instance's peer object that is a member of
     *          {@link TarCat}'s superclass
     */
    public IncludeTar(TarFileSet peer) {
      this.peer = peer;
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
      peer.setPrefix(prefix);
    }

    /**
     * Set the name/location of a tar file to add to a tar operation.
     * 
     * @param tarFile the tar file to read
     */
    public void setSrc(File tarFile) {
      peer.setFile(tarFile);
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
   * A map to the set of {@link IncludeTar}s from their peer objects.
   */
  private final Map<TarFileSet, IncludeTar> peerMap = new IdentityHashMap<TarFileSet, IncludeTar>();

  /**
   * Creates a task instance.
   */
  public TarCat() {
  }

  /**
   * Add a new tar to include in this tar operation.
   */
  public IncludeTar createIncludeTar() {
    /*
     * Create a peer TarFileSet corresponding to one of our own IncludeTars.
     * This causes the superclass to call us back during execution.
     */
    TarFileSet peer = super.createTarFileSet();
    IncludeTar includeTar = new IncludeTar(peer);
    peerMap.put(peer, includeTar);
    return includeTar;
  }

  protected void tarFile(File file, TarOutputStream tOut, String vPath,
      TarFileSet tarFileSet) throws IOException {
    if (!peerMap.containsKey(tarFileSet)) {
      // Not one of ours, punt to superclass.
      super.tarFile(file, tOut, vPath, tarFileSet);
      return;
    }

    IncludeTar includeTar = peerMap.get(tarFileSet);
    String prefix = tarFileSet.getPrefix();
    if (prefix.length() > 0 && !prefix.endsWith("/")) {
      // '/' is appended for compatibility with the zip task.
      prefix = prefix + "/";
    }
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

    } catch (IOException e) {
      throw new BuildException("Error while expanding " + file.getPath(), e,
          getLocation());
    } finally {
      if (tIn != null) {
        try {
          tIn.close();
        } catch (IOException ignored) {
        }
      }
    }
  }
}
