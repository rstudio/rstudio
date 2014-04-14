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

import com.google.gwt.dev.About;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A base class for a GWT related command-line application. To use this:
 * <ol>
 * <li>Derive a class for this class.</li>
 * <li>In your constructor, call {@link #registerHandler(ArgHandler)}
 * repeatedly to register particular command line arguments and options.</li>
 * <li>Write a main that looks like this:
 *
 * <pre>
 * public static void main(String[] args) {
 *  MyShell myShell = new MyShell();
 *  if (myShell.processArgs(args)) {
 *    // main program operation
 *  }
 *  System.exit(1);
 * }
 * </pre>
 *
 * </li>
 * <li>Create launch config whose main class is MyShell.</li>
 * </ol>
 */
public abstract class ToolBase {

  private static final String PROPERTY_NOWARN_LEGACY_TOOLS = "gwt.nowarn.legacy.tools";

  public static void legacyWarn(Class<?> legacy, Class<?> replacement) {
    if (System.getProperty(PROPERTY_NOWARN_LEGACY_TOOLS) == null) {
      System.err.println("WARNING: '" + legacy.getName()
          + "' is deprecated and will be removed in a future release.");
      System.err.println("Use '" + replacement.getName() + "' instead.");
      System.err.println("(To disable this warning, pass -D"
          + PROPERTY_NOWARN_LEGACY_TOOLS + " as a JVM arg.)");
    }
  }

  /**
   * Use a linked hash map to preserve the declaration order.
   */
  private final Map<String, ArgHandler> argHandlers = new LinkedHashMap<String, ArgHandler>();

  /**
   * Log deprecation warning on old JREs.
   */
  protected void checkJdkVersion() {
    // JDK 1.6 is 50
    double classVersion = Double.valueOf(System.getProperty("java.class.version","0.0"));
    if (classVersion < 50) {
      System.err.println("WARNING: Use on a JRE prior to version 1.6 is deprecated");
    }
  }

  protected String getDescription() {
    return null;
  }

  protected String getName() {
    return getClass().getName();
  }

  protected void printHelp() {
    printHelp(false);
  }

  protected void printHelp(boolean includeUndocumented) {
    System.err.println(About.getGwtVersion());

    Set<ArgHandler> uniqueArgHandlers = new LinkedHashSet<ArgHandler>(argHandlers.values());

    ArgHandler nullHandler = null;
    int widest = 0;
    for (ArgHandler handler : uniqueArgHandlers) {
      if (handler.isUndocumented()) {
        continue;
      }
      String helpTag = handler.getHelpTag();
      if (helpTag != null) {
        if (helpTag.length() > widest) {
          widest = helpTag.length();
        }
      } else {
        nullHandler = handler;
        int len = nullHandler.getTagArgs()[0].length();
        if (len > widest) {
          widest = len;
        }
      }
    }

    // Print the name.
    //
    String name = getName();
    int i = name.lastIndexOf('.');
    if (i != -1) {
      name = name.substring(i + 1);
    }
    System.err.print(name);

    // Print the command-line template.
    //
    for (ArgHandler handler : uniqueArgHandlers) {
      if (!includeUndocumented && handler.isUndocumented()) {
        continue;
      }
      String helpTag = handler.getHelpTag();
      if (helpTag != null) {
        System.err.print(handler.isRequired() ? " " : " [");
        System.err.print(helpTag);
        String[] tagArgs = handler.getTagArgs();
        for (String tagArg : tagArgs) {
          System.err.print(" " + tagArg);
        }
        System.err.print(handler.isRequired() ? "" : "]");
      }
    }

    // Print the flagless args.
    //
    if (nullHandler != null && (!nullHandler.isUndocumented() || includeUndocumented)) {
      String[] tagArgs = nullHandler.getTagArgs();
      for (String element : tagArgs) {
        System.err.print(nullHandler.isRequired() ? " " : " [");
        System.err.print(element);
        System.err.print(nullHandler.isRequired() ? " " : "]");
      }
      System.err.println();
    }

    System.err.println();
    String description = getDescription();
    if (description != null) {
      System.err.println(description);
      System.err.println();
    }

    System.err.println("where ");

    // Print the details.
    //
    for (ArgHandler handler : uniqueArgHandlers) {
      if (!includeUndocumented && handler.isUndocumented()) {
        continue;
      }
      String helpTag = handler.getHelpTag();
      if (helpTag != null) {
        int len = helpTag.length();
        System.err.print("  ");
        System.err.print(helpTag);
        for (i = len; i < widest; ++i) {
          System.err.print(' ');
        }
        System.err.print("  ");
        System.err.print(handler.getPurpose());
        System.err.println();
      }
    }

    // And details for the "extra" args, if any.
    //
    if (nullHandler != null && (!nullHandler.isUndocumented() || includeUndocumented)) {
      System.err.println("and ");
      String tagArg = nullHandler.getTagArgs()[0];
      int len = tagArg.length();
      System.err.print("  ");
      System.err.print(tagArg);
      for (i = len; i < widest; ++i) {
        System.err.print(' ');
      }
      System.err.print("  ");
      System.err.print(nullHandler.getPurpose());
      System.err.println();
    }
  }

  protected boolean processArgs(String... args) {
    checkJdkVersion();

    if (args.length > 0) {
      boolean help = false;
      boolean helpUndocumented = false;
      if ("-help".equalsIgnoreCase(args[0])) {
        help = true;
      } else if ("-?".equals(args[0])) {
        help = true;
      } else if ("-helpfull".equalsIgnoreCase(args[0])) {
        help = helpUndocumented = true;
      } else if ("-??".equals(args[0])) {
        help = helpUndocumented = true;
      }

      if (help) {
        printHelp(helpUndocumented);
        return false;
      }
    }

    Set<ArgHandler> defs = new HashSet<ArgHandler>(argHandlers.values());
    int extraArgCount = 0;

    Set<ArgHandler> receivedArg = new HashSet<ArgHandler>();

    // Let the args drive the handlers.
    //
    ArgHandler nullHandler = argHandlers.get("");
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      ArgHandler handler;
      if (arg.startsWith("-")) {
        // Use the handler registered for this flag.
        //
        handler = argHandlers.get(arg);
      } else {
        // Use the handler that doesn't have a leading flag.
        //
        handler = nullHandler;
        ++extraArgCount;
      }

      if (handler == null) {
        System.err.println("Unknown argument: " + arg);
        printHelp();
        return false;
      }

      int addtlConsumed = handler.handle(args, i);
      if (addtlConsumed == -1) {
        printHelp();
        return false;
      }

      i += addtlConsumed;

      // We don't need to use this as a default handler.
      //
      defs.remove(handler);

      // Record that this handler saw a value
      //
      receivedArg.add(handler);
    }

    // See if any handler didn't get its required argument(s).
    //
    for (ArgHandler argHandler : argHandlers.values()) {
      if (argHandler.isRequired() && !receivedArg.contains(argHandler)) {
        System.err.print("Missing required argument '");
        String tag = argHandler.getTag();
        if (tag != null) {
          System.err.print(tag);
          System.err.print(" ");
        }

        String tagArg = argHandler.getTagArgs()[0];
        System.err.print(tagArg);
        System.err.println("'");

        printHelp();
        return false;
      }
    }
    if (extraArgCount == 0 && nullHandler != null && nullHandler.isRequired()) {
      System.err.print("Missing required argument '");
      String tagArg = nullHandler.getTagArgs()[0];
      System.err.print(tagArg);
      System.err.println("'");
      printHelp();
      return false;
    }

    // Set if there are any remaining unused handlers with default arguments.
    // Allow the default handlers to pretend there were other arguments.
    //
    for (ArgHandler def : defs) {
      String[] defArgs = def.getDefaultArgs();
      if (defArgs != null) {
        if (def.handle(defArgs, 0) == -1) {
          return false;
        }
      }
    }

    return true;
  }

  protected void registerHandler(ArgHandler handler) {
    for (String tag : handler.getTags()) {
      tag = tag != null ? tag : "";
      argHandlers.put(tag, handler);
    }
  }
}
