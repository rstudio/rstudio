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
package com.google.gwt.user.tools;

import com.google.gwt.dev.About;
import com.google.gwt.dev.ArgProcessorBase;
import com.google.gwt.dev.Compiler;
import com.google.gwt.dev.DevMode;
import com.google.gwt.dev.GwtVersion;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.collect.HashSet;
import com.google.gwt.user.tools.util.ArgHandlerIgnore;
import com.google.gwt.user.tools.util.ArgHandlerOverwrite;
import com.google.gwt.user.tools.util.CreatorUtilities;
import com.google.gwt.util.tools.ArgHandlerExtra;
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ArgHandlerOutDir;
import com.google.gwt.util.tools.ArgHandlerString;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Creates a GWT application skeleton.
 */
public final class WebAppCreator {

  class ArgProcessor extends ArgProcessorBase {

    private final class ArgHandlerOutDirExtension extends ArgHandlerOutDir {
      @Override
      public void setDir(File dir) {
        outDir = dir;
      }
    }

    public ArgProcessor() {
      registerHandler(new ArgHandlerOverwriteExtension());
      registerHandler(new ArgHandlerIgnoreExtension());
      registerHandler(new ArgHandlerTemplates());
      registerHandler(new ArgHandlerModuleName());
      registerHandler(new ArgHandlerOutDirExtension());
      registerHandler(new ArgHandlerNoEclipse());
      registerHandler(new ArgHandlerOnlyEclipse());
      registerHandler(new ArgHandlerJUnitPath());
      registerHandler(new ArgHandlerMaven());
      registerHandler(new ArgHandlerNoAnt());
    }

    @Override
    protected String getName() {
      return WebAppCreator.class.getName();
    }
  }

  private final class ArgHandlerIgnoreExtension extends ArgHandlerIgnore {
    @Override
    public boolean setFlag() {
      if (overwrite) {
        System.err.println("-ignore cannot be used with -overwrite");
        return false;
      }
      ignore = true;
      return true;
    }
  }

  private final class ArgHandlerJUnitPath extends ArgHandlerString {

    @Override
    public String[] getDefaultArgs() {
      return null;
    }

    @Override
    public String getPurpose() {
      return "Specifies the path to your junit.jar (optional)";
    }

    @Override
    public String getTag() {
      return "-junit";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"pathToJUnitJar"};
    }

    @Override
    public boolean isRequired() {
      return false;
    }

    @Override
    public boolean setString(String str) {
      File f = new File(str);
      if (!f.exists() || !f.isFile()) {
        System.err.println("File not found: " + str);
        return false;
      }
      junitPath = str;
      return true;
    }
  }

  private final class ArgHandlerMaven extends ArgHandlerFlag {
    @Override
    public String getPurpose() {
      return "Deprecated. Create a maven2 project structure and pom file (default disabled). "
          + "Equivalent to specifying 'maven' in the list of templates.";
    }

    @Override
    public String getTag() {
      return "-maven";
    }

    @Override
    public boolean setFlag() {
      if (onlyEclipse) {
        System.err.println("-maven and -XonlyEclipse cannot be used at the same time.");
        return false;
      }
      if (!templates.contains("maven")) {
        templates.add("maven");
      }
      return true;
    }
  }

  private final class ArgHandlerModuleName extends ArgHandlerExtra {
    @Override
    public boolean addExtraArg(String arg) {
      if (moduleName != null) {
        System.err.println("Too many arguments.");
        return false;
      }

      if (!CreatorUtilities.isValidModuleName(arg)) {
        System.err.println("'"
            + arg
            + "' does not appear to be a valid fully-qualified Java class name.");
        return false;
      }

      moduleName = arg;
      return true;
    }

    @Override
    public String getPurpose() {
      return "The name of the module to create (e.g. com.example.myapp.MyApp)";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"moduleName"};
    }

    @Override
    public boolean isRequired() {
      return true;
    }
  }

  private final class ArgHandlerNoAnt extends ArgHandlerFlag {
    @Override
    public String getPurpose() {
      return "Deprecated. Do not create an ant configuration file. "
          + "Equivalent to not specifying 'ant' in the list of templates.";
    }

    @Override
    public String getTag() {
      return "-noant";
    }

    @Override
    public boolean setFlag() {
      argProcessingToDos.add(new Procrastinator() {
        @Override
        public void stopProcratinating() {
          if (templates.contains("maven")) {
            System.err.println("-maven and -noant are redundant. Continuing.");
          }
          if (templates.contains("ant")) {
            System.err.println("Removing ant template from generated output.");
            templates.remove("ant");
          }
        }
      });
      return true;
    }
  }

  private final class ArgHandlerNoEclipse extends ArgHandlerFlag {
    @Override
    public String getPurpose() {
      return "Deprecated. Do not generate eclipse files. "
          + "Equivalent to not specifying 'eclipse' in the list of templates";
    }

    @Override
    public String getTag() {
      return "-XnoEclipse";
    }

    @Override
    public boolean isUndocumented() {
      return true;
    }

    @Override
    public boolean setFlag() {
      if (onlyEclipse) {
        System.err.println("-XonlyEclipse and -XnoEclipse cannot be used at the same time.");
        return false;
      }
      if (!templates.contains("maven")) {
        System.err.println("-maven and -XnoEclipse are redundant. Continuing.");
      }
      noEclipse = true;
      argProcessingToDos.add(new Procrastinator() {
        @Override
        public void stopProcratinating() {
          if (templates.contains("eclipse")) {
            System.err.println("Removing eclipse template from generated output.");
            templates.remove("eclipse");
          }
        }
      });
      return true;
    }
  }

  private final class ArgHandlerOnlyEclipse extends ArgHandlerFlag {
    @Override
    public String getPurpose() {
      return "Deprecated. Generate only eclipse files. "
          + "Equivalent to only specifying 'eclipse' in the list of templates.";
    }

    @Override
    public String getTag() {
      return "-XonlyEclipse";
    }

    @Override
    public boolean isUndocumented() {
      return true;
    }

    @Override
    public boolean setFlag() {
      if (noEclipse) {
        System.err.println("-XonlyEclipse and -XnoEclipse cannot be used at the same time.");
        return false;
      }
      if (templates.contains("maven")) {
        System.err.println("-maven and -XonlyEclipse cannot be used at the same time.");
        return false;
      }
      onlyEclipse = true;
      argProcessingToDos.add(new Procrastinator() {
        @Override
        public void stopProcratinating() {
          System.err.println("Removing all templates but 'eclipse' from generated output.");
          templates.clear();
          templates.add("eclipse");
        }
      });
      return true;
    }
  }

  private final class ArgHandlerOverwriteExtension extends ArgHandlerOverwrite {
    @Override
    public boolean setFlag() {
      if (ignore) {
        System.err.println("-overwrite cannot be used with -ignore");
        return false;
      }
      overwrite = true;
      return true;
    }
  }

  private final class ArgHandlerTemplates extends ArgHandlerString {

    @Override
    public String[] getDefaultArgs() {
      return new String[] {getTag(), "sample, ant, eclipse, readme"};
    }

    @Override
    public String getPurpose() {
      return "Specifies the template(s) to use (comma separeted)."
          + " Defaults to 'sample,ant,eclipse,readme'";
    }

    @Override
    public String getTag() {
      return "-templates";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"template1,template2,..."};
    }

    @Override
    public boolean isRequired() {
      return false;
    }

    @Override
    public boolean setString(String str) {
      String[] templateList = str.split(", *");
      for (String template : templateList) {
        URL url = getTemplateRoot(template);
        if (url == null) {
          System.err.println("Template not found: " + template);
          return false;
        }
        templates.add(template);
      }
      return true;
    }
  }

  private static final class FileCreator {
    private final File destDir;

    private final String destName;
    private final boolean isBinary;
    private final String sourceName;
    public FileCreator(File destDir, String destName, String sourceName, boolean isBinary) {
      this.destDir = destDir;
      this.sourceName = sourceName;
      this.destName = destName;
      this.isBinary = isBinary;
    }

    @Override
    public String toString() {
      return "FileCreator [destDir=" + destDir + ", destName=" + destName
          + ", sourceName=" + sourceName + ", isBinary=" + isBinary + "]";
    }
  }

  private abstract static class Procrastinator {
    public abstract void stopProcratinating();
  }

  public static void main(String[] args) {
    System.exit(doMain(args) ? 0 : 1);
  }

  protected static boolean doMain(String... args) {
    WebAppCreator creator = new WebAppCreator();
    ArgProcessor argProcessor = creator.new ArgProcessor();
    if (argProcessor.processArgs(args)) {
      return creator.run();
    }
    return false;
  }

  private static String getTemplateBasePath(String template) {
    return "/" + WebAppCreator.class.getPackage().getName().replace('.', '/') + "/templates/" 
        + template + "/";
  }

  private static URL getTemplateRoot(String template) {
    return WebAppCreator.class.getResource("templates/" + template);
  }

  private static String replaceFileName(Map<String, String> replacements, String name) {
    String replacedContents = name;
    Set<Entry<String, String>> entries = replacements.entrySet();
    for (Iterator<Entry<String, String>> iter = entries.iterator(); iter.hasNext();) {
      Entry<String, String> entry = iter.next();
      String replaceThis = entry.getKey();
      replaceThis = replaceThis.replaceAll("@(.*)", "_$1_");
      String withThis = entry.getValue();
      withThis = withThis.replaceAll("\\\\", "\\\\\\\\");
      withThis = withThis.replaceAll("\\$", "\\\\\\$");
      replacedContents = replacedContents.replaceAll(replaceThis, withThis);
    }
    return replacedContents;
  }
  private ArrayList<Procrastinator> argProcessingToDos = new ArrayList<Procrastinator>();
  private boolean ignore = false;
  private String junitPath = null;
  private String moduleName;
  private boolean noEclipse;
  private boolean onlyEclipse;
  private File outDir;
  private boolean overwrite = false;

  private HashSet<String> templates = new HashSet<String>();

  public List<FileCreator> getFiles(Map<String, String> replacements)
      throws IOException, WebAppCreatorException {
    List<FileCreator> files = new ArrayList<FileCreator>();

    Utility.getDirectory(outDir.getPath(), true);

    for (String template : templates) {
      URL templateUrl = getTemplateRoot(template);
      if ("jar".equals(templateUrl.getProtocol())) {
        files.addAll(getTemplateFilesFromZip(replacements, templateUrl, outDir));
      } else if ("file".equals(templateUrl.getProtocol())) {
        File templateRoot = new File(templateUrl.getPath());
        files.addAll(getTemplateFiles(replacements, templateRoot, outDir,
            getTemplateBasePath(template)));
      } else {
        throw new WebAppCreatorException("Cannot handle template '" + template + "' protocol: " 
            + templateUrl.getProtocol());
      }
    }
    
    return files;
  }

  public Map<String, String> getReplacements(String installPath, String theModuleName) {
    // GWT libraries
    String gwtUserPath = installPath + '/' + "gwt-user.jar";
    String gwtDevPath = installPath + '/' + "gwt-dev.jar";
    String gwtValidationPath = installPath + '/' + "validation-api-1.0.0.GA.jar";
    String gwtValidationSourcesPath = installPath + '/' + "validation-api-1.0.0.GA-sources.jar";

    // Generate a DTD reference.
    String gwtModuleDtd = "\n<!-- Using DTD from SVN 'trunk'. You probably want to change this"
        + " to a specific, release tagged, DTD -->"
        + "\n<!DOCTYPE module PUBLIC \"-//Google Inc.//DTD Google Web Toolkit "
        + About.getGwtVersionNum()
        + "//EN\" \"http://google-web-toolkit.googlecode.com/svn/trunk/"
        + "/distro-source/core/src/gwt-module.dtd\">";
    GwtVersion gwtVersion = About.getGwtVersionObject();
    if (!gwtVersion.isNoNagVersion() && !gwtVersion.equals(new GwtVersion(null))) {
      gwtModuleDtd = "\n<!--"
          + "\n  When updating your version of GWT, you should also update this DTD reference,"
          + "\n  so that your app can take advantage of the latest GWT module capabilities."
          + "\n-->"
          + "\n<!DOCTYPE module PUBLIC \"-//Google Inc.//DTD Google Web Toolkit "
          + About.getGwtVersionNum() + "//EN\""
          + "\n  \"http://google-web-toolkit.googlecode.com/svn/tags/"
          + About.getGwtVersionNum()
          + "/distro-source/core/src/gwt-module.dtd\">";
    }

    // Compute module package and name.
    int pos = theModuleName.lastIndexOf('.');
    String modulePackageName = theModuleName.substring(0, pos);
    String moduleShortName = theModuleName.substring(pos + 1);

    // Create a map of replacements
    Map<String, String> replacements = new HashMap<String, String>();

    // Compute module name and directories
    String srcFolder = templates.contains("maven") ? "src/main/java" : "src";
    String testFolder = templates.contains("maven") ? "src/test/java" : "test";
    String warFolder = templates.contains("maven") ? "src/main/webapp" : "war";

    {
      // pro-actively let ant user know that this script can also create tests.
      if (junitPath == null) {
        System.err.println("Not creating tests because -junit argument was not specified.\n");
      }

      String testTargetsBegin = "";
      String testTargetsEnd = "";
      String junitJarPath = junitPath;
      String eclipseTestDir = "";
      if (junitPath != null) {
        eclipseTestDir = "\n   <classpathentry kind=\"src\" path=\""
            + testFolder + "\"/>";
      }
      if (junitPath == null) {
        testTargetsBegin = "\n<!--"
            + "\n"
            + "Test targets suppressed because -junit argument was not specified when running"
            + " webAppCreator.\n";
        testTargetsEnd = "-->\n";
        junitJarPath = "path_to_the_junit_jar";
      }
      replacements.put("@testTargetsBegin", testTargetsBegin);
      replacements.put("@testTargetsEnd", testTargetsEnd);
      replacements.put("@junitJar", junitJarPath);
      replacements.put("@eclipseTestDir", eclipseTestDir);
    }

    replacements.put("@srcFolder", srcFolder);
    replacements.put("@testFolder", testFolder);
    replacements.put("@warFolder", warFolder);

    replacements.put("@moduleShortName", moduleShortName);
    replacements.put("@modulePackageName", modulePackageName);
    replacements.put("@moduleFolder", modulePackageName.replace('.', '/'));
    replacements.put("@moduleName", theModuleName);
    replacements.put("@clientPackage", modulePackageName + ".client");
    replacements.put("@serverPackage", modulePackageName + ".server");
    replacements.put("@sharedPackage", modulePackageName + ".shared");
    replacements.put("@gwtSdk", installPath);
    replacements.put("@gwtUserPath", gwtUserPath);
    replacements.put("@gwtDevPath", gwtDevPath);
    replacements.put("@gwtValidationPath", gwtValidationPath);
    replacements.put("@gwtValidationSourcesPath", gwtValidationSourcesPath);
    replacements.put("@gwtVersion", About.getGwtVersionNum());
    replacements.put("@gwtModuleDtd", gwtModuleDtd);
    replacements.put("@shellClass", DevMode.class.getName());
    replacements.put("@compileClass", Compiler.class.getName());
    replacements.put("@startupUrl", moduleShortName + ".html");
    replacements.put("@renameTo", moduleShortName.toLowerCase());
    replacements.put("@moduleNameJUnit", theModuleName + "JUnit");

    // Add command to copy gwt-servlet-deps.jar into libs, unless this is a
    // maven project. Maven projects should include libs as maven dependencies.
    String copyServletDeps = "";
    copyServletDeps = "<copy todir=\"" + warFolder + "/WEB-INF/lib\" "
        + "file=\"${gwt.sdk}/gwt-servlet-deps.jar\" />";
    replacements.put("@copyServletDeps", copyServletDeps);

    // Collect the list of server libs to include on the eclipse classpath.
    File libDirectory = new File(outDir + "/" + warFolder + "/WEB-INF/lib");
    StringBuilder serverLibs = new StringBuilder();
    if (libDirectory.exists()) {
      for (File file : libDirectory.listFiles()) {
        if (file.getName().toLowerCase().endsWith(".jar")) {
          serverLibs.append("   <classpathentry kind=\"lib\" path=\"war/WEB-INF/lib/");
          serverLibs.append(file.getName());
          serverLibs.append("\"/>\n");
        }
      }
    }
    replacements.put("@serverClasspathLibs", serverLibs.toString());

    String antEclipseRule = "";
    if (!templates.contains("eclipse")) {
      /*
       * Generate a rule into the build file that allows for the generation of
       * an eclipse project later on. This is primarily for distro samples. This
       * is a quick and dirty way to inject a build rule, but it works.
       */
      antEclipseRule = "\n\n"
          + "  <target name=\"eclipse.generate\" depends=\"libs\" description"
          + "=\"Generate eclipse project\">\n"
          + "    <java failonerror=\"true\" fork=\"true\" classname=\""
          + this.getClass().getName() + "\">\n" + "      <classpath>\n"
          + "        <path refid=\"project.class.path\"/>\n"
          + "      </classpath>\n" + "      <arg value=\"-XonlyEclipse\"/>\n"
          + "      <arg value=\"-ignore\"/>\n" + "      <arg value=\""
          + theModuleName + "\"/>\n" + "    </java>\n" + "  </target>";
    } else {
      antEclipseRule = "";
    }
    replacements.put("@antEclipseRule", antEclipseRule);
    return replacements;
  }

  /**
   * Create the sample app.
   * 
   * @throws IOException if any disk write fails
   * @throws WebAppCreatorException if any tag expansion of template processing fails
   * @deprecated as of GWT 2.1, replaced by {@link #doRun(String)}
   */
  @Deprecated
  protected void doRun() throws IOException, WebAppCreatorException {
    doRun(Utility.getInstallPath());
  }

  /**
   * Create the sample app.
   * 
   * @param installPath directory containing GWT libraries
   * @throws IOException if any disk write fails
   * @throws WebAppCreatorException  if any tag expansion of template processing fails
   */
  protected void doRun(String installPath) throws IOException, WebAppCreatorException {
    for (Procrastinator toDo : argProcessingToDos) {
      toDo.stopProcratinating();
    }

    // Maven projects do not need Ant nor Eclipse files
    if (templates.contains("maven")) {
      junitPath = "junit-provided-by-maven";

      if (templates.contains("eclipse")) {
        System.err.println("'maven' template is being generated removing 'eclipse'"
            + " template from generated output.");
        templates.remove("eclipse");            
      }
      if (templates.contains("ant")) {
        System.err.println("'maven' template is being generated removing 'ant'"
            + " template from generated output.");
        templates.remove("ant");
      }
    }

    // Eagerly look for test templates
    if (junitPath != null) {
      ArrayList<String> testTemplates = new ArrayList<String>();
      for (String template : templates) {
        String testTemplateName = "_" + template + "-test";
        if (getTemplateRoot(testTemplateName) != null) {
          testTemplates.add(testTemplateName);
        }
      }
      templates.addAll(testTemplates);
    }

    System.out.println("Generating from templates: " + templates);

    // Generate string replacements 
    Map<String, String> replacements = getReplacements(installPath, moduleName);

    // Create a list with the files to create
    List<FileCreator> files = getFiles(replacements);

    // copy source files, replacing the content as needed
    for (FileCreator fileCreator : files) {
      URL url = WebAppCreator.class.getResource(fileCreator.sourceName);
      if (url == null) {
        throw new WebAppCreatorException("Could not find " + fileCreator.sourceName);
      }
      File file = Utility.createNormalFile(fileCreator.destDir,
          fileCreator.destName, overwrite, ignore);
      if (file == null) {
        continue;
      }
      if (fileCreator.isBinary) {
        byte[] data = Util.readURLAsBytes(url);
        Utility.writeTemplateBinaryFile(file, data);
      } else {
        String data = Util.readURLAsString(url);
        Utility.writeTemplateFile(file, data, replacements);
      }
    }
  }

  protected boolean run() {
    try {
      doRun(Utility.getInstallPath());
      return true;
    } catch (IOException e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      return false;
    } catch (WebAppCreatorException e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      return false;
    }
  }

  private Collection<? extends FileCreator> getTemplateFiles(
      Map<String, String> replacements, File srcDirectory, File destDirectory,
      String templateClassRoot) throws IOException {
    List<FileCreator> files = new ArrayList<FileCreator>();
    
    File[] filesInDir = srcDirectory.listFiles();

    for (File srcFile : filesInDir) {
      String replacedName = replaceFileName(replacements, srcFile.getName());
      
      if (srcFile.isDirectory()) {
        File newDirectory = Utility.getDirectory(destDirectory, replacedName, true);
        files.addAll(getTemplateFiles(replacements, srcFile, newDirectory, templateClassRoot
            + srcFile.getName() + "/"));
      } else if (srcFile.getName().endsWith("src")) {
        String srcFilename = templateClassRoot + srcFile.getName();
        String destName = replacedName.substring(0, replacedName.length() - 3);
        files.add(new FileCreator(destDirectory, destName, srcFilename, false));
      } else if (srcFile.getName().endsWith("bin")) {
        String srcFilename = templateClassRoot + srcFile.getName();
        String destName = replacedName.substring(0, replacedName.length() - 3);
        files.add(new FileCreator(destDirectory, destName, srcFilename, true));
      } // else ... ignore everything not a directory, "*src" nor "*bin"
    }
    
    return files;
  }

  private Collection<? extends FileCreator> getTemplateFilesFromZip(
      Map<String, String> replacements, URL zipUrl, File destDirectory)
      throws WebAppCreatorException, IOException {
    String zipPath = zipUrl.getFile();
    int separator = zipPath.indexOf('!');

    if (separator == -1) {
      throw new WebAppCreatorException("Error opening template zip file. '!' not found in "
          + zipUrl);
    }

    String zipFilename = zipPath.substring(0, separator);
    String templateDirName = zipPath.substring(separator + 2);
    ZipFile zipFile;

    try {
      zipFile = new ZipFile(new File(new URI(zipFilename)));
    } catch (URISyntaxException e) {
      throw new WebAppCreatorException("Could not open Zip file. Malformed URI", e);
    }

    Enumeration<? extends ZipEntry> allZipEntries = zipFile.entries();

    ArrayList<FileCreator> templateEntries = new ArrayList<FileCreator>(); 

    while (allZipEntries.hasMoreElements()) {
      ZipEntry entry = allZipEntries.nextElement();
      String fullName = entry.getName();
      if (fullName.startsWith(templateDirName + "/")) {
        String relativeName = fullName.substring(templateDirName.length());
        String replacedName = replaceFileName(replacements, relativeName);
        if (entry.isDirectory()) {
          Utility.getDirectory(destDirectory, replacedName, true);
        } else if (fullName.endsWith("src")) {
          // remove the src suffix 
          String destName = replacedName.substring(0, replacedName.length() - 3);
          templateEntries.add(new FileCreator(destDirectory, destName, "/" + fullName, false));
        } else if (fullName.endsWith("bin")) {
          String destName = replacedName.substring(0, replacedName.length() - 3);
          templateEntries.add(new FileCreator(destDirectory, destName, "/" + fullName, true));
        }
      } // else ... ignore everything not a directory, "*src" nor "*bin"
    }

    return templateEntries;
  }

}
