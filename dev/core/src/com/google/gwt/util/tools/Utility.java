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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * A smattering of useful functions.
 */
public final class Utility {


  private static String sInstallPath = null;

  /**
   * Helper that ignores exceptions during close, because what are you going to
   * do?
   */
  public static void close(InputStream is) {
    try {
      if (is != null) {
        is.close();
      }
    } catch (IOException e) {
    }
  }

  /**
   * Helper that ignores exceptions during close, because what are you going to
   * do?
   */
  public static void close(OutputStream os) {
    try {
      if (os != null) {
        os.close();
      }
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
      if (reader != null) {
        reader.close();
      }
    } catch (IOException e) {
    }
  }

  /**
   * Helper that ignores exceptions during close, because what are you going to
   * do?
   */
  public static void close(Socket socket) {
    try {
      if (socket != null) {
        socket.close();
      }
    } catch (IOException e) {
    }
  }

  /**
   * Helper that ignores exceptions during close, because what are you going to
   * do?
   */
  public static void close(Writer writer) {
    try {
      if (writer != null) {
        writer.close();
      }
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

    if (!file.exists() || file.isDirectory()) {
      throw new IOException(file.getPath() + " : could not create normal file.");
    }

    if (ignore) {
      System.out.println(file + " already exists; skipping");
      return null;
    }

    if (!overwrite) {
      throw new IOException(
          file.getPath()
              + " : already exists; please remove it or use the -overwrite or -ignore option.");
    }

    System.out.println("Overwriting existing file " + file);
    return file;
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
      // No need to check mkdirs result because we check for dir.exists()
      dir.mkdirs();
    }

    if (!dir.exists() || !dir.isDirectory()) {
      if (create) {
        throw new IOException(dir.getPath() + " : could not create directory.");
      } else {
        throw new IOException(dir.getPath() + " : could not find directory.");
      }
    }

    if (create && !alreadyExisted) {
      System.out.println("Created directory " + dir);
    }

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
   * Gets the contents of a file from the class path as a String. Note: this
   * method is only guaranteed to work for resources in the same class loader
   * that contains this {@link Utility} class.
   *
   * @param partialPath the partial path to the resource on the class path
   * @return the contents of the file
   * @throws IOException if the file could not be found or an error occurred
   *           while reading it
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
      streamOut(in, os, 1024);
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

  /**
   * Creates a randomly-named temporary directory.
   *
   * @param baseDir base directory to contain the new directory. May be
   *          {@code null}, in which case the directory given by the
   *          {@code java.io.tmpdir} system property will be used.
   * @param prefix the initial characters of the new directory name
   * @return a newly-created temporary directory; the caller must delete this
   *          directory (either when done or on VM exit)
   */
  public static File makeTemporaryDirectory(File baseDir, String prefix)
      throws IOException {
    if (baseDir == null) {
      baseDir = new File(System.getProperty("java.io.tmpdir"));
    }
    // No need to check the result of this mkdirs call because
    // we will detect the subsequent failure
    baseDir.mkdirs();

    // Try this a few times due to non-atomic delete+mkdir operations.
    for (int tries = 0; tries < 3; ++tries) {
      File result = File.createTempFile(prefix, null, baseDir);
      if (!result.delete()) {
        throw new IOException("Couldn't delete temporary file "
            + result.getAbsolutePath() + " to replace with a directory.");
      }
      if (result.mkdirs()) {
        // Success.
        return result;
      }
    }
    throw new IOException(
        "Couldn't create temporary directory after 3 tries in "
            + baseDir.getAbsolutePath());
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

  public static void writeTemplateBinaryFile(File file, byte[] contents) throws IOException {

    FileOutputStream o = new FileOutputStream(file);
    o.write(contents);
    close(o);
  }

  public static void writeTemplateFile(File file, String contents,
      Map<String, String> replacements) throws IOException {

    String replacedContents = contents;
    Set<Entry<String, String>> entries = replacements.entrySet();
    for (Iterator<Entry<String, String>> iter = entries.iterator(); iter.hasNext();) {
      Entry<String, String> entry = iter.next();
      String replaceThis = entry.getKey();
      String withThis = entry.getValue();
      withThis = withThis.replaceAll("\\\\", "\\\\\\\\");
      withThis = withThis.replaceAll("\\$", "\\\\\\$");
      replacedContents = replacedContents.replaceAll(replaceThis, withThis);
    }

    PrintWriter pw = new PrintWriter(file);
    LineNumberReader lnr = new LineNumberReader(new StringReader(replacedContents));
    for (String line = lnr.readLine(); line != null; line = lnr.readLine()) {
      pw.println(line);
    }
    close(pw);
  }

  private static void computeInstallationPath() {
    try {
      String override = System.getProperty("gwt.devjar");
      if (override == null) {
        String partialPath = Utility.class.getName().replace('.', '/').concat(
            ".class");
        URL url = Utility.class.getClassLoader().getResource(partialPath);
        if (url != null && "jar".equals(url.getProtocol())) {
          String path = url.toString();
          String jarPath = path.substring(path.indexOf("file:"),
              path.lastIndexOf('!'));
          File devJarFile = new File(URI.create(jarPath));
          if (!devJarFile.isFile()) {
            throw new IOException("Could not find jar file; "
                + devJarFile.getCanonicalPath()
                + " does not appear to be a valid file");
          }

          String dirPath = jarPath.substring(0, jarPath.lastIndexOf('/') + 1);
          File installDirFile = new File(URI.create(dirPath));
          if (!installDirFile.isDirectory()) {
            throw new IOException("Could not find installation directory; "
                + installDirFile.getCanonicalPath()
                + " does not appear to be a valid directory");
          }

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
        } else {
          sInstallPath = override.substring(0, pos);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(
          "Installation problem detected, please reinstall GWT", e);
    }
  }

}
