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
package com.google.gwt.tools.apichecker;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.dev.javac.Shared;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ArgHandlerString;
import com.google.gwt.util.tools.ToolBase;

import org.apache.tools.ant.types.ZipScanner;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * {@link ApiCompatibilityChecker} Main class to check if the new API is
 * compatible with the existing API.
 * 
 * <p>
 * To compute API diffs, follow these 2 steps:
 * <ol>
 * <li>for each of the two repositories, construct an {@link ApiContainer}
 * <li>call getApiDiff on the {@code ApiDiffGenerator}
 * </ol>
 * </p>
 * 
 * <p>
 * An {@code ApiContainer} object is a list of {@link ApiPackage} objects.
 * {@code ApiPackage} objects themselves are list of {@link ApiClass} objects.
 * {@code ApiClass} objects contain list of {@code ApiConstructor},
 * {@code ApiMethod}, and {@code JField} objects.
 * </p>
 * 
 * <p>
 * Each {@code ApiDiffGenerator} object computes the list of intersecting and
 * missing {@link ApiPackageDiffGenerator} objects. Each
 * {@code ApiPackageDiffGenerator} object in turn computes the list of
 * intersecting and missing {@link ApiClassDiffGenerator} objects. Each
 * {@code ApiClassDiffGenerator} object in turn computes the list of
 * intersecting and missing API members. The members are represented by
 * {@link ApiConstructor} for constructors, {@link ApiMethod} for methods, and
 * {@link ApiField} for fields.
 * </p>
 * 
 * <p>
 * For each intersecting API member, a list of {@link ApiChange} objects is
 * stored. Each ApiChange object encodes a specific {@code ApiChange} like
 * adding the 'final' keyword to the API member.
 * 
 */
public class ApiCompatibilityChecker extends ToolBase {

  // TODO(amitmanjhi): check gwt's dev/core/src files. Would need the ability to
  // build TypeOracle from class files

  // TODO(amitmanjhi): better handling of exceptions and exception-chaining.

  static class FileResource extends Resource {
    private final File file;
    private final String path;

    public FileResource(File file, String path) {
      this.file = file;
      this.path = path;
      assert path.endsWith(".java");
      assert file.getAbsolutePath().endsWith(path);
    }

    @Override
    public long getLastModified() {
      return file.lastModified();
    }

    @Override
    public String getLocation() {
      return file.getAbsolutePath();
    }

    @Override
    public String getPath() {
      return path;
    }

    @Override
    public InputStream openContents() {
      try {
        return new FileInputStream(file);
      } catch (FileNotFoundException e) {
        throw new RuntimeException("Unable to open file '" + file.getAbsolutePath() + "'", e);
      }
    }

    @Override
    public boolean wasRerooted() {
      return false;
    }
  }

  static class StaticResource extends Resource {

    private final long lastModified;
    private final String source;
    private final String typeName;

    public StaticResource(String typeName, String source, long lastModified) {
      this.typeName = typeName;
      this.source = source;
      this.lastModified = lastModified;
    }

    @Override
    public long getLastModified() {
      return lastModified;
    }

    @Override
    public String getLocation() {
      return "/mock/" + getPath();
    }

    @Override
    public String getPath() {
      return Shared.toPath(typeName);
    }

    @Override
    public InputStream openContents() {
      return new ByteArrayInputStream(Util.getBytes(source));
    }

    @Override
    public boolean wasRerooted() {
      return false;
    }
  }

  /**
   * Class that specifies a set of
   * {@link com.google.gwt.dev.javac.CompilationUnit CompilationUnit} read from
   * jar files.
   */
  private static class JarFileResources extends Resources {
    private static final String MOCK_PREFIX = "/mock/";
    private static final int MOCK_PREFIX_LENGTH = MOCK_PREFIX.length();

    private final ZipScanner excludeScanner;
    private final Set<String> includedPaths;
    private final JarFile jarFiles[];
    private Set<Resource> resources = null;

    JarFileResources(JarFile[] jarFiles, Set<String> includedPaths, Set<String> excludedPaths,
        TreeLogger logger) {
      super(logger);
      this.jarFiles = jarFiles;
      this.includedPaths = includedPaths;

      // initialize the ant scanner
      excludeScanner = new ZipScanner();
      List<String> list = new ArrayList<String>(excludedPaths);
      excludeScanner.setIncludes(list.toArray(new String[list.size()]));
      excludeScanner.addDefaultExcludes();
      excludeScanner.setCaseSensitive(true);
      excludeScanner.init();
    }

    @Override
    public Set<Resource> getResources() throws IOException {
      if (resources != null) {
        return resources;
      }

      resources = new HashSet<Resource>();
      for (JarFile jarFile : jarFiles) {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
          JarEntry jarEntry = entries.nextElement();
          String fileName = jarEntry.toString();
          if (fileName.startsWith(MOCK_PREFIX)) {
            fileName = fileName.substring(MOCK_PREFIX_LENGTH);
          }
          if (fileName.endsWith(".java") && isIncluded(fileName)) {
            // add this compilation unit
            String fileContent = getFileContentsFromJar(jarFile, jarEntry);
            String packageName = extractPackageName(new StringReader(fileContent));
            if (packageName == null) {
              logger.log(TreeLogger.WARN, "Not adding file = " + fileName
                  + ", because packageName = null", null);
            } else {
              if (isValidPackage(packageName, fileName)) {
                // Add if package and fileNames are okay
                long lastModified = jarEntry.getTime();
                if (lastModified < 0) {
                  lastModified = System.currentTimeMillis();
                }
                resources.add(new StaticResource(packageName + "." + getClassName(fileName),
                    fileContent, lastModified));
                logger.log(TreeLogger.DEBUG, "adding pkgName = " + packageName + ", file = "
                    + fileName, null);
              } else {
                logger.log(TreeLogger.SPAM, " not adding file " + fileName, null);
              }
            }
          }
        }
      }
      return resources;
    }

    String getFileContentsFromJar(JarFile jarFile, JarEntry jarEntry) throws IOException {
      StringBuffer fileContent = new StringBuffer();
      InputStream is = jarFile.getInputStream(jarEntry);
      BufferedInputStream bis = new BufferedInputStream(is);
      int length = 500;
      byte buf[] = new byte[length];
      int count = 0;
      while ((count = bis.read(buf, 0, length)) != -1) {
        fileContent.append(new String(buf, 0, count));
        buf = new byte[length];
      }
      bis.close();
      is.close();
      return fileContent.toString();
    }

    private String getClassName(String fileName) {
      int index = fileName.lastIndexOf("/");
      int endOffset = 0;
      if (fileName.endsWith(".java")) {
        endOffset = 5;
      }
      return fileName.substring(index + 1, fileName.length() - endOffset);
    }

    private boolean isIncluded(String fileName) {
      if (excludeScanner.match(fileName)) {
        logger.log(TreeLogger.SPAM, fileName + " is excluded");
        return false;
      }
      for (String includedPath : includedPaths) {
        if (fileName.startsWith(includedPath)) {
          logger.log(TreeLogger.SPAM, fileName + " is not excluded, and is included by "
              + includedPath);
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Abstract internal class that specifies a set of
   * {@link com.google.gwt.dev.javac.CompilationUnit CompilationUnit}.
   */
  private abstract static class Resources {
    protected final TreeLogger logger;

    Resources(TreeLogger logger) {
      this.logger = logger;
    }

    public abstract Set<Resource> getResources() throws NotFoundException, IOException,
        UnableToCompleteException;

    // TODO (amitmanjhi): remove this code. use TypeOracle functionality
    // instead.
    protected String extractPackageName(Reader reader) throws IOException {
      BufferedReader br = new BufferedReader(reader);
      String str = null;
      while ((str = br.readLine()) != null) {
        if (str.indexOf("package") != 0) {
          continue;
        }
        String parts[] = str.split("[\b\t\n\r ]+");
        if ((parts.length == 2) && parts[0].equals("package")) {
          return parts[1].substring(0, parts[1].length() - 1); // the ; char
        }
      }
      return null;
    }

    protected File getFileFromName(String tag, String pathName) throws FileNotFoundException {
      File file = new File(pathName);
      if (!file.exists()) {
        throw new FileNotFoundException(tag + "file " + pathName + " not found");
      }
      return file;
    }

    // TODO (amitmanjhi): remove this code. use TypeOracle functionality
    // instead.
    protected boolean isValidPackage(String packageName, String filePath) {
      logger
          .log(TreeLogger.SPAM, "packageName = " + packageName + ", filePath = " + filePath, null);
      if (packageName == null) {
        return false;
      }
      int lastSlashPosition = filePath.lastIndexOf("/");
      if (lastSlashPosition == -1) {
        return false;
      }
      String dirPath = filePath.substring(0, lastSlashPosition);
      String packageNameAsPath = packageName.replace('.', '/');
      logger.log(TreeLogger.SPAM, "packageNameAsPath " + packageNameAsPath + ", dirPath = "
          + dirPath, null);
      return dirPath.endsWith(packageNameAsPath);
    }
  }

  /**
   * Class that specifies a set of
   * {@link com.google.gwt.dev.javac.CompilationUnit CompilationUnit} read from
   * the file-system.
   */
  private static class SourceFileResources extends Resources {
    private final ZipScanner excludeScanner;
    private final Set<File> includedPaths;
    private Set<Resource> units = null;

    SourceFileResources(String dirRoot, Set<String> includedPathsAsString,
        Set<String> excludedPathsAsString, TreeLogger logger) throws FileNotFoundException,
        IOException {
      super(logger);
      if (dirRoot == null) {
        dirRoot = "";
      }
      includedPaths = new HashSet<File>();
      for (String includedPath : includedPathsAsString) {
        includedPaths.add(getFileFromName("source file: ", dirRoot + includedPath));
      }

      String fullExcludedPaths[] = new String[excludedPathsAsString.size()];
      int count = 0;
      String dirRootAbsolutePath = getFileFromName("dirRoot: ", dirRoot).getAbsolutePath();
      for (String excludedPath : excludedPathsAsString) {
        fullExcludedPaths[count++] = dirRootAbsolutePath + "/" + excludedPath;
      }

      // initialize the ant scanner
      excludeScanner = new ZipScanner();
      if (fullExcludedPaths.length > 0) {
        excludeScanner.setIncludes(fullExcludedPaths);
      }
      excludeScanner.addDefaultExcludes();
      excludeScanner.setCaseSensitive(true);
      excludeScanner.init();
    }

    @Override
    public Set<Resource> getResources() throws NotFoundException, IOException,
        UnableToCompleteException {
      if (units != null) {
        return units;
      }

      units = new HashSet<Resource>();
      for (File sourceFile : includedPaths) {
        updateCompilationUnitsInPath(sourceFile);
      }
      return units;
    }

    private boolean isExcludedFile(String fileName) {
      String pattern = "file:";
      if (fileName.indexOf(pattern) == 0) {
        fileName = fileName.substring(pattern.length());
      }
      return excludeScanner.match(fileName);
    }

    private void updateCompilationUnitsInPath(File sourcePathEntry) throws NotFoundException,
        IOException, UnableToCompleteException {
      logger.log(TreeLogger.SPAM, "entering addCompilationUnitsInPath, file = " + sourcePathEntry,
          null);
      File[] files = sourcePathEntry.listFiles();
      if (files == null) { // No files found.
        return;
      }

      for (int i = 0; i < files.length; i++) {
        final File file = files[i];
        logger.log(TreeLogger.SPAM, "deciding the fate of file " + file, null);
        // Ignore files like .svn and .cvs
        if (file.getName().startsWith(".") || file.getName().equals("CVS")) {
          continue;
        }
        if (isExcludedFile(file.getAbsolutePath())) {
          // do not process the subtree
          logger.log(TreeLogger.DEBUG, "not traversing " + file.toURI().toURL(), null);
          continue;
        }
        if (file.isFile()) {
          String fileName = file.getName();
          if (file.getName().endsWith("java")) {
            String className = file.getName().substring(0, fileName.length() - 5);
            String pkgName = extractPackageName(new FileReader(file));
            if (pkgName == null) {
              logger.log(TreeLogger.WARN, "Not adding file = " + file.toString()
                  + ", because packageName = null", null);
            } else {
              if (isValidPackage(pkgName, sourcePathEntry.toURI().toURL().toString())) {
                // Add if the package and fileNames are okay
                String typeName = Shared.makeTypeName(pkgName, className);
                units.add(new FileResource(file, Shared.toPath(typeName)));
                logger.log(TreeLogger.DEBUG, "adding pkgName = " + pkgName + ", file = "
                    + file.toString(), null);
              } else {
                logger.log(TreeLogger.SPAM, " not adding file " + file.toURI().toURL(), null);
              }
            }
          }
        } else {
          // Recurse into subDirs
          updateCompilationUnitsInPath(file);
        }
      }
    }
  }

  // currently doing only source_compatibility. true by default.
  public static final boolean API_SOURCE_COMPATIBILITY = true;

  /**
   * Prints which class the member was declared in plus the string message in
   * ApiChange, false by default.
   */
  public static boolean DEBUG = false;

  public static final boolean DEBUG_DUPLICATE_REMOVAL = false;

  /**
   * Flag for filtering duplicates, true by default. Just for debugging in rare
   * cases.
   */
  public static final boolean FILTER_DUPLICATES = true;

  /**
   * Print APIs common in the two repositories. Should be false by default.
   */
  public static final boolean PRINT_COMPATIBLE = false;
  /**
   * Print APIs common in the two repositories. Should be false by default.
   */
  public static final boolean PRINT_COMPATIBLE_WITH = false;

  /**
   * Flag for debugging whether typeOracle builds.
   */
  public static final boolean PROCESS_EXISTING_API = true;

  /**
   * Flag for debugging whether typeOracle builds.
   */
  public static final boolean PROCESS_NEW_API = true;

  // true by default
  public static final boolean REMOVE_NON_SUBCLASSABLE_ABSTRACT_CLASS_FROM_API = true;

  // remove duplicates by default
  public static Collection<ApiChange> getApiDiff(ApiContainer newApi, ApiContainer existingApi,
      Set<String> whiteList) throws NotFoundException {
    ApiDiffGenerator apiDiff = new ApiDiffGenerator(newApi, existingApi);
    return getApiDiff(apiDiff, whiteList, FILTER_DUPLICATES);
  }

  public static void main(String args[]) {
    try {
      ApiCompatibilityChecker checker = new ApiCompatibilityChecker();
      if (!checker.processArgs(args)) {
        // if we couldn't parse arguments, return non-zero so the build breaks
        System.exit(1);
      }

      ApiContainer newApi = null, existingApi = null;

      AbstractTreeLogger logger = new PrintWriterTreeLogger();
      logger.setMaxDetail(checker.type);
      logger.log(TreeLogger.INFO, "gwtDevJar = " + checker.gwtDevJar + ", userJar = "
          + checker.gwtUserJar + ", refjars = " + Arrays.toString(checker.refJars)
          + ", logLevel = " + checker.type + ", printAllApi = " + checker.printAllApi, null);

      Set<String> excludedPackages = checker.getSetOfExcludedPackages(checker.configProperties);
      if (PROCESS_NEW_API) {
        Set<Resource> resources = new HashSet<Resource>();
        resources.addAll(new SourceFileResources(checker.configProperties
            .getProperty("dirRoot_new"), checker.getConfigPropertyAsSet("sourceFiles_new"), checker
            .getConfigPropertyAsSet("excludedFiles_new"), logger).getResources());
        resources.addAll(checker.getJavaxValidationCompilationUnits(logger));
        resources.addAll(checker.getGwtCompilationUnits(logger));
        newApi =
            new ApiContainer(checker.configProperties.getProperty("name_new"), resources,
                excludedPackages, logger);
        if (checker.printAllApi) {
          logger.log(TreeLogger.INFO, newApi.getApiAsString());
        }
      }
      if (PROCESS_EXISTING_API) {
        Set<Resource> resources = new HashSet<Resource>();
        if (checker.refJars == null) {
          resources.addAll(new SourceFileResources(checker.configProperties
              .getProperty("dirRoot_old"), checker.getConfigPropertyAsSet("sourceFiles_old"),
              checker.getConfigPropertyAsSet("excludedFiles_old"), logger).getResources());
        } else {
          resources.addAll(new JarFileResources(checker.refJars, checker
              .getConfigPropertyAsSet("sourceFiles_old"), checker
              .getConfigPropertyAsSet("excludedFiles_old"), logger).getResources());
        }
        resources.addAll(checker.getJavaxValidationCompilationUnits(logger));
        resources.addAll(checker.getGwtCompilationUnits(logger));
        existingApi =
            new ApiContainer(checker.configProperties.getProperty("name_old"), resources,
                excludedPackages, logger);
        if (checker.printAllApi) {
          logger.log(TreeLogger.INFO, existingApi.getApiAsString());
        }
      }

      if (PROCESS_NEW_API && PROCESS_EXISTING_API) {
        Collection<ApiChange> apiDifferences = getApiDiff(newApi, existingApi, checker.whiteList);
        for (ApiChange apiChange : apiDifferences) {
          System.out.println(apiChange);
        }
        if (apiDifferences.size() == 0) {
          System.out.println("API compatibility check SUCCESSFUL");
        } else {
          System.out.println("API compatibility check FAILED");
        }
        System.exit(apiDifferences.size() == 0 ? 0 : 1);
      }

    } catch (Throwable t) {
      // intercepting all exceptions in main, because I have to exit with -1 so
      // that the build breaks.
      try {
        t.printStackTrace();
        System.err
            .println("To view the help for this tool, execute this tool without any arguments");
      } finally {
        System.exit(-1);
      }
    }
  }

  // interface for testing, since do not want to build ApiDiff frequently
  static Collection<ApiChange> getApiDiff(ApiDiffGenerator apiDiff, Set<String> whiteList,
      boolean removeDuplicates) throws NotFoundException {
    Collection<ApiChange> collection = apiDiff.getApiDiff();
    if (removeDuplicates) {
      collection = apiDiff.removeDuplicates(collection);
    }
    Set<String> matchedWhiteList = new HashSet<String>();

    Collection<ApiChange> prunedCollection = new ArrayList<ApiChange>();
    for (ApiChange apiChange : collection) {
      String apiChangeAsString = apiChange.getStringRepresentationWithoutMessage();
      apiChangeAsString = apiChangeAsString.trim();
      if (whiteList.contains(apiChangeAsString)) {
        matchedWhiteList.add(apiChangeAsString);
        continue;
      }
      // check for Status.Compatible and Status.Compatible_with
      if (!PRINT_COMPATIBLE && apiChange.getStatus().equals(ApiChange.Status.COMPATIBLE)) {
        continue;
      }
      if (!PRINT_COMPATIBLE_WITH && apiChange.getStatus().equals(ApiChange.Status.COMPATIBLE_WITH)) {
        continue;
      }
      prunedCollection.add(apiChange);
    }
    whiteList.removeAll(matchedWhiteList);
    if (whiteList.size() > 0) {
      List<String> al = new ArrayList<String>(whiteList);
      Collections.sort(al);
      System.err.println("ApiChanges [");
      for (String apiChange : al) {
        System.err.println(apiChange);
      }
      System.err.println("],  not found. Are you using a properly formatted configuration file?");
    }
    List<ApiChange> apiChangeList = new ArrayList<ApiChange>(prunedCollection);
    Collections.sort(apiChangeList);
    return apiChangeList;
  }

  private Properties configProperties;
  private JarFile[] extraSourceJars;
  private JarFile gwtDevJar;
  private JarFile gwtUserJar;

  // prints the API of the two containers, false by default.
  private boolean printAllApi = false;
  private JarFile refJars[];
  // default log output
  private TreeLogger.Type type = TreeLogger.WARN;
  private Set<String> whiteList;

  protected ApiCompatibilityChecker() {

    /*
     * register handlers for gwtDevJar, gwtUserJar, refJar, logLevel,
     * printAllApi, configProperties
     */

    // handler for gwtDevJar
    registerHandler(new ArgHandlerString() {

      @Override
      public String getPurpose() {
        return "Path of the gwt dev jar";
      }

      @Override
      public String getTag() {
        return "-gwtDevJar";
      }

      @Override
      public String[] getTagArgs() {
        return new String[] {"gwt_dev_jar_path"};
      }

      @Override
      public boolean setString(String str) {
        gwtDevJar = getJarFromString(str);
        return gwtDevJar != null;
      }
    });

    // handler for gwtUserJar
    registerHandler(new ArgHandlerString() {

      @Override
      public String getPurpose() {
        return "Path of the gwt user jar";
      }

      @Override
      public String getTag() {
        return "-gwtUserJar";
      }

      @Override
      public String[] getTagArgs() {
        return new String[] {"gwt_user_jar_path"};
      }

      @Override
      public boolean setString(String str) {
        gwtUserJar = getJarFromString(str);
        return gwtUserJar != null;
      }
    });

    // handler for refJar
    registerHandler(new ArgHandlerString() {
      @Override
      public String getPurpose() {
        return "Path of the reference jar";
      }

      @Override
      public String getTag() {
        return "-refJar";
      }

      @Override
      public String[] getTagArgs() {
        return new String[] {"reference_jar_path"};
      }

      @Override
      public boolean setString(String str) {
        String refJarStrings[] = str.split(System.getProperty("path.separator"));
        refJars = new JarFile[refJarStrings.length];
        int count = 0;
        for (String refJarString : refJarStrings) {
          refJars[count++] = getJarFromString(refJarString);
          if (refJars[count - 1] == null) {
            return false; // bail-out early
          }
        }
        return refJars != null;
      }
    });

    // handler for logLevel
    registerHandler(new ArgHandlerString() {

      @Override
      public String getPurpose() {
        return "Sets the log level of the TreeLogger";
      }

      @Override
      public String getTag() {
        return "-logLevel";
      }

      @Override
      public String[] getTagArgs() {
        String values[] = new String[TreeLogger.Type.values().length];
        int count = 0;
        for (TreeLogger.Type tempType : TreeLogger.Type.values()) {
          values[count++] = tempType.name();
        }
        return values;
      }

      @Override
      public boolean setString(String str) {
        for (TreeLogger.Type tempType : TreeLogger.Type.values()) {
          if (tempType.name().equals(str)) {
            type = tempType;
            return true;
          }
        }
        return false;
      }

    });

    // handler for printAllApi
    registerHandler(new ArgHandlerFlag() {

      @Override
      public String getPurpose() {
        return "Prints all api";
      }

      @Override
      public String getTag() {
        return "-printAllApi";
      }

      @Override
      public boolean setFlag() {
        printAllApi = true;
        return true;
      }

    });

    // handler for configFile
    registerHandler(new ArgHandlerString() {

      @Override
      public String getPurpose() {
        return "Path of the configuration file";
      }

      @Override
      public String getTag() {
        return "-configFile";
      }

      @Override
      public String[] getTagArgs() {
        return new String[] {"config_file_path"};
      }

      @Override
      public boolean isRequired() {
        return true;
      }

      @Override
      public boolean setString(String str) {
        setPropertiesAndWhitelist(str);
        return configProperties != null && whiteList != null;
      }
    });

    registerHandler(new ArgHandlerString() {

      @Override
      public String getPurpose() {
        return "The location of the javax.validation sources";
      }

      @Override
      public String getTag() {
        return "-validationSourceJars";
      }

      @Override
      public String[] getTagArgs() {
        return new String[] {"jar1.jar:jar2.jar"};
      }

      @Override
      public boolean setString(String str) {
        boolean success = true;
        String[] parts = str.split(System.getProperty("path.separator"));
        extraSourceJars = new JarFile[parts.length];
        for (int i = 0, j = parts.length; i < j; i++) {
          extraSourceJars[i] = getJarFromString(parts[i]);
        }
        return success;
      }
    });
  }

  @Override
  public void printHelp() {
    super.printHelp();
    StringBuffer sb = new StringBuffer();
    sb.append("\n");
    sb.append("The config file must specify two repositories of java source files: ");
    sb.append("'_old' and '_new', which are to be compared for API source compatibility.\n");
    sb.append("An optional whitelist is present at the end of ");
    sb.append("the config file. The format of the whitelist is same as the output of ");
    sb.append("the tool without the whitelist.\n");
    sb.append("Each repository is specified by the following four properties:\n");
    sb.append("name             specifies how the api should be refered to in the output\n");
    sb.append("dirRoot          optional argument that specifies the base directory of all other file/directory names\n");
    sb.append("sourceFiles      a colon-separated list of files/directories that specify the roots of the the filesystem trees to be included.\n");
    sb.append("excludeFiles     a colon-separated lists of ant patterns to exclude");

    sb.append("\n\n");
    sb.append("Example api.conf file:\n");
    sb.append("name_old         gwtEmulator");
    sb.append("\n");
    sb.append("dirRoot_old      ./");
    sb.append("\n");
    sb.append("sourceFiles_old  dev/core/super:user/super:user/src");
    sb.append("\n");
    sb.append("excludeFiles_old user/super/com/google/gwt/junit/*.java");
    sb.append("\n\n");

    sb.append("name_new         gwtEmulatorCopy");
    sb.append("\n");
    sb.append("dirRoot_new      ../gwt-14/");
    sb.append("\n");
    sb.append("sourceFiles_new  dev/core:user/super:user/src");
    sb.append("\n");
    sb.append("excludeFiles_new user/super/com/google/gwt/junit/*.java");
    sb.append("\n\n");

    System.err.println(sb.toString());
  }

  protected JarFile getJarFromString(String str) {
    try {
      return new JarFile(str);
    } catch (IOException ex) {
      System.err.println("exception in getting jar from fileName: " + str + ", message: "
          + ex.getMessage());
      return null;
    }
  }

  protected void setPropertiesAndWhitelist(String fileName) throws IllegalArgumentException {
    try {
      // load config properties
      FileInputStream fis = new FileInputStream(fileName);
      configProperties = new Properties();
      configProperties.load(fis);
      fis.close();

      // load whitelist
      FileReader fr = new FileReader(fileName);
      whiteList = readWhiteListFromFile(fr);
      fr.close();
    } catch (IOException ex) {
      System.err.println("Have you specified the path of the config file correctly?");
      throw new IllegalArgumentException(ex);
    }
  }

  private Set<String> getConfigPropertyAsSet(String key) {
    Set<String> set = new HashSet<String>();
    String propertyValue = configProperties.getProperty(key);
    if (propertyValue == null) {
      return set;
    }
    for (String element : propertyValue.split(":")) {
      element = element.trim();
      set.add(element);
    }
    return set;
  }

  private Set<Resource> getGwtCompilationUnits(TreeLogger logger) throws FileNotFoundException,
      IOException, NotFoundException, UnableToCompleteException {
    Set<Resource> resources = new HashSet<Resource>();
    if (gwtDevJar == null || gwtUserJar == null) {
      if (gwtDevJar != null) {
        System.err.println("Argument gwtUserJar must be provided for gwtDevJar to be used");
      }
      if (gwtUserJar != null) {
        System.err.println("Argument gwtDevJar must be provided for gwtUserJar to be used");
      }
      return resources;
    }
    // gwt-user.jar
    Set<String> gwtIncludedPaths =
        new HashSet<String>(Arrays.asList(new String[] {"com/google/gwt", "com/google/web"}));
    Set<String> gwtExcludedPaths =
        new HashSet<String>(
            Arrays
                .asList(new String[] {
                    "com/google/gwt/benchmarks",
                    "com/google/gwt/i18n/rebind",
                    "com/google/gwt/i18n/tools",
                    "com/google/gwt/json",
                    "com/google/gwt/junit",
                    "com/google/gwt/user/client/rpc/core/java/util/LinkedHashMap_CustomFieldSerializer.java",
                    "com/google/gwt/user/rebind", "com/google/gwt/user/server",
                    "com/google/gwt/user/tools",}));
    Resources cu =
        new JarFileResources(new JarFile[] {gwtUserJar}, gwtIncludedPaths, gwtExcludedPaths, logger);
    resources.addAll(cu.getResources());

    // gwt-dev-*.jar
    gwtIncludedPaths =
        new HashSet<String>(Arrays.asList(new String[] {
            "com/google/gwt/core/client", "com/google/gwt/dev/jjs/intrinsic/com/google/gwt/lang",
            "com/google/gwt/lang",}));
    cu =
        new JarFileResources(new JarFile[] {gwtDevJar}, gwtIncludedPaths, new HashSet<String>(),
            logger);
    resources.addAll(cu.getResources());
    return resources;
  }

  /**
   * This is a hack to make the ApiChecker able to find the javax.validation
   * sources, which we include through an external jar file.
   */
  private Set<Resource> getJavaxValidationCompilationUnits(TreeLogger logger)
      throws UnableToCompleteException, NotFoundException, IOException {
    Set<Resource> resources = new HashSet<Resource>();
    if (extraSourceJars != null) {
      Resources extra = new JarFileResources(
          extraSourceJars,
          Collections.singleton(""), 
          new HashSet<String>(Arrays.asList(
              "javax/validation/Configuration.java",
              "javax/validation/MessageInterpolator.java",
              "javax/validation/Validation.java",
              "javax/validation/ValidatorContext.java",
              "javax/validation/ValidatorFactory.java",
              "javax/validation/ValidationProviderResolver.java",
              "javax/validation/bootstrap/GenericBootstrap.java",
              "javax/validation/bootstrap/ProviderSpecificBootstrap.java",
              "javax/validation/constraints/Pattern.java",
              "javax/validation/spi/BootstrapState.java",
              "javax/validation/spi/ConfigurationState.java",
              "javax/validation/spi/ValidationProvider.java")), 
          logger);
      Set<Resource> loaded = extra.getResources();
      System.out.println("Found " + loaded.size() + " new resources");
      resources.addAll(loaded);
    }
    return resources;
  }

  /*
   * excludedPackages is used in only one place: to determine whether some class
   * is an api class or not
   */
  private Set<String> getSetOfExcludedPackages(Properties config) {
    String allExcludedPackages = config.getProperty("excludedPackages");
    if (allExcludedPackages == null) {
      allExcludedPackages = "";
    }
    String excludedPackagesArray[] = allExcludedPackages.split(":");
    return new HashSet<String>(Arrays.asList(excludedPackagesArray));
  }

  /**
   * Each whiteList element is an {@link ApiElement} and
   * {@link ApiChange.Status} separated by space. For example,
   * "java.util.ArrayList::size() MISSING". The {@code ApiElement} is
   * represented as the string obtained by invoking the getRelativeSignature()
   * method on {@link ApiElement}.
   */
  private Set<String> readWhiteListFromFile(FileReader fr) throws IOException {
    Set<String> hashSet = new HashSet<String>();
    BufferedReader br = new BufferedReader(fr);
    String str = null;
    while ((str = br.readLine()) != null) {
      str = str.trim();
      // ignore comments
      if (str.startsWith("#")) {
        continue;
      }
      String splits[] = str.split(" ");
      if (splits.length > 1 && ApiChange.contains(splits[1])) {
        String identifier = splits[0] + ApiDiffGenerator.DELIMITER + splits[1];
        hashSet.add(identifier.trim());
      }
    }
    return hashSet;
  }

}
