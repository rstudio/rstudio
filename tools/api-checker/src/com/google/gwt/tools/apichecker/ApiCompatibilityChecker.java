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
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link ApiCompatibilityChecker} Main class to check if the new API is
 * compatible with the existing API.
 * 
 * 
 * <p>
 * To compute API diffs, follow these 2 steps:
 * <ol>
 * <li> for each of the two repositories, construct an {@link ApiContainer}
 * <li> call getApiDiff on the {@code ApiDiffGenerator}
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
public class ApiCompatibilityChecker {

  // TODO(amitmanjhi): use ToolBase for command-line processing

  // TODO(amitmanjhi): check gwt's dev/core/src files. Would need the ability to
  // build TypeOracle from class files

  // TODO(amitmanjhi): ignore API breakages due to impl package. More generally,
  // white-list of packages that should not be checked.

  // TODO(amitmanjhi): better handling of exceptions and exception-chaining.

  // currently doing only source_compatibility. true by default.
  public static final boolean API_SOURCE_COMPATIBILITY = true;

  // prints which class the member was declared in, false by default
  public static final boolean DEBUG = false;

  // prints the API of the two containers, false by default.
  public static final boolean DEBUG_PRINT_ALL_API = false;

  // these two parameters print APIs common in the two repositories. Should be
  // false by default.
  public static final boolean PRINT_COMPATIBLE = false;

  public static final boolean PRINT_COMPATIBLE_WITH = false;
  // for debugging. To see if TypeOracle builds
  public static final boolean PROCESS_EXISTING_API = true;

  public static final boolean PROCESS_NEW_API = true;
  // true by default
  public static final boolean REMOVE_NON_SUBCLASSABLE_ABSTRACT_CLASS_FROM_API = true;

  // Tweak for log output.
  public static final TreeLogger.Type type = TreeLogger.ERROR;

  // remove duplicates by default
  public static Collection<ApiChange> getApiDiff(ApiContainer newApi,
      ApiContainer existingApi, Set<String> whiteList) throws NotFoundException {
    ApiDiffGenerator apiDiff = new ApiDiffGenerator(newApi, existingApi);
    return getApiDiff(apiDiff, whiteList, true);
  }

  // Call APIBuilders for each of the 2 source trees
  public static void main(String args[]) {

    try {
      ApiContainer newApi = null, existingApi = null;

      if (args.length < 1) {
        printHelp();
        System.exit(-1);
      }

      AbstractTreeLogger logger = new PrintWriterTreeLogger();
      logger.setMaxDetail(type);
      if (PROCESS_NEW_API) {
        newApi = new ApiContainer(args[0], "_new", logger);
        if (ApiCompatibilityChecker.DEBUG_PRINT_ALL_API) {
          logger.log(TreeLogger.INFO, newApi.getApiAsString()); // print the API
        }
      }
      if (PROCESS_EXISTING_API) {
        existingApi = new ApiContainer(args[0], "_old", logger);
        if (ApiCompatibilityChecker.DEBUG_PRINT_ALL_API) {
          logger.log(TreeLogger.INFO, existingApi.getApiAsString());
        }
      }

      if (PROCESS_NEW_API && PROCESS_EXISTING_API) {
        Collection<ApiChange> apiDifferences = getApiDiff(newApi, existingApi,
            readWhiteListFromFile(args[0]));
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
    } catch (Exception e) {
      // intercepting all exceptions in main, because I have to exit with -1 so
      // that the build breaks.
      e.printStackTrace();
      System.err.println("To view the help for this tool, execute this tool without any arguments");
      System.exit(-1);
    }
  }

  public static void printHelp() {
    StringBuffer sb = new StringBuffer();
    sb.append("java ApiCompatibilityChecker configFile\n");
    sb.append("The ApiCompatibilityChecker tool requires a config file as an argument. ");
    sb.append("The config file must specify two repositories of java source files: ");
    sb.append("'_old' and '_new', which are to be compared for API source compatibility.\n");
    sb.append("An optional whitelist is present at the end of ");
    sb.append("the config file. The format of the whitelist is same as the output of ");
    sb.append("the tool without the whitelist.\n");
    sb.append("Each repository is specified by the following four properties:\n");
    sb.append("name           specifies how the api should be refered to in the output\n");
    sb.append("dirRoot        optional argument that specifies the base directory of all other file/directory names\n");
    sb.append("sourceFiles    a colon-separated list of files/directories that specify the roots of the the filesystem trees to be included.\n");
    sb.append("excludeFiles   a colon-separated lists of files/directories that specify the roots of the filesystem trees to be excluded");

    sb.append("\n\n");
    sb.append("Example api.conf file:\n");
    sb.append("name_old         gwtEmulator");
    sb.append("\n");
    sb.append("dirRoot_old      ./");
    sb.append("\n");
    sb.append("sourceFiles_old  dev/core/super:user/super:user/src");
    sb.append("\n");
    sb.append("excludeFiles_old user/super/com/google/gwt/junit");
    sb.append("\n\n");

    sb.append("name_new         gwtEmulatorCopy");
    sb.append("\n");
    sb.append("dirRoot_new      ../gwt-14/");
    sb.append("\n");
    sb.append("sourceFiles_new  dev/core:user/super:user/src");
    sb.append("\n");
    sb.append("excludeFiles_new user/super/com/google/gwt/junit");
    sb.append("\n\n");

    System.out.println(sb.toString());
  }

  // interface for testing, since do not want to build ApiDiff frequently
  static Collection<ApiChange> getApiDiff(ApiDiffGenerator apiDiff,
      Set<String> whiteList, boolean removeDuplicates) throws NotFoundException {
    Collection<ApiChange> collection = apiDiff.getApiDiff(removeDuplicates);
    Set<ApiChange> prunedCollection = new HashSet<ApiChange>();
    for (ApiChange apiChange : collection) {
      String apiChangeAsString = apiChange.toString();
      apiChangeAsString = apiChangeAsString.trim();
      if (whiteList.remove(apiChangeAsString)) {
        continue;
      }
      // check for Status.Compatible and Status.Compatible_with
      if (!PRINT_COMPATIBLE
          && apiChange.getStatus().equals(ApiChange.Status.COMPATIBLE)) {
        continue;
      }
      if (!PRINT_COMPATIBLE_WITH
          && apiChange.getStatus().equals(ApiChange.Status.COMPATIBLE_WITH)) {
        continue;
      }
      prunedCollection.add(apiChange);
    }
    if (whiteList.size() > 0) {
      List<String> al = new ArrayList<String>(whiteList);
      Collections.sort(al);
      System.err.println("ApiChanges "
          + al
          + ",  not found. Are you using a properly formatted configuration file?");
    }
    List<ApiChange> apiChangeList = new ArrayList<ApiChange>(prunedCollection);
    Collections.sort(apiChangeList);
    return apiChangeList;
  }

  /**
   * Each whiteList element is an {@link ApiElement} and
   * {@link ApiChange.Status} separated by space. For example,
   * "java.util.ArrayList::size() MISSING". The {@code ApiElement} is
   * represented as the string obtained by invoking the getRelativeSignature()
   * method on {@link ApiElement}.
   * 
   * @param fileName
   * @return
   */
  private static Set<String> readWhiteListFromFile(String fileName)
      throws IOException {
    if (fileName == null) {
      throw new IllegalArgumentException("fileName is null");
    }
    Set<String> hashSet = new HashSet<String>();
    FileReader fr = new FileReader(fileName);
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