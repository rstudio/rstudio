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
package com.google.gwt.util.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A smattering of useful file functions
 */
public final class Utility {

  private static String sDevJarName = null;
  private static String sInstallPath = null;

  /**
   * Helper that ignores exceptions during close, because what are you going to
   * do?
   */
  public static void close(InputStream is) {
    try {
      if (is != null)
        is.close();
    } catch (IOException e) {
    }
  }

  /**
   * Helper that ignores exceptions during close, because what are you going to
   * do?
   */
  public static void close(OutputStream os) {
    try {
      if (os != null)
        os.close();
    } catch (IOException e) {
    }
  }

  /**
   * Helper that ignores exceptions during close, because what are you going to
   * do?
   */
  public static void close(RandomAccessFile f) {
    if (f != null) {
      try {
        f.close();
      } catch (IOException e) {
      }
    }
  }

  /**
   * Helper that ignores exceptions during close, because what are you going to
   * do?
   */
  public static void close(Reader reader) {
    try {
      if (reader != null)
        reader.close();
    } catch (IOException e) {
    }
  }

  /**
   * Helper that ignores exceptions during close, because what are you going to
   * do?
   */
  public static void close(Writer writer) {
    try {
      if (writer != null)
        writer.close();
    } catch (IOException e) {
    }
  }

  /**
   * @param parent Parent directory
   * @param fileName New file name
   * @param overwrite Is overwriting an existing file allowed?
   * @return Handle to the file
   * @throws IOException If the file cannot be created, or if the file already
   *           existed and overwrite was false.
   */
  public static File createNormalFile(File parent, String fileName,
      boolean overwrite, boolean ignore) throws IOException {
    File file = new File(parent, fileName);
    if (file.createNewFile()) {
      System.out.println("Created file " + file);
      return file;
    }

    if (!file.exists() || file.isDirectory())
      throw new IOException(file.getPath() + " : could not create normal file.");

    if (ignore) {
      System.out.println(file + " already exists; skipping");
      return null;
    }

    if (!overwrite)
      throw new IOException(
        file.getPath()
          + " : already exists; please remove it or use the -overwrite or -ignore option.");

    System.out.println("Overwriting existing file " + file);
    return file;
  }

  public static String getDevJarName() {
    if (sDevJarName == null) {
      computeInstallationPath();
    }
    return sDevJarName;
  }

  /**
   * @param parent Parent directory of the requested directory.
   * @param dirName Requested name for the directory.
   * @param create Create the directory if it does not already exist?
   * @return A {@link File} representing a directory that now exists.
   * @throws IOException If the directory is not found and/or cannot be created.
   */
  public static File getDirectory(File parent, String dirName, boolean create)
      throws IOException {
    File dir = new File(parent, dirName);
    boolean alreadyExisted = dir.exists();

    if (create) {
      dir.mkdirs();
    }

    if (!dir.exists() || !dir.isDirectory()) {
      if (create)
        throw new IOException(dir.getPath() + " : could not create directory.");
      else
        throw new IOException(dir.getPath() + " : could not find directory.");
    }

    if (create && !alreadyExisted)
      System.out.println("Created directory " + dir);

    return dir;
  }

  /**
   * @param dirPath Requested path for the directory.
   * @param create Create the directory if it does not already exist?
   * @return A {@link File} representing a directory that now exists.
   * @throws IOException If the directory is not found and/or cannot be created.
   */
  public static File getDirectory(String dirPath, boolean create)
      throws IOException {
    return getDirectory(null, dirPath, create);
  }

  /**
   * Gets the contents of a file from the class path as a string.
   * 
   * @param filename the partial path to the resource from this class's class
   *          file
   * @return the contents of the file, or <code>null</code> if the file could
   *         not be found
   * @throws IOException
   */
  public static String getFileFromClassPath(String partialPath)
      throws IOException {
    InputStream in = Utility.class.getClassLoader().getResourceAsStream(
      partialPath);
    try {
      if (in == null) {
        throw new FileNotFoundException(partialPath);
      }
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      int ch;
      while ((ch = in.read()) != -1) {
        os.write(ch);
      }
      return new String(os.toByteArray(), "UTF-8");
    } finally {
      close(in);
    }
  }

  public static String getInstallPath() {
    if (sInstallPath == null) {
      computeInstallationPath();
    }
    return sInstallPath;
  }

  public static void streamOut(File file, OutputStream out, int bufferSize)
      throws IOException {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(file);
      streamOut(fis, out, bufferSize);
    } finally {
      com.google.gwt.util.tools.Utility.close(fis);
    }
  }

  public static void streamOut(InputStream in, OutputStream out, int bufferSize)
      throws IOException {
    assert (bufferSize >= 0);

    byte[] buffer = new byte[bufferSize];
    int bytesRead = 0;
    while (true) {
      bytesRead = in.read(buffer);
      if (bytesRead >= 0) {
        // Copy the bytes out.
        out.write(buffer, 0, bytesRead);
      } else {
        // End of input stream.
        return;
      }
    }
  }

  public static void writeTemplateFile(File file, String contents,
      Map replacements) throws IOException {

    String replacedContents = contents;
    Set entries = replacements.entrySet();
    for (Iterator iter = entries.iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      String replaceThis = (String) entry.getKey();
      String withThis = (String) entry.getValue();
      withThis = withThis.replaceAll("\\\\", "\\\\\\\\");
      withThis = withThis.replaceAll("\\$", "\\\\\\$");
      replacedContents = replacedContents.replaceAll(replaceThis, withThis);
    }

    FileWriter fw = new FileWriter(file);
    fw.write(replacedContents);
    close(fw);
  }

  private static void computeInstallationPath() {
    try {
      String override = System.getProperty("gwt.devjar");
      if (override == null) {
        String partialPath = Utility.class.getName().replace('.', '/').concat(
          ".class");
        URL url = Utility.class.getClassLoader().getResource(partialPath);
        if (url != null && url.getProtocol() == "jar") {
          String path = url.toString();
          String jarPath = path.substring(path.indexOf("file:"),
            path.lastIndexOf('!'));
          File devJarFile = new File(URI.create(jarPath));
          if (!devJarFile.isFile())
            throw new IOException("Could not find jar file; "
              + devJarFile.getCanonicalPath()
              + " does not appear to be a valid file");
          sDevJarName = devJarFile.getName();

          String dirPath = jarPath.substring(0, jarPath.lastIndexOf('/') + 1);
          File installDirFile = new File(URI.create(dirPath));
          if (!installDirFile.isDirectory())
            throw new IOException("Could not find installation directory; "
              + installDirFile.getCanonicalPath()
              + " does not appear to be a valid directory");
          sInstallPath = installDirFile.getCanonicalPath().replace(
            File.separatorChar, '/');
        } else {
          throw new IOException(
            "Cannot determine installation directory; apparently not running from a jar");
        }
      } else {
        override = override.replace('\\', '/');
        int pos = override.lastIndexOf('/');
        if (pos < 0) {
          sInstallPath = "";
          sDevJarName = override;
        } else {
          sInstallPath = override.substring(0, pos);
          sDevJarName = override.substring(pos + 1);
        }

      }
    } catch (IOException e) {
      throw new RuntimeException(
        "Installation problem detected, please reinstall GWT", e);
    }
  }

}
