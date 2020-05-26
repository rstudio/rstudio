/*
 * JsObjectInjectorGenerator.java
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
package org.rstudio.core.rebind.command;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.*;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import org.rstudio.core.client.js.BaseExpression;

import java.io.PrintWriter;

public class JsObjectInjectorGenerator extends Generator
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
         JClassType baseClass = context_.getTypeOracle().getType(
               "org.rstudio.core.client.js.JsObjectInjector");
         JClassType c = baseType_.asParameterizationOf(baseClass.isGenericType());
         JType typeToInject = c.isParameterized().getTypeArgs()[0];

         w.print("public native final void injectObject(");
         w.print(typeToInject.getQualifiedSourceName());
         w.println(" value) /*-{");
         w.indent();

         w.println(baseExpression_ + " = {");
         w.indent();

         JMethod[] methods = typeToInject.isClassOrInterface().getMethods();
         for (int i = 0; i < methods.length; i++)
         {
            JMethod method = methods[i];
            final JParameter[] jParameters = method.getParameters();

            StringBuilder argString = new StringBuilder();
            for (int j = 0; j < jParameters.length; j++)
            {
               argString.append("_").append(j);
               if (j < jParameters.length - 1)
                  argString.append(", ");
            }

            w.println(method.getName() + ": function(" + argString + ") {");
            w.indent();

            if (!method.getReturnType().getQualifiedSourceName().equals("void"))
               w.print("return ");
            w.print("value.@");
            w.print(typeToInject.getQualifiedSourceName());
            w.print("::");
            w.print(method.getName());
            w.print("(");
            for (JParameter param : jParameters)
               w.print(param.getType().getJNISignature());
            w.print(")(");
            w.print(argString.toString());
            w.println(");");

            w.outdent();
            w.print("}");
            w.println((i < methods.length - 1) ? "," : "");
         }

         w.outdent();
         w.println("};");

         w.outdent();
         w.println("}-*/;");

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
