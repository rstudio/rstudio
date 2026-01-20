/*
 * DummyCommandsGenerator.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.commands;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * GWT generator that creates a dummy implementation of DummyCommands.
 * Generated implementations return null for all AppCommand methods and have
 * empty bodies for menu methods.
 */
public class DummyCommandsGenerator extends Generator
{
   @Override
   public String generate(TreeLogger logger,
                          GeneratorContext context,
                          String typeName) throws UnableToCompleteException
   {
      try
      {
         return new Helper(logger, context, typeName).generate();
      }
      catch (UnableToCompleteException e)
      {
         throw e;
      }
      catch (Exception e)
      {
         logger.log(TreeLogger.Type.ERROR, "Failed to generate DummyCommands", e);
         throw new UnableToCompleteException();
      }
   }

   private static class Helper
   {
      Helper(TreeLogger logger,
             GeneratorContext context,
             String typeName) throws Exception
      {
         logger_ = logger;
         context_ = context;
         bundleType_ = context_.getTypeOracle().getType(typeName);

         // Find the CommandBundle superclass (e.g., Commands)
         commandBundleType_ = findCommandBundleSuperclass(bundleType_);
         if (commandBundleType_ == null)
         {
            throw new Exception("DummyCommands must extend a CommandBundle subclass");
         }

         packageName_ = bundleType_.getPackage().getName();
      }

      /**
       * Find the CommandBundle subclass that this type extends.
       * For example, if the type is "DummyCommands extends Commands",
       * this returns the Commands type.
       */
      private JClassType findCommandBundleSuperclass(JClassType type)
      {
         JClassType commandBundleType = context_.getTypeOracle().findType(
               "org.rstudio.core.client.command.CommandBundle");

         if (commandBundleType == null)
            return null;

         JClassType superClass = type.getSuperclass();
         while (superClass != null)
         {
            if (superClass.isAssignableTo(commandBundleType) &&
                !superClass.getQualifiedSourceName().equals("org.rstudio.core.client.command.CommandBundle"))
            {
               return superClass;
            }
            superClass = superClass.getSuperclass();
         }

         return null;
      }

      /**
       * Generates the implementation class and returns its fully qualified name.
       */
      public String generate() throws Exception
      {
         String simpleName = bundleType_.getName().replace('.', '_') + "__Generated";

         PrintWriter printWriter = context_.tryCreate(
               logger_, packageName_, simpleName);

         if (printWriter != null)
         {
            ClassSourceFileComposerFactory factory =
                  new ClassSourceFileComposerFactory(packageName_, simpleName);
            factory.setSuperclass(commandBundleType_.getQualifiedSourceName());
            factory.addImport("org.rstudio.core.client.command.AppCommand");
            factory.addImport("org.rstudio.core.client.command.MenuCallback");
            SourceWriter writer = factory.createSourceWriter(context_, printWriter);

            emitStubMethods(writer);

            writer.outdent();
            writer.println("}");
            context_.commit(logger_, printWriter);
         }

         return packageName_ + "." + simpleName;
      }

      /**
       * Emit stub implementations for all abstract methods in the CommandBundle hierarchy.
       */
      private void emitStubMethods(SourceWriter writer)
      {
         ArrayList<JMethod> methods = getAbstractMethods(commandBundleType_);

         for (JMethod method : methods)
         {
            emitStubMethod(writer, method);
         }
      }

      /**
       * Get all abstract methods from a class and its superclasses.
       */
      private ArrayList<JMethod> getAbstractMethods(JClassType type)
      {
         ArrayList<JMethod> methods = new ArrayList<>();

         JClassType current = type;
         while (current != null)
         {
            for (JMethod method : current.getMethods())
            {
               if (method.isAbstract())
               {
                  methods.add(method);
               }
            }
            current = current.getSuperclass();
         }

         return methods;
      }

      /**
       * Emit a single stub method.
       */
      private void emitStubMethod(SourceWriter writer, JMethod method)
      {
         String returnType = method.getReturnType().getQualifiedSourceName();
         String methodName = method.getName();

         writer.println("@Override");
         writer.print("public " + returnType + " " + methodName + "(");

         // Emit parameters
         JParameter[] params = method.getParameters();
         for (int i = 0; i < params.length; i++)
         {
            if (i > 0)
               writer.print(", ");
            writer.print(params[i].getType().getQualifiedSourceName() + " " + params[i].getName());
         }
         writer.println(") {");

         writer.indent();

         // Emit body based on return type
         if (isAppCommandMethod(method))
         {
            writer.println("return null;");
         }
         // void methods have empty body

         writer.outdent();
         writer.println("}");
         writer.println();
      }

      private boolean isAppCommandMethod(JMethod method)
      {
         return method.getReturnType().getQualifiedSourceName().equals(
               "org.rstudio.core.client.command.AppCommand");
      }

      private final TreeLogger logger_;
      private final GeneratorContext context_;
      private final JClassType bundleType_;
      private final JClassType commandBundleType_;
      private final String packageName_;
   }
}
