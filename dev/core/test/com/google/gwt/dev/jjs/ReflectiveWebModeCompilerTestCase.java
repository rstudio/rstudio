// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;
import com.google.gwt.dev.jdt.SourceOracle;
import com.google.gwt.dev.jdt.StaticCompilationUnitProvider;
import com.google.gwt.dev.jdt.WebModeCompilerFrontEnd;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ReflectiveWebModeCompilerTestCase extends TestCase {

  public ReflectiveWebModeCompilerTestCase() {
    // Build a map of packages and classes.
    // Each package P is represented by a nested class named
    // "Package_P", whose methods are declared as "src_T()",
    // where T is the name of a type within logical package P.
    // Calling src_T() produces the source for type T.
    // The default package class should be called "Package_".
    //
    ctorMapPackagesInTestCase(getClass());
  }

  public void compile(String appClass, Map rebinds)
      throws UnableToCompleteException {
    rebinds = rebinds != null ? rebinds : new HashMap();
    AbstractTreeLogger logger = new PrintWriterTreeLogger();
    logger.setMaxDetail(TreeLogger.INFO);
    RebindPermOracleImpl rpoi = new RebindPermOracleImpl(rebinds);
    SourceOracleReflectiveDecorator soi = new SourceOracleReflectiveDecorator();
    WebModeCompilerFrontEnd astCompiler = new WebModeCompilerFrontEnd(soi, rpoi);
    JavaToJavaScriptCompiler jjs = new JavaToJavaScriptCompiler(logger,
      astCompiler, new String[]{appClass});
    jjs.compile(logger, rpoi);
  }

  private class SourceOracleReflectiveDecorator implements SourceOracle {

    private final SourceOracle inner = new SourceOracleImpl();

    public CompilationUnitProvider findCompilationUnit(TreeLogger logger,
        String sourceTypeName) throws UnableToCompleteException {
      CompilationUnitProvider result = inner.findCompilationUnit(logger,
        sourceTypeName);
      if (result == null) {
        int lastDot = sourceTypeName.lastIndexOf('.');
        String packageName = "";
        String simpleName = sourceTypeName;
        if (lastDot != -1) {
          packageName = sourceTypeName.substring(0, lastDot);
          simpleName = sourceTypeName.substring(lastDot + 1);
        }

        String mangledPackageName = makeMangledPackageName(packageName);
        Map methodsByMangledClassName = (Map) methodMapByPackageName
          .get(mangledPackageName);

        if (methodsByMangledClassName != null) {
          String mangledSimpleName = makeMangledSimpleName(simpleName);
          Method method = (Method) methodsByMangledClassName
            .get(mangledSimpleName);
          if (method != null) {
            try {
              String source = (String) method.invoke(null, new Object[0]);
              result = new StaticCompilationUnitProvider(packageName,
                simpleName, source.toCharArray());
            } catch (IllegalArgumentException e) {
              e.printStackTrace();
              return null;
            } catch (IllegalAccessException e) {
              e.printStackTrace();
              return null;
            } catch (InvocationTargetException e) {
              e.printStackTrace();
              return null;
            }
          }
        }
      }
      return result;
    }

    public boolean isPackage(String possiblePackageName) {
      boolean result = inner.isPackage(possiblePackageName);
      if (!result) {
        String mangledName = makeMangledPackageName(possiblePackageName);

        // Any matching prefix qualifies.
        //
        for (Iterator iter = methodMapByPackageName.keySet().iterator(); iter
          .hasNext();) {
          String testMangledPackageName = (String) iter.next();
          if (testMangledPackageName.startsWith(mangledName))
            return true;
        }
      }
      return result;
    }

  }

  private void ctorMapMethodsInPackage(Class packageClass,
      Map/* <String, Method> */methodByName) {
    // We build the map from the top of the hierachy downward, so that map
    // inserts in derived classes will override those in superclasses.
    //
    Class superClass = packageClass.getSuperclass();
    if (superClass != null)
      ctorMapMethodsInPackage(superClass, methodByName);

    // Now contribute to the map on behalf of packageClass.
    //
    Method[] methods = packageClass.getDeclaredMethods();
    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      String methodName = method.getName();
      if (methodName.startsWith("src_")) {
        if (method.getParameterTypes().length == 0) {
          int modifiers = method.getModifiers();
          if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) {
            // This counts as a source-producing method.
            //
            method.setAccessible(true);
            methodByName.put(methodName, method);
          } else {
            System.err.println("Source method " + methodName + " in class "
              + packageClass.getName() + " should be 'public' and 'static'");
          }
        }
      }
    }
  }

  private void ctorMapPackagesInTestCase(Class testCaseClass) {
    // We build the map from the top of the hierachy downward, so that map
    // inserts in derived classes will override those in superclasses.
    //
    if (!testCaseClass.equals(ReflectiveWebModeCompilerTestCase.class)) {
      Class superClass = testCaseClass.getSuperclass();
      if (superClass != null)
        ctorMapPackagesInTestCase(superClass);
    }

    // Now contribute to the map on behalf of containingClass.
    //
    Class[] nestedClasses = testCaseClass.getDeclaredClasses();

    for (int i = 0; i < nestedClasses.length; i++) {
      Class nestedClass = nestedClasses[i];
      String nestedClassName = nestedClass.getName();
      int indexOfSimpleName = nestedClassName.lastIndexOf('$') + 1;
      nestedClassName = nestedClassName.substring(indexOfSimpleName);
      if (nestedClassName.startsWith("Package_")) {
        int modifiers = nestedClass.getModifiers();
        if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) {
          // This counts as a package.
          //
          Map/* <String, Method> */methodMap = (Map) methodMapByPackageName
            .get(nestedClassName);
          if (methodMap == null) {
            methodMap = new HashMap();
            methodMapByPackageName.put(nestedClassName, methodMap);
          }

          // Add source-producing methods.
          //
          ctorMapMethodsInPackage(nestedClass, methodMap);
        } else {
          System.err.println("Package class " + nestedClassName + " in class "
            + testCaseClass.getName() + " should be 'public' and 'static'");
        }
      }
    }
  }

  private String makeMangledPackageName(String packageName) {
    packageName = "Package_" + packageName.replace('.', '_');
    return packageName;
  }

  private String makeMangledSimpleName(String typeName) {
    return "src_" + typeName;
  }

  private final Map/* <String, Map<String, Method>> */methodMapByPackageName = new HashMap();

}
