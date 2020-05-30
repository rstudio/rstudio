/*
 * AsyncShimGenerator.java
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
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.*;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.StringSourceWriter;

import java.io.PrintWriter;

public class AsyncShimGenerator extends Generator
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

      }

      private void doAssert(boolean assertion, String message)
      {
         if (!assertion)
            throw new IllegalArgumentException(message);
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
            factory.setSuperclass(baseType_.getName());
            factory.addImport("com.google.gwt.core.client.GWT");
            factory.addImport("com.google.gwt.core.client.RunAsyncCallback");
            factory.addImport("com.google.inject.Provider");
            factory.addImport("com.google.gwt.user.client.Command");
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
               "org.rstudio.core.client.AsyncShim");
         JClassType c = baseType_.asParameterizationOf(baseClass.isGenericType());
         JType delayedType = c.isParameterized().getTypeArgs()[0];

         w.println();
         w.println("private " + delayedType.getQualifiedSourceName() + " o;");
         w.println("private Provider<" + delayedType.getQualifiedSourceName() + "> po;");
         w.println();
         w.println("@Override");
         w.println("public void initialize(Provider<"
                   + delayedType.getQualifiedSourceName()
                   + "> provider) {");
         w.indentln("po = provider;");
         w.println("}");
         w.println();

         w.println("@Override");
         w.println("public void forceLoad(boolean downloadCodeOnly, Command continuation) {");
         w.indentln("load(downloadCodeOnly ? -1 : 0, new Object[] {continuation});");
         w.println("}");
         w.println();

         StringSourceWriter w_switch = new StringSourceWriter();

         int methodNum = 0;

         for (JMethod method : baseType_.getOverridableMethods())
         {
            if (!method.isAbstract())
               continue;

            doAssert(method.getReturnType().equals(JPrimitiveType.VOID),
                     "Async method had a non-void return type");

            w.print("public final void ");
            w.print(method.getName() + "(");
            String delim = "";
            for (JParameter param : method.getParameters())
            {
               w.print(delim);
               delim = ", ";
               w.print(param.getType().getQualifiedSourceName());
               w.print(" " + param.getName());
            }
            w.print(") ");
            if (method.getThrows().length > 0)
            {
               w.print("throws ");
               String delim2 = "";
               for (JType eType : method.getThrows())
               {
                  w.print(delim2);
                  delim2 = ", ";
                  w.print(eType.getQualifiedSourceName());
               }
            }
            w.println("{");
            w.indent();

            methodNum++;
            if (method.getParameters().length == 0)
            {
               w.println("load(" + methodNum + ", null);");
            }
            else
            {
               w.println("load(" + methodNum + ", new Object[] {");
               w.indent();
               String delim3 = "";
               for (JParameter p : method.getParameters())
               {
                  w.print(delim3);
                  delim3 = ", ";
                  w.print(p.getName());
               }
               w.outdent();
               w.println("});");
            }
            
            w.outdent();
            w.println("}");
            w.println();


            w_switch.println("case " + methodNum + ":");
            w_switch.indent();
            w_switch.print("o." + method.getName() + "(");
            String delim4 = "";
            for (int i = 0; i < method.getParameters().length; i++)
            {
               w_switch.print(delim4);
               delim4 = ", ";
               w_switch.print("(");
               w_switch.print(method.getParameters()[i].getType().getQualifiedSourceName());
               w_switch.print(")");
               w_switch.print("args[" + i + "]");
            }
            w_switch.println(");");
            w_switch.println("break;");
            w_switch.outdent();
         }

         w.println("private void load(final int method, final Object[] args) {");
         w.indent();
         w.println("GWT.runAsync(new RunAsyncCallback() {");
         w.indent();
         w.println("public void onFailure(Throwable reason) {");
         w.indent();
         w.println("try {");
         w.indentln("onDelayLoadFailure(reason);");
         w.println("} finally {");
         w.indentln("if (method <= 0 && args[0] != null) ((Command)args[0]).execute();");
         w.println("}");
         w.outdent();
         w.println("}");
         w.println("public void onSuccess() {");
         w.indent();
         w.println("preInstantiationHook(new Command() {");
         w.indent();
         w.println("public void execute() {");
         w.indent();
         w.println("onSuccess2();");
         w.outdent();
         w.println("}");
         w.outdent();
         w.println("});");
         w.outdent();
         w.println("}");
         w.println("private void onSuccess2() {");
         w.indent();
         w.println("try {");
         w.indent();
         w.println("if (method < 0) return; // download code only");
         w.println("if (o == null) {");
         w.indent();
         w.println("o = po.get();");
         w.println("onDelayLoadSuccess(o);");
         w.outdent();
         w.println("}");
         w.println("switch (method) {");
         w.println("case 0: break;");
         w.println(w_switch.toString());
         w.println("}");
         w.outdent();
         w.println("} finally {");
         w.indentln("if (method <= 0 && args[0] != null) ((Command)args[0]).execute();");
         w.println("}");
         w.outdent();
         w.println("}");
         w.outdent();
         w.println("});");
         w.outdent();
         w.println("}");
      }

      private final TreeLogger logger_;
      private final GeneratorContext context_;
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
