/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.typeinfo.test;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.BadTypeArgsException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.ParseException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;
import com.google.gwt.dev.javac.TypeOracleMediator;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InteractiveTypeOracle {

  public static interface CommandHandler {
    /**
     * Specifies the keyword causes this handler to be invoked.
     */
    String getCommandToken();

    /**
     * Executes the command given the specified args, writing the results or an
     * error.
     * 
     * @return <code>false</code> if there was a problem, in which case the
     *         error description will have been written to results
     */
    boolean process(TreeLogger logger, String[] args);

    boolean requiresCurrentType();
  }

  public static void main(String[] args) throws UnableToCompleteException {
    AbstractTreeLogger logger = new PrintWriterTreeLogger();

    // See if we should create a gui logger.
    // TODO: this was removed to avoid making an SWT dependency from test code
    //
    // for (int i = 0; i < args.length; i++) {
    // if ("-gui".equals(args[i])) {
    // logger = TreeLoggerWidget.getAsDetachedWindow(
    // "Interactive Type Oracle Log", 700, 600, true);
    // break;
    // }
    // }

    String logLevel = System.getProperty("gwt.logLevel");
    if (logLevel != null) {
      logger.setMaxDetail(TreeLogger.Type.valueOf(logLevel));
    }

    InputStreamReader isr = new InputStreamReader(System.in);
    BufferedReader br = new BufferedReader(isr);

    // Build an oracle.
    //
    TypeOracleMediator mediator = new TypeOracleMediator();
    TypeOracle oracle = mediator.getTypeOracle();
    // TODO: add compilation units

    // Create an interactive wrapper around the oracle.
    //
    InteractiveTypeOracle ito = new InteractiveTypeOracle(oracle);

    try {
      String command;
      System.out.print("> ");
      System.out.flush();
      while (null != (command = br.readLine())) {
        ito.processCommand(logger, command);
        System.out.print("> ");
        System.out.flush();
      }
    } catch (IOException e) {
      System.err.println("Error reading stdin");
      e.printStackTrace();
    }
  }

  public InteractiveTypeOracle(TypeOracle oracle) {
    this.oracle = oracle;
    registerCommandHandler(cmdHelp);
    registerCommandHandler(cmdParse);
    registerCommandHandler(cmdSelectType);
    registerCommandHandler(cmdAllTypes);
    registerCommandHandler(cmdSubtypes);
    registerCommandHandler(cmdSupertypes);
    registerCommandHandler(cmdEnclosing);
    registerCommandHandler(cmdOverloads);
    registerCommandHandler(cmdNested);
    registerCommandHandler(cmdConstructors);
    registerCommandHandler(cmdMethods);
    registerCommandHandler(cmdFields);
  }

  public boolean processCommand(TreeLogger logger, String command) {
    String[] tokens = command.split("[ \t]");
    return processCommand(logger, tokens);
  }

  public boolean processCommand(TreeLogger logger, String[] tokens) {
    if (tokens.length == 0) {
      logger.log(TreeLogger.WARN, "Expecting a command", null);
      return false;
    }

    CommandHandler handler = handlers.get(tokens[0]);
    if (handler == null) {
      logger.log(TreeLogger.WARN, "Unknown command: " + tokens[0], null);
      cmdHelp.process(logger, new String[0]);
      return false;
    }

    if (currType == null && handler.requiresCurrentType()) {
      logger.log(TreeLogger.WARN,
          "This command requires a current type to be selected", null);
      return false;
    }

    String[] args = new String[tokens.length - 1];
    System.arraycopy(tokens, 1, args, 0, args.length);
    return handler.process(logger, args);
  }

  /**
   * Registers a command handler.
   * 
   * @return if not <code>null</code>, the previous handler for the same
   *         command token
   */
  public CommandHandler registerCommandHandler(CommandHandler handler) {
    String token = handler.getCommandToken();
    CommandHandler old = handlers.put(token, handler);
    return old;
  }

  private final CommandHandler cmdAllTypes = new CommandHandler() {

    public String getCommandToken() {
      return "all-types";
    }

    public boolean process(TreeLogger logger, String[] args) {
      if (args.length != 0) {
        logger.log(TreeLogger.WARN, "No arguments expected", null);
        logger.log(TreeLogger.INFO, "Usage: all-types", null);
        logger.log(TreeLogger.INFO, "Prints all known types", null);
        return false;
      }

      JClassType[] types = oracle.getTypes();
      oracle.sort(types);
      TreeLogger sublogger = null;
      for (int i = 0; i < types.length; i++) {
        if (sublogger == null) {
          sublogger = logger.branch(TreeLogger.INFO, "All known types", null);
        }
        JClassType type = types[i];
        String typename = type.getQualifiedSourceName();
        sublogger.log(TreeLogger.INFO, typename, null);
      }
      return true;
    }

    public boolean requiresCurrentType() {
      return false;
    }
  };

  private final CommandHandler cmdHelp = new CommandHandler() {

    public String getCommandToken() {
      return "help";
    }

    public boolean process(TreeLogger logger, String[] args) {
      TreeLogger sublogger = null;
      Set<String> keySet = handlers.keySet();
      String[] cmdTokens = keySet.toArray(new String[keySet.size()]);
      for (int i = 0; i < cmdTokens.length; i++) {
        String cmdToken = cmdTokens[i];
        if (sublogger == null) {
          sublogger = logger.branch(TreeLogger.INFO, "Available commands", null);
        }
        sublogger.log(TreeLogger.INFO, cmdToken, null);
      }
      return true;
    }

    public boolean requiresCurrentType() {
      return false;
    }
  };

  private final CommandHandler cmdSelectType = new CommandHandler() {

    public String getCommandToken() {
      return "select";
    }

    public boolean process(TreeLogger logger, String[] args) {
      if (args.length != 1) {
        logger.log(TreeLogger.WARN, "Expected a type name", null);
        logger.log(TreeLogger.INFO, "Usage: select <typename>", null);
        return false;
      }

      String typename = args[0];
      JClassType type = oracle.findType(typename);
      if (type == null) {
        logger.log(TreeLogger.WARN, "Cannot find type " + typename, null);
        return false;
      }

      currType = type;
      logger.log(TreeLogger.INFO, "Current type is now "
          + type.getQualifiedSourceName(), null);
      return true;
    }

    public boolean requiresCurrentType() {
      return false;
    }
  };

  private final CommandHandler cmdSubtypes = new CommandHandler() {

    public String getCommandToken() {
      return "subtypes";
    }

    public boolean process(TreeLogger logger, String[] args) {
      if (args.length != 0) {
        logger.log(TreeLogger.WARN, "No arguments expected", null);
        logger.log(TreeLogger.INFO, "Usage: subtypes", null);
        logger.log(TreeLogger.INFO, "Prints all subtypes of the current type",
            null);
        return false;
      }

      String typename = currType.getQualifiedSourceName();
      TreeLogger sublogger = null;

      JClassType[] subtypes = currType.getSubtypes();
      oracle.sort(subtypes);
      for (int i = 0; i < subtypes.length; i++) {
        if (sublogger == null) {
          sublogger = logger.branch(TreeLogger.INFO, "Subtypes of " + typename,
              null);
        }
        sublogger.log(TreeLogger.INFO, subtypes[i].getQualifiedSourceName(),
            null);
      }

      return true;
    }

    public boolean requiresCurrentType() {
      return true;
    }
  };

  private final CommandHandler cmdFields = new CommandHandler() {

    public String getCommandToken() {
      return "fields";
    }

    public boolean process(TreeLogger logger, String[] args) {
      if (args.length != 0) {
        logger.log(TreeLogger.WARN, "No arguments expected", null);
        logger.log(TreeLogger.INFO, "Usage: fields", null);
        logger.log(TreeLogger.INFO, "Prints the fields of the current type",
            null);
        return false;
      }

      String typename = currType.getQualifiedSourceName();
      TreeLogger sublogger = null;
      JField[] fields = currType.getFields();
      oracle.sort(fields);
      for (int i = 0; i < fields.length; i++) {
        if (sublogger == null) {
          sublogger = logger.branch(TreeLogger.INFO, "Fields of " + typename,
              null);
        }
        sublogger.log(TreeLogger.INFO, fields[i].toString(), null);
      }

      return true;
    }

    public boolean requiresCurrentType() {
      return true;
    }
  };

  private final CommandHandler cmdMethods = new CommandHandler() {

    public String getCommandToken() {
      return "methods";
    }

    public boolean process(TreeLogger logger, String[] args) {
      if (args.length != 0) {
        logger.log(TreeLogger.WARN, "No arguments expected", null);
        logger.log(TreeLogger.INFO, "Usage: methods", null);
        logger.log(TreeLogger.INFO, "Prints the methods of the current type",
            null);
        return false;
      }

      String typename = currType.getQualifiedSourceName();
      TreeLogger sublogger = null;
      JMethod[] methods = currType.getMethods();
      oracle.sort(methods);
      for (int i = 0; i < methods.length; i++) {
        if (sublogger == null) {
          sublogger = logger.branch(TreeLogger.INFO, "Methods of " + typename,
              null);
        }
        sublogger.log(TreeLogger.INFO, methods[i].toString(), null);
      }

      return true;
    }

    public boolean requiresCurrentType() {
      return true;
    }
  };

  private final CommandHandler cmdNested = new CommandHandler() {

    public String getCommandToken() {
      return "nested";
    }

    public boolean process(TreeLogger logger, String[] args) {
      if (args.length != 0) {
        logger.log(TreeLogger.WARN, "No arguments expected", null);
        logger.log(TreeLogger.INFO, "Usage: nested", null);
        logger.log(TreeLogger.INFO,
            "Prints the nested types of the current type", null);
        return false;
      }

      String typename = currType.getQualifiedSourceName();
      TreeLogger sublogger = null;
      JClassType[] nestedTypes = currType.getNestedTypes();
      oracle.sort(nestedTypes);
      for (int i = 0; i < nestedTypes.length; i++) {
        if (sublogger == null) {
          sublogger = logger.branch(TreeLogger.INFO, "Types nested inside "
              + typename, null);
        }
        sublogger.log(TreeLogger.INFO, nestedTypes[i].toString(), null);
      }

      return true;
    }

    public boolean requiresCurrentType() {
      return true;
    }
  };

  private final CommandHandler cmdParse = new CommandHandler() {

    public String getCommandToken() {
      return "parse";
    }

    public boolean process(TreeLogger logger, String[] args) {
      if (args.length == 0) {
        logger.log(TreeLogger.WARN, "At least one argument expected", null);
        logger.log(TreeLogger.INFO, "Usage: parse ...", null);
        logger.log(TreeLogger.INFO, "Parses a string into a type", null);
        return false;
      }

      String mushed = "";
      for (int i = 0; i < args.length; i++) {
        mushed += args[i];
      }

      try {
        JType type = oracle.parse(mushed);
        logger.log(TreeLogger.INFO, type.getQualifiedSourceName(), null);
      } catch (NotFoundException e) {
        logger.log(TreeLogger.WARN, "Could not find type", e);
      } catch (ParseException e) {
        logger.log(TreeLogger.WARN, "Unable to parse " + mushed, e);
      } catch (BadTypeArgsException e) {
        logger.log(TreeLogger.WARN, "Bad arguments " + mushed, e);
      } catch (TypeOracleException e) {
        logger.log(TreeLogger.WARN,
            "Some other type oracle exception while parsing " + mushed, e);
      }

      return true;
    }

    public boolean requiresCurrentType() {
      return false;
    }
  };

  private final CommandHandler cmdEnclosing = new CommandHandler() {

    public String getCommandToken() {
      return "enclosing";
    }

    public boolean process(TreeLogger logger, String[] args) {
      if (args.length != 0) {
        logger.log(TreeLogger.WARN, "No arguments", null);
        logger.log(TreeLogger.INFO, "Usage: enclosing", null);
        logger.log(TreeLogger.INFO,
            "Prints the enclosing type of the current type", null);
        return false;
      }

      JClassType enclosingType = currType.getEnclosingType();
      if (enclosingType != null) {
        logger.log(TreeLogger.INFO, enclosingType.getQualifiedSourceName(),
            null);
      }

      return true;
    }

    public boolean requiresCurrentType() {
      return true;
    }
  };

  private final CommandHandler cmdConstructors = new CommandHandler() {

    public String getCommandToken() {
      return "ctors";
    }

    public boolean process(TreeLogger logger, String[] args) {
      if (args.length != 0) {
        logger.log(TreeLogger.WARN, "No arguments", null);
        logger.log(TreeLogger.INFO, "Usage: ctors", null);
        logger.log(TreeLogger.INFO,
            "Prints the constructors of the current type", null);
        return false;
      }

      String typename = currType.getQualifiedSourceName();
      JConstructor[] ctors = currType.getConstructors();
      oracle.sort(ctors);
      TreeLogger sublogger = null;
      for (int i = 0; i < ctors.length; i++) {
        if (sublogger == null) {
          sublogger = logger.branch(TreeLogger.INFO, "Constructors of "
              + typename, null);
        }
        sublogger.log(TreeLogger.INFO, ctors[i].toString(), null);
      }

      return true;
    }

    public boolean requiresCurrentType() {
      return true;
    }
  };

  private final CommandHandler cmdOverloads = new CommandHandler() {

    public String getCommandToken() {
      return "overloads";
    }

    public boolean process(TreeLogger logger, String[] args) {
      if (args.length != 1) {
        logger.log(TreeLogger.WARN, "One argument is expected", null);
        logger.log(TreeLogger.INFO, "Usage: overloads <method-name>", null);
        logger.log(TreeLogger.INFO,
            "Prints the overloads of a particular method in the current type",
            null);
        return false;
      }

      String typename = currType.getQualifiedSourceName();
      JMethod[] overloads = currType.getOverloads(args[0]);
      oracle.sort(overloads);
      TreeLogger sublogger = null;
      for (int i = 0; i < overloads.length; i++) {
        if (sublogger == null) {
          sublogger = logger.branch(TreeLogger.INFO, "Overloads in " + typename
              + " of " + args[0], null);
        }
        sublogger.log(TreeLogger.INFO, overloads[i].toString(), null);
      }

      return true;
    }

    public boolean requiresCurrentType() {
      return true;
    }
  };

  private final CommandHandler cmdSupertypes = new CommandHandler() {
    public String getCommandToken() {
      return "supertypes";
    }

    public boolean process(TreeLogger logger, String[] args) {
      if (args.length != 0) {
        logger.log(TreeLogger.WARN, "No arguments", null);
        logger.log(TreeLogger.INFO, "Usage: supertypes", null);
        logger.log(TreeLogger.INFO,
            "Prints the hierarchy of supertypes of the current type", null);
        return false;
      }

      String typename = currType.getQualifiedSourceName();
      logger = logger.branch(TreeLogger.INFO, "Supertypes of " + typename, null);
      printSupertypesImpl(logger, currType);
      return true;
    }

    private void printSupertypesImpl(TreeLogger parentLogger, JClassType type) {
      TreeLogger childLogger;

      // Superclass
      JClassType superclass = type.getSuperclass();
      if (superclass != null) {
        String name = superclass.getQualifiedSourceName();
        childLogger = parentLogger.branch(TreeLogger.INFO, name, null);
        printSupertypesImpl(childLogger, superclass);
      }

      // Superinterfaces
      JClassType[] superintfs = type.getImplementedInterfaces();
      for (int i = 0; i < superintfs.length; i++) {
        JClassType superintf = superintfs[i];
        String name = superintf.getQualifiedSourceName();
        childLogger = parentLogger.branch(TreeLogger.INFO, name, null);
        printSupertypesImpl(childLogger, superintf);
      }
    }

    public boolean requiresCurrentType() {
      return true;
    }
  };

  private JClassType currType;
  private final Map<String, CommandHandler> handlers = new HashMap<String, CommandHandler>();
  private final TypeOracle oracle;
}
