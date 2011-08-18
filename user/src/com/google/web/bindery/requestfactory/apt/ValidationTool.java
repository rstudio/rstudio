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
package com.google.web.bindery.requestfactory.apt;

import com.google.gwt.dev.util.Name.BinaryName;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import javax.lang.model.SourceVersion;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * Provides "late" validation services when server types aren't available to the
 * shared-interface compilation process. This tool is provided the name of an
 * output jar and the binary names of RequestFactory interfaces that should be
 * validated. The validation process will provide pre-computed type map builders
 * for use by the ServiceLayer.
 * 
 * @see http://code.google.com/p/google-web-toolkit/wiki/
 *      RequestFactoryInterfaceValidation
 */
public class ValidationTool {
  /**
   * A JavaFileManager that writes the class outputs into a jar file or a
   * directory.
   */
  static class JarOrDirectoryOutputFileManager extends ForwardingJavaFileManager<JavaFileManager> {
    private final List<MemoryJavaFileObject> toOutput = new ArrayList<MemoryJavaFileObject>();
    private final File output;

    JarOrDirectoryOutputFileManager(File output, JavaFileManager fileManager) {
      super(fileManager);
      this.output = output;
    }

    @Override
    public void close() throws IOException {
      if (output.isDirectory()) {
        writeToDirectory();
      } else {
        writeToJar();
      }
    }

    /**
     * Not expected to be called. Overridden to prevent accidental writes to
     * disk.
     */
    @Override
    public FileObject getFileForOutput(Location location, String packageName, String relativeName,
        FileObject sibling) throws IOException {
      throw new UnsupportedOperationException("Not expecting to write " + packageName + "/"
          + relativeName);
    }

    /**
     * This method will receive generated source and class files.
     */
    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind,
        FileObject sibling) throws IOException {
      String path = BinaryName.toInternalName(className);
      String suffix;
      switch (kind) {
        case CLASS:
          suffix = ".class";
          break;
        case SOURCE:
          suffix = ".java";
          break;
        default:
          throw new UnsupportedOperationException("Unexpected kind " + kind);
      }
      MemoryJavaFileObject toReturn =
          new MemoryJavaFileObject(uri("memory:///" + path + suffix), kind);
      if (StandardLocation.CLASS_OUTPUT.equals(location) && Kind.CLASS.equals(kind)) {
        toOutput.add(toReturn);
      }
      return toReturn;
    }

    @Override
    public boolean isSameFile(FileObject a, FileObject b) {
      if (a instanceof MemoryJavaFileObject && b instanceof MemoryJavaFileObject) {
        MemoryJavaFileObject memoryA = (MemoryJavaFileObject) a;
        MemoryJavaFileObject memoryB = (MemoryJavaFileObject) b;
        return memoryA.getKind().equals(memoryB.getKind())
            && memoryA.toUri().equals(memoryB.toUri());
      }
      if (a instanceof FakeJavaFileObject && b instanceof FakeJavaFileObject) {
        // Only one file ever created
        return true;
      }
      return super.isSameFile(a, b);
    }

    private void writeToDirectory() throws IOException {
      for (MemoryJavaFileObject file : toOutput) {
        String path = file.toUri().getPath();
        if (path.equals("/fake/Fake.class")) {
          // ignore dummy class
          continue;
        }
        File target = new File(output, path);
        target.getParentFile().mkdirs();
        FileOutputStream out = new FileOutputStream(target);
        out.write(file.bytes.toByteArray());
        out.close();
      }
    }

    private void writeToJar() throws IOException, FileNotFoundException {
      JarOutputStream jar = new JarOutputStream(new FileOutputStream(output));
      for (MemoryJavaFileObject file : toOutput) {
        String path = file.toUri().getPath();
        if (path.equals("/fake/Fake.class")) {
          // ignore dummy class
          continue;
        }
        // Strip leading /
        ZipEntry entry = new ZipEntry(path.substring(1));
        jar.putNextEntry(entry);
        jar.write(file.bytes.toByteArray());
      }
      jar.close();
    }
  }

  /**
   * Provides a fake type to seed the compilation process with.
   */
  private static class FakeJavaFileObject extends SimpleJavaFileObject {
    public FakeJavaFileObject() {
      super(uri("fake:///fake/Fake.java"), JavaFileObject.Kind.SOURCE);
    }

    @Override
    public CharSequence getCharContent(boolean arg0) throws IOException {
      return "package fake; interface Fake {}";
    }
  }

  /**
   * An in-memory implementation of JavaFileObject.
   */
  private static class MemoryJavaFileObject extends SimpleJavaFileObject {
    private ByteArrayOutputStream bytes;
    private StringWriter charContents;

    public MemoryJavaFileObject(URI uri, JavaFileObject.Kind kind) {
      super(uri, kind);
    }

    @Override
    public CharSequence getCharContent(boolean ignored) throws IOException {
      return charContents.toString();
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
      bytes = new ByteArrayOutputStream();
      return bytes;
    }

    @Override
    public Writer openWriter() throws IOException {
      charContents = new StringWriter();
      return charContents;
    }
  }

  public static void main(String[] args) throws IOException {
    System.exit(exec(args) ? 0 : -1);
  }

  /**
   * A testable "main" method.
   */
  static boolean exec(String[] args) throws IOException {
    if (args.length < 2) {
      System.err.println("java -cp requestfactory-client.jar:your_server-code.jar "
          + ValidationTool.class.getCanonicalName()
          + " (/some/directory | output.jar) com.example.shared.MyRequestFactory");
      System.err.println("See "
          + "http://code.google.com/p/google-web-toolkit/wiki/RequestFactoryInterfaceValidation "
          + "for more information.");
      return false;
    }
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      System.err.println("This tool must be run with a JDK, not a JRE");
      return false;
    }
    if (!compiler.getSourceVersions().contains(SourceVersion.RELEASE_6)) {
      System.err.println("This tool must be run with a Java 1.6 compiler");
      return false;
    }

    boolean clientOnly = false;
    List<String> argList = new ArrayList<String>(Arrays.asList(args));
    if (argList.get(0).equals("-client")) {
      clientOnly = true;
      argList.remove(0);
    }

    // Control how the compile process writes data to disk
    JavaFileManager fileManager =
        new JarOrDirectoryOutputFileManager(new File(argList.remove(0)), compiler
            .getStandardFileManager(null, null, null));

    // Create a validator and require it to process the specified types
    RfValidator processor = new RfValidator();
    if (clientOnly) {
      processor.setClientOnly(true);
    } else {
      processor.setMustResolveAllMappings(true);
    }
    processor.setRootOverride(argList);

    // Create the compilation task
    CompilationTask task =
        compiler.getTask(null, fileManager, null, null, null, Arrays
            .asList(new FakeJavaFileObject()));
    task.setProcessors(Arrays.asList(processor));
    if (!task.call()) {
      return false;
    }
    // Save data only on successful compilation
    fileManager.close();
    return true;
  }

  /**
   * Convenience method for discarding {@link URISyntaxException}.
   */
  private static URI uri(String contents) {
    try {
      return new URI(contents);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
