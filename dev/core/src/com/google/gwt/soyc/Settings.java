/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.soyc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Command-line settings for Compile Reports (SOYC).
 */
public class Settings {
  /**
   * An exception indicating that there is a problem in an argument list.
   */
  public static class ArgumentListException extends Exception {
    public ArgumentListException(String message) {
      super(message);
    }
  }

  /**
   * One individual setting.
   */
  public abstract static class Setting<T> {
    private final String help;
    private T value;

    public Setting(T initialValue, String help) {
      value = initialValue;
      this.help = help;
    }

    public T get() {
      return value;
    }

    public String getHelp() {
      return help;
    }

    public void set(T newValue) {
      value = newValue;
    }

    /**
     * Consume arguments from the front of the list. If the front of the
     * argument list is not a match, do nothing. If the front of the argument
     * list is a match but has some problem, then throw an exception.
     */
    abstract boolean consumeArguments(List<String> arguments)
        throws ArgumentListException;
  }

  /**
   * A setting that is an option followed by a string argument.
   */
  public static class StringSetting extends Setting<String> {
    private final String option;

    public StringSetting(String option, String argumentName,
        String defaultSetting, String description) {
      super(defaultSetting, option + " " + argumentName + "    " + description);
      this.option = option;
    }

    @Override
    public String toString() {
      return option + " " + get();
    }

    @Override
    boolean consumeArguments(List<String> arguments)
        throws ArgumentListException {
      if (arguments.get(0).equals(option)) {
        if (arguments.size() < 2) {
          throw new ArgumentListException("Option " + option
              + " requires an argument");
        }
        arguments.remove(0);
        set(arguments.remove(0));
        return true;
      }

      return false;
    }
  }

  public static Settings fromArgumentList(String[] allArguments)
      throws ArgumentListException {
    Settings settings = new Settings();

    List<String> remainingArguments = new LinkedList<String>(
        Arrays.asList(allArguments));

    // Handle hyphenated options
    next_argument : while (!remainingArguments.isEmpty()) {
      for (Setting<?> setting : settings.allSettings) {
        if (setting.consumeArguments(remainingArguments)) {
          continue next_argument;
        }
      }
      break; // No setting wanted the remaining arguments
    }

    if ((settings.soycDir.get() == null)
        && (settings.symbolMapsDir.get() == null)) {

      // If in legacy command line mode, handle bare arguments at the end of the
      // list
      if (remainingArguments.isEmpty()) {
        throw new ArgumentListException("Must specify the soyc directory");
      }

      if (remainingArguments.get(0).startsWith("-")) {
        throw new ArgumentListException("Unrecognized argument: "
            + remainingArguments.get(0));
      }

      settings.storiesFileName = remainingArguments.remove(0);

      if (!remainingArguments.isEmpty()) {
        settings.depFileName = remainingArguments.remove(0);
      }
      if (!remainingArguments.isEmpty()) {
        settings.splitPointsFileName = remainingArguments.remove(0);
      }

      if (!remainingArguments.isEmpty()) {
        throw new ArgumentListException("Too many arguments");
      }

    } else if (settings.soycDir.get() != null) {
      if (settings.symbolMapsDir.get() == null) {
        throw new ArgumentListException(
            "Must specify symbol maps directory when specifying Soyc directory.");
      }
    } else {
      if (settings.soycDir.get() == null) {
        throw new ArgumentListException(
            "Must specify Soyc directory when specifying symbol maps directory .");
      }
    }

    // if the output directory doesn't exist, create it before going on
    if (settings.out.get() != null) {
      File dir = new File(settings.out.get());
      if (!dir.exists()) {
        dir.mkdir();
      }
    }
    return settings;
  }

  public static String settingsHelp() {
    StringBuffer help = new StringBuffer();
    for (Setting<?> setting : new Settings().allSettings) {
      help.append(setting.getHelp() + "\n");
    }
    return help.toString();
  }

  public String depFileName;

  public final Setting<String> out = addSetting(new StringSetting("-out",
      "dir", ".", "output directory"));

  public final Setting<String> resources = addSetting(new StringSetting(
      "-resources",
      "dir",
      null,
      "present only for backwards compatibility; directory or jar file with CSS, etc., resources"));

  public final Setting<String> soycDir = addSetting(new StringSetting(
      "-soycDir", "dir", null, " directory for soyc files"));

  public String splitPointsFileName;

  public String storiesFileName;

  public final Setting<String> symbolMapsDir = addSetting(new StringSetting(
      "-symbolMapsDir", "dir", null, " directory or symbol maps files"));

  private List<Setting<?>> allSettings;

  private <T> Setting<T> addSetting(Setting<T> setting) {
    if (allSettings == null) {
      allSettings = new ArrayList<Setting<?>>();
    }
    allSettings.add(setting);
    return setting;
  }

}
