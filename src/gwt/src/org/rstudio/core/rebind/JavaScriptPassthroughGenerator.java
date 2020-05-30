/*
 * JavaScriptPassthroughGenerator.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.rebind;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import org.rstudio.core.client.js.BaseExpression;

import java.io.PrintWriter;

public class JavaScriptPassthroughGenerator extends Generator
{
   private class Helper
   {
      public Helper(TreeLogger logger,
                    GeneratorContext context,
                    String typeName) throws Exception
      {
         logger_ = logger;
         context_ = context;
         baseType_ = context_.getTypeOracle().getType(typeName);
         packageName_ = baseType_.getPackage().getName();

         BaseExpression be = baseType_.getAnnotation(BaseExpression.class);
         if (be == null)
         {
            logger_.log(Type.ERROR, "Missing @BaseExpression class annotation");
            throw new UnableToCompleteException();
         }

         baseExpression_ = be.value();

      }

      public String generate() throws Exception
      {
         String simpleName = baseType_.getName().replace('.', '_') + "__Impl";

         PrintWriter printWriter = context_.tryCreate(
               logger_, packageName_, simpleName);
         if (printWriter != null)
         {
            ClassSourceFileComposerFactory factory =
                  new ClassSourceFileComposerFactory(packageName_, simpleName);
            factory.addImplementedInterface(baseType_.getName());
            SourceWriter writer = factory.createSourceWriter(context_, printWriter);

            emitBody(writer);

            // Close the class and commit it
            writer.outdent();
            writer.println("}");
            context_.commit(logger_, printWriter);
         }
         return packageName_ + "." + simpleName;
      }

      private void emitBody(SourceWriter w) throws NotFoundException
      {
         for (JMethod method : baseType_.getMethods())
         {
            if (!method.isAbstract())
               continue;

            w.println();
            w.print("public native final ");
            w.print(method.getReturnType().getQualifiedSourceName());
            w.print(" ");
            w.print(method.getName());
            w.print("(");
            printParams(w, method, true);
            w.println(") /*-{");

            w.indent();
            if (!method.getReturnType().getQualifiedSourceName().equals("void"))
               w.print("return ");
            w.print(baseExpression_);
            w.print("." + method.getName() + "(");
            printParams(w, method, false);
            w.println(");");
            w.outdent();

            w.println("}-*/;");
         }
      }

      private void printParams(SourceWriter w,
                               JMethod method,
                               boolean withTypes)
      {
         JParameter[] parameters = method.getParameters();
         for (int i = 0; i < parameters.length; i++)
         {
            if (i > 0)
               w.print(", ");
            if (withTypes)
            {
               w.print(parameters[i].getType().getQualifiedSourceName());
               w.print(" ");
               w.print(parameters[i].getName());
            }
            else
            {
               String simpleName = parameters[i].getType().getSimpleSourceName();

               // wrap Command/CommandWithArg parameters with a plain JavaScript
               // function
               if (simpleName.equals("Command") || simpleName.equals("CommandWithArg"))
               {
                  w.print("function(result) { ");
                  w.print(parameters[i].getName() + ".@" + 
                          parameters[i].getType().getQualifiedSourceName() + 
                          "::execute");
                  if (simpleName.equals("Command"))
                  {
                     w.print("()()");
                  }
                  else if (simpleName.equals("CommandWithArg"))
                  {
                     w.print("(*)(result)");
                  }
                  w.print(";");
                  w.print("}");
               }
               else
               {
                  w.print(parameters[i].getName());
               }
            }
         }
      }

      private final TreeLogger logger_;
      private final GeneratorContext context_;
      private final String baseExpression_;
      private final JClassType baseType_;
      private final String packageName_;
   }

   @Override
   public String generate(TreeLogger logger,
                          GeneratorContext context,
                          String typeName) throws UnableToCompleteException
   {
      try
      {
         return new Helper(logger,
                           context,
                           typeName).generate();
      }
      catch (UnableToCompleteException e)
      {
         throw e;
      }
      catch (Exception e)
      {
         logger.log(TreeLogger.Type.ERROR, "Barf", e);
         throw new UnableToCompleteException();
      }

   }
}
