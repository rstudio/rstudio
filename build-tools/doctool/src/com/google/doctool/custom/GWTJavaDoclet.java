package com.google.doctool.custom;

import com.sun.javadoc.Doclet;
import com.sun.javadoc.RootDoc;
import com.sun.tools.doclets.standard.Standard;
import com.sun.tools.javadoc.Main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GWTJavaDoclet extends Doclet {

  static RootDoc root = null;

  private static final String[] TAGLET_ARGS = new String[]{
    "-taglet", ExampleTaglet.class.getName(), "-taglet",
    TipTaglet.class.getName(), "-taglet", IncludeTaglet.class.getName()};

  public static void main(String[] args) {
    List examplePackages = new ArrayList();
    List filteredArgs = new ArrayList();

    // filter out and save packages args
    for (int i = 0; i < args.length; ++i) {
      if (args[i].equalsIgnoreCase("-examplepackages")) {
        String nextArg = args[++i];
        String[] split = nextArg.split(":|;");
        for (int j = 0; j < split.length; ++j) {
          examplePackages.add(split[j]);
        }
      } else if (args[i].equalsIgnoreCase("-packages")) {
        String nextArg = args[++i];
        String[] split = nextArg.split(":|;");
        for (int j = 0; j < split.length; ++j) {
          filteredArgs.add(split[j]);
        }
      } else {
        filteredArgs.add(args[i]);
      }
    }

    // Build a javadoc structure that includes example packages for reference
    String name = GWTJavaDoclet.class.getName();
    List myArgs = new ArrayList();
    myArgs.addAll(filteredArgs);
    myArgs.addAll(examplePackages);
    Main.execute(name, name, (String[]) myArgs.toArray(new String[]{}));

    // Now delegate to the real javadoc without the example packages
    filteredArgs.addAll(0, Arrays.asList(TAGLET_ARGS));
    Main.execute((String[]) filteredArgs.toArray(new String[]{}));
  }

  public static int optionLength(String option) {
    // delegate
    return Standard.optionLength(option);
  }

  public static boolean start(RootDoc root) {
    // cache the root; ExampleTag will use it for reference later.
    GWTJavaDoclet.root = root;
    return true;
  }

}
