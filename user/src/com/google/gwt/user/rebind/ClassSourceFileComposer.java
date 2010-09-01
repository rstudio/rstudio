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
package com.google.gwt.user.rebind;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory.JavaSourceCategory;

import java.io.PrintWriter;

class ClassSourceFileComposer implements SourceWriter {

  /**
   * For the interior of a '*' style comment.
   */
  private static final String STAR_COMMENT_LINE = " * ";

  private boolean atStart;

  /**
   * Either STAR/BLOCK comment line, not pulled out into a ENUM class because
   * only used by this class.
   */
  private String commentIndicator;

  private final GeneratorContext ctx;

  /**
   * Are you currently in a comment?
   */
  private boolean inComment;

  private int indent;

  private final PrintWriter printWriter;

  ClassSourceFileComposer(GeneratorContext ctx, PrintWriter printWriter,
      String targetPackageName, String[] annotationDeclarations,
      String targetClassShortName, String superClassName,
      String[] interfaceNames, String[] imports, JavaSourceCategory category,
      String classJavaDocComment) {
    this.ctx = ctx;
    this.printWriter = printWriter;
    if (targetPackageName == null) {
      throw new IllegalArgumentException("Cannot supply a null package name to"
          + targetClassShortName);
    }
    // TODO: support a user-specified file header
    if (targetPackageName.length() > 0) {
      println("package " + targetPackageName + ";");
    }

    if (imports != null && imports.length > 0) {
      println();
      for (int i = 0, n = imports.length; i < n; ++i) {
        println("import " + imports[i] + ";");
      }
    }
    if (classJavaDocComment != null) {
      beginJavaDocComment();
      print(classJavaDocComment);
      endJavaDocComment();
    } else {
      // beginJavaDocComment adds its own leading newline, make up for it here.
      println();
    }
    for (String annotation : annotationDeclarations) {
      println(annotation);
    }
    if (category == JavaSourceCategory.CLASS) {
      emitClassDecl(targetClassShortName, superClassName, interfaceNames);
    } else {
      emitInterfaceDecl(targetClassShortName, superClassName, interfaceNames);
    }
    println(" {");
    indent();
  }

  /**
   * Begin emitting a JavaDoc comment.
   */
  public void beginJavaDocComment() {
    println("\n/**");
    inComment = true;
    commentIndicator = STAR_COMMENT_LINE;
  }

  public void commit(TreeLogger logger) {
    outdent();
    println("}");
    printWriter.close();
    // If generating a class on the command line, may not have a
    if (ctx != null) {
      ctx.commit(logger, printWriter);
    }
  }

  /**
   * End emitting a JavaDoc comment.
   */
  public void endJavaDocComment() {
    inComment = false;
    println("\n */");
  }

  public void indent() {
    ++indent;
  }

  public void indentln(String s) {
    indent();
    println(s);
    outdent();
  }

  public void indentln(String s, Object... args) {
    indentln(String.format(s, args));
  }

  public void outdent() {
    --indent;
  }

  public void print(String s) {
    // If we just printed a newline, print an indent.
    //
    if (atStart) {
      for (int j = 0; j < indent; ++j) {
        printWriter.print("  ");
      }
      if (inComment) {
        printWriter.print(commentIndicator);
      }
      atStart = false;
    }
    // Now print up to the end of the string or the next newline.
    //
    String rest = null;
    int i = s.indexOf("\n");
    if (i > -1 && i < s.length() - 1) {
      rest = s.substring(i + 1);
      s = s.substring(0, i + 1);
    }
    printWriter.print(s);
    // If rest is non-null, then s ended with a newline and we recurse.
    //
    if (rest != null) {
      atStart = true;
      print(rest);
    }
  }

  public void print(String s, Object... args) {
    print(String.format(s, args));
  }

  public void println() {
    print("\n");
    atStart = true;
  }

  public void println(String s) {
    print(s + "\n");
    atStart = true;
  }

  public void println(String s, Object... args) {
    println(String.format(s, args));
  }

  private void emitClassDecl(String targetClassShortName,
      String superClassName, String[] interfaceNames) {
    print("public class " + targetClassShortName);
    if (superClassName != null) {
      print(" extends " + superClassName);
    }
    if (interfaceNames != null && interfaceNames.length > 0) {
      print(" implements ");
      for (int i = 0, n = interfaceNames.length; i < n; ++i) {
        if (i > 0) {
          print(", ");
        }
        print(interfaceNames[i]);
      }
    }
  }

  private void emitInterfaceDecl(String targetClassShortName,
      String superClassName, String[] interfaceNames) {
    if (superClassName != null) {
      throw new IllegalArgumentException("Cannot set superclass name "
          + superClassName + " on a interface.");
    }
    print("public interface " + targetClassShortName);
    if (interfaceNames != null && interfaceNames.length > 0) {
      print(" extends ");
      for (int i = 0; i < interfaceNames.length; ++i) {
        if (i > 0) {
          print(", ");
        }
        print(interfaceNames[i]);
      }
    }
  }
}
