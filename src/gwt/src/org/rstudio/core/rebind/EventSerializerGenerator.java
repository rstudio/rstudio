/*
 * EventSerializerGenerator.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.*;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;

public class EventSerializerGenerator extends Generator
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

         JClassType[] interfaces = baseType_.getImplementedInterfaces();
         JParameterizedType parentType = interfaces[0].isParameterized();
         JClassType[] classTypes = parentType.getTypeArgs();
         serializedType_ = classTypes[0];
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
            factory.addImport("com.google.gwt.core.client.JavaScriptObject");
            SourceWriter writer = factory.createSourceWriter(context_, printWriter);

            emitBody(writer);

            writer.outdent();
            writer.println("}");
            context_.commit(logger_, printWriter);
         }
         return packageName_ + "." + simpleName;
      }

      private void emitBody(SourceWriter w) throws NotFoundException
      {
         w.print("public final native JavaScriptObject serializeToJSO(");
         w.println("Object source) /*-{");
         w.indent();
         w.println("return {");
         w.indent();
         JField[] fields = serializedType_.getFields();
         for (int i = 0; i < fields.length; i++) 
         {
            JField field = fields[i];
            if (!field.isStatic())
            {
               w.println("\"" + field.getName() + "\": " + 
                         "source.@" + serializedType_.getPackage().getName() + "." 
                         + serializedType_.getName() + "::" + field.getName() + 
                         (i < (fields.length - 1) ? ", " : ""));
            }
         }
         w.outdent();
         w.println("};");
         w.outdent();
         w.println("}-*/;");
         w.println();

         w.println("private final native void deserializeJSO(Object dest, " +
                   "JavaScriptObject source) /*-{");
         w.indent();
         for (JField field : serializedType_.getFields())
         {
            if (!field.isStatic())
            {
               w.println("dest.@" + serializedType_.getPackage().getName() + "." 
                         + serializedType_.getName() + "::" + field.getName() + 
                         " = " + "source[\"" + field.getName() + "\"];");
            }
         }
         w.outdent();
         w.println("}-*/;");

         w.println("public final " + serializedType_.getName() + 
                   " deserializeFromJSO(JavaScriptObject source)");
         w.println("{");
         w.indent();
         w.println(serializedType_.getName() + " n = new " + 
                   serializedType_.getName() + "();");
         w.println("deserializeJSO(n, source);");
         w.println("return n;");
         w.outdent();
         w.println("}");
         w.println();

      }

      private final TreeLogger logger_;
      private final GeneratorContext context_;
      private final JClassType baseType_;
      private final JClassType serializedType_;
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
