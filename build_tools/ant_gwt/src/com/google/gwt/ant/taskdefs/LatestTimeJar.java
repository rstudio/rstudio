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
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.zip.ZipExtraField;
import org.apache.tools.zip.ZipOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * A variation on Jar which handles duplicate entries by only archiving the most
 * recent of any given path. This is done by keeping a map of paths (as shown in
 * the jar file) against {@link EntryInfo} objects identifying the input source
 * and its timestamp. Most of the actual archiving is deferred until archive
 * finalization, when we've decided on the actual de-duplicated set of entries.
 */
public class LatestTimeJar extends Jar {

  /**
   * Metadata about pending entries in the jar, for replacement if newer entries
   * are found. Subclasses of EntryInfo are held in the
   */
  protected abstract class EntryInfo {
    protected long timestamp;
    protected int mode;

    public EntryInfo(long lastModified, int mode) {
      this.timestamp = lastModified;
      this.mode = mode;
    }

    /**
     * Called to actually add the entry to a given zip stream.
     * 
     * @param out
     * @param path
     * @throws IOException
     */
    public abstract void addToZip(ZipOutputStream out, String path)
        throws IOException;

    public long getLastModified() {
      return timestamp;
    }

    public int getMode() {
      return mode;
    }
  }

  /**
   * Metadata about a directory entry.
   */
  protected class DirEntryInfo extends EntryInfo {
    protected File dir;
    protected ZipExtraField extra[];

    public DirEntryInfo(File dir, long touchTime, int mode,
        ZipExtraField extra[]) {
      super(touchTime, mode);
      this.dir = dir;
      this.extra = extra;
    }

    @Override
    public void addToZip(ZipOutputStream out, String path) throws IOException {
      doZipDir(dir, out, path, mode, extra);
    }
  }

  /**
   * Metadata about a file entry.
   */
  protected class FileEntryInfo extends EntryInfo {
    private File tmpFile;
    private File archive;

    public FileEntryInfo(InputStream in, long lastModified, File fromArchive,
        int mode) throws IOException {
      super(lastModified, mode);
      tmpFile = createTempFile("gwtjar", "");
      tmpFile.deleteOnExit();
      OutputStream fos = new FileOutputStream(tmpFile);
      int readLen = in.read(buffer);
      while (readLen > 0) {
        fos.write(buffer, 0, readLen);
        readLen = in.read(buffer);
      }
      fos.close();
      archive = fromArchive;
    }

    @Override
    public void addToZip(ZipOutputStream out, String path) throws IOException {
      FileInputStream inStream = new FileInputStream(tmpFile);
      try {
        doZipFile(inStream, out, path, timestamp, archive, mode);
        tmpFile.delete();
      } finally {
        inStream.close();
      }
    }
  }

  /**
   * Used to generate temporary file names.
   */
  private static long counter = -1;

  /**
   * Creates a temporary file.
   * 
   * @param prefix the file prefix
   * @param suffix the file suffix
   * @return the new file
   * @throws IOException if the file cannot be created
   */
  private static File createTempFile(String prefix, String suffix)
      throws IOException {
    if (suffix == null) {
      suffix = ".tmp";
    }

    // Get the temp file directory.
    File tmpDir = new File(System.getProperty("java.io.tmpdir"));
    tmpDir.mkdirs();

    // Generate a random name.
    if (counter == -1) {
      counter = new Random().nextLong();
    }
    boolean created = false;
    File tmpFile;
    do {
      counter++;
      tmpFile = new File(tmpDir, prefix + Long.toString(counter) + suffix);
      if (!tmpFile.exists()) {
        created = tmpFile.createNewFile();
        if (!created) {
          // If we fail the create the temp file, it must have been created by
          // another thread between lines 161 and 162.  We re-seed to avoid
          // further race conditions.
          counter = new Random().nextLong();
        }
      }
    } while (!created);

    // Create the file.
    tmpFile.createNewFile();
    return tmpFile;
  }

  private byte buffer[] = new byte[16 * 1024];
  private Map<String, EntryInfo> paths = new TreeMap<String, EntryInfo>();

  @Override
  protected void finalizeZipOutputStream(ZipOutputStream out)
      throws IOException, BuildException {
    for (String path : paths.keySet()) {
      paths.get(path).addToZip(out, path);
    }
    super.finalizeZipOutputStream(out);
  }

  @Override
  protected void zipDir(File dir,
      @SuppressWarnings("unused") ZipOutputStream out, String path, int mode,
      ZipExtraField[] extra) throws IOException {
    long thisTouchTime = (dir == null ? 0L : dir.lastModified());
    String dirName = (dir == null ? "<null>" : dir.getAbsolutePath());
    if (shouldReplace(path, thisTouchTime)) {
      if (paths.get(path) != null) {
        log("Obsoleting older " + path + " with " + dirName,
            Project.MSG_VERBOSE);
      }
      paths.put(path, new DirEntryInfo(dir, thisTouchTime, mode, extra));
    } else {
      log("Newer " + path + " already added, skipping " + dirName,
          Project.MSG_VERBOSE);
    }
  }

  @Override
  protected void zipFile(InputStream in,
      @SuppressWarnings("unused") ZipOutputStream out, String path,
      long lastModified, File fromArchive, int mode) throws IOException {

    String desc = (fromArchive == null ? "file" : "file from "
        + fromArchive.getAbsolutePath());

    if (shouldReplace(path, lastModified)) {
      if (paths.get(path) != null) {
        log("Obsoleting older " + path + " with " + desc, Project.MSG_VERBOSE);
      }
      paths.put(path, new FileEntryInfo(in, lastModified, fromArchive, mode));
    } else {
      log("Newer " + path + " already added, skipping " + desc,
          Project.MSG_VERBOSE);
    }
  }

  private void doZipDir(File dir, ZipOutputStream out, String entryName,
      int mode, ZipExtraField[] extra) throws IOException {
    super.zipDir(dir, out, entryName, mode, extra);
  }

  private void doZipFile(InputStream inStream, ZipOutputStream out,
      String entryName, long timestamp, File archive, int mode)
      throws IOException {
    super.zipFile(inStream, out, entryName, timestamp, archive, mode);
  }

  /**
   * Checks whether an entry should be replaced, by touch dates and duplicates
   * setting.
   * 
   * @param path the path of an entry being considered
   * @param touchTime the lastModified of the candiate replacement
   * @return true if the file should be replaced
   */
  private boolean shouldReplace(String path, long touchTime) {
    EntryInfo oldInfo = paths.get(path);
    // adding from jars, we get directories with 0L time; missing should be
    // earlier than that, -1L.
    long existingTouchTime = ((oldInfo != null) ? oldInfo.getLastModified()
        : -1L);
    return (existingTouchTime < touchTime || (existingTouchTime == touchTime && this.duplicate.equals("add")));
  }
}
