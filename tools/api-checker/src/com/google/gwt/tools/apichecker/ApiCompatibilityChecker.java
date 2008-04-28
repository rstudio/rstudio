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
import java.util.HashSet;

/**
 * Checks if the new API is compatible with the existing API.
 * 
 * 
 * To compute Api diffs, follow these 5 steps: i) for each of the two
 * repositories, construct an ApiContainer, ii) construct an ApiDiffGenerator,
 * iii) call computeApiDiff on the ApiDiffGenerator iv) call cleanApiDiff on the
 * ApiDiffGenerator v) call printApiDiff on the ApiDiffGenerator
 * 
 * An apicontainer object has a list of apiPackage objects. ApiPackage objects
 * themselves are list of ApiClass objects. ApiClass objects contain list of
 * ApiConstructor, ApiMethod, and JField objects.
 * 
 * Each ApiDiffGenerator object has a list of intersecting and missing
 * ApiPackageDiffGenerator objects. Each ApiPackageDiffGenerator object has a
 * list of intersecting and missing ApiClassDiffGenerator objects. Each
 * ApiClassDiffGenerator object has a list of intersecting and missing
 * apiMembers, where these members are constructors, methods, and fields.
 * 
 * For each intersecting apiMember, a list of ApiChange objects is stored. Each
 * ApiChange object encodes a specific Api change like adding the 'final'
 * keyword to an apiMethod.
 * 
 */
public class ApiCompatibilityChecker {

  public static final boolean API_SOURCE_COMPATIBILITY = true;
  public static final boolean REMOVE_ABSTRACT_CLASS_FROM_API = true;
  public static final boolean IGNORE_WHITELIST = false;
  public static final boolean PRINT_COMPATIBLE_WITH = false;
  public static final boolean PRINT_INTERSECTIONS = false;
  public static final boolean REMOVE_DUPLICATES = true;
  public static final boolean DEBUG = false;
  public static final boolean DISABLE_CHECKS = true;

  private static final AbstractTreeLogger logger1 = createTreeLogger();

  public static String getApiDiff(ApiDiffGenerator temp,
      HashSet<String> whiteList, boolean removeDuplicates)
      throws NotFoundException {
    temp.computeApiDiff();
    if (removeDuplicates) {
      temp.cleanApiDiff();
    }
    String apiDifferences = temp.printApiDiff();
    return removeWhiteListMatches(apiDifferences, whiteList);
  }

  // Call APIBuilders for each of the 2 source trees
  public static void main(String args[]) {

    try {
      ApiContainer newApi = null, existingApi = null;
      boolean processNewApi = true;
      boolean processExistingApi = true;

      if (args.length < 1) {
        printHelp();
        System.exit(-1);
      }

      if (processNewApi) {
        newApi = new ApiContainer(args[0], "_new", logger1);
      }
      if (processExistingApi) {
        existingApi = new ApiContainer(args[0], "_old", logger1);
      }

      if ((processNewApi && processExistingApi)
          && (newApi != null && existingApi != null)) {
        HashSet<String> whiteList = new HashSet<String>();
        if (!IGNORE_WHITELIST) {
          whiteList = readStringFromFile(args[0]);
        }

        ApiDiffGenerator apiDiff = new ApiDiffGenerator(newApi, existingApi);
        String apiDifferences = getApiDiff(apiDiff, whiteList,
            REMOVE_DUPLICATES);
        System.out.println(apiDifferences);
        System.out.println("\t\t\t\tApi Compatibility Checker tool, Copyright Google Inc. 2008");
        System.exit(apiDifferences.length() == 0 ? 0 : 1);
      }

    } catch (Exception e) {
      System.err.println("Exception " + e.getMessage()
          + ", printing stacktrace");
      e.printStackTrace();
      System.exit(-1);
    }
  }

  public static void printHelp() {
    System.out.println("java ApiCompatibilityChecker configFile\n");
    System.out.println("The ApiCompatibilityChecker tool requires a config file as an argument. "
        + "The config file must specify two repositories of java source files "
        + "that must be compared for API source compatibility. Each repository "
        + "must specify three properties: 'name', 'sourceFiles', and 'excludeFiles.' "
        + "A suffix of '_old' is attached to properties of the first repository, "
        + "while a suffix of '_new' is attached to properties of the second "
        + "repository. An optional whitelist can also be present at the end of "
        + "the config file. The format of the whitelist is same as the output of "
        + "the tool without the whitelist.");
    System.out.println();
    System.out.println("The name property specifies the api name that should "
        + "be used in the output. The sourceFiles property, a colon-separated "
        + "list of files/directories, specifies the roots of the the filesystem "
        + "trees that must be included. The excludeFiles property, "
        + "a colon-separated lists of files/directories specifies the roots of "
        + "the filesystem trees that must be excluded.");
    System.out.println();
    System.out.println();
    System.out.println("Example api.conf file:\n"

        + "name_old gwtEmulator\n"
        + "sourceFiles_old dev/core/super/com/google/gwt/dev/jjs/intrinsic/:user/super/com/google/gwt/emul/:user/src/com/google/gwt/core/client\n"
        + "excludeFiles_old \n\n"

        + "name_new gwtEmulatorCopy\n"
        + "sourceFiles_new dev/core/super/com/google/gwt/dev/jjs/intrinsic/:user/super/com/google/gwt/emul/:user/src/com/google/gwt/core/client\n"
        + "excludeFiles_new \n\n");
  }

  /**
   * Tweak this for the log output.
   */
  private static AbstractTreeLogger createTreeLogger() {
    AbstractTreeLogger logger = new PrintWriterTreeLogger();
    int choice = 3; // 1, 2, 3
    switch (choice) {
      case 1:
        logger.setMaxDetail(TreeLogger.ALL);
        break;
      case 2:
        logger.setMaxDetail(null);
        break;
      default:
        logger.setMaxDetail(TreeLogger.ERROR);
    }
    return logger;
  }

  private static HashSet<String> readStringFromFile(String fileName)
      throws IOException {
    if (fileName == null) {
      throw new IllegalArgumentException("fileName is null");
    }
    HashSet<String> hashSet = new HashSet<String>();
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
      if (splits.length > 1) {
        hashSet.add(splits[0] + " " + splits[1]);
      }
    }
    return hashSet;
  }

  private static String removeWhiteListMatches(String apiDifferences,
      HashSet<String> whiteList) {
    String apiDifferencesArray[] = apiDifferences.split("\n");
    String whiteListArray[] = whiteList.toArray(new String[0]);
    for (int i = 0; i < apiDifferencesArray.length; i++) {
      String temp = apiDifferencesArray[i].trim();
      for (String whiteListElement : whiteListArray) {
        if (temp.startsWith(whiteListElement)) {
          apiDifferencesArray[i] = "";
        }
      }
    }

    StringBuffer sb = new StringBuffer();
    for (String temp : apiDifferencesArray) {
      if (temp.length() > 0) {
        sb.append(temp);
        sb.append("\n");
      }
    }
    return sb.toString();
  }

}
