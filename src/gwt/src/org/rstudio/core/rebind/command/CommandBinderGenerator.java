/*
 * CommandBinderGenerator.java
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
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import org.rstudio.core.client.command.Handler;

import java.io.PrintWriter;

public class CommandBinderGenerator extends Generator
{
   private class Helper
   {
      public Helper(TreeLogger logger,
                    GeneratorContext context,
                    String typeName) throws Exception
      {
         logger_ = logger;
         context_ = context;
         binderType_ = context_.getTypeOracle().getType(typeName);
         packageName_ = binderType_.getPackage().getName();

         JClassType[] interfaces = binderType_.getImplementedInterfaces();
         doAssert(interfaces.length == 1,
                  binderType_.getQualifiedSourceName() + " extends >1 interfaces");
         JParameterizedType parentType = interfaces[0].isParameterized();
         doAssert(parentType != null, "No parent type");
         JClassType[] classTypes = parentType.getTypeArgs();
         doAssert(classTypes.length == 2, "Unexpected number of type args");
         commandsType_ = classTypes[0];
         handlersType_ = classTypes[1];
      }

      private void doAssert(boolean assertion, String message)
      {
         if (!assertion)
            throw new IllegalArgumentException(message);
      }

      public String generate() throws Exception
      {
         String simpleName = binderType_.getName().replace('.', '_') + "__Impl";

         PrintWriter printWriter = context_.tryCreate(
               logger_, packageName_, simpleName);
         if (printWriter != null)
         {
            ClassSourceFileComposerFactory factory =
                  new ClassSourceFileComposerFactory(packageName_, simpleName);
            factory.addImplementedInterface(binderType_.getName());
            factory.addImport("com.google.gwt.event.shared.HandlerRegistration");
            SourceWriter writer = factory.createSourceWriter(context_, printWriter);

            emitBind(writer);

            // Close the class and commit it
            writer.outdent();
            writer.println("}");
            context_.commit(logger_, printWriter);
         }
         return packageName_ + "." + simpleName;
      }

      private void emitBind(SourceWriter w)
      {
         w.print("public HandlerRegistration bind(final ");
         w.print(commandsType_.getQualifiedSourceName());
         w.print(" commands, final ");
         w.print(handlersType_.getQualifiedSourceName());
         w.println(" handlers) {");
         w.indent();

         w.print("final java.util.ArrayList<HandlerRegistration> regs = ");
         w.println("new java.util.ArrayList<HandlerRegistration>();");

         for (JMethod method : handlersType_.getMethods())
         {
            Handler h = method.getAnnotation(Handler.class);
            if (h == null)
               continue;

            if (!method.getReturnType().equals(JPrimitiveType.VOID))
               logger_.log(TreeLogger.Type.WARN,
                           "Handler method (" + handlersType_.getQualifiedSourceName()
                           + "." + method.getName() + ") has non-void return type "
                           + method.getReturnType().getQualifiedSourceName());

            if (method.getParameters().length != 0)
               throw new IllegalArgumentException(
                     "Handler methods must not have arguments ("
                     + method.getName() + ")");

            String methodName = method.getName();
            String commandName = h.value();
            if (commandName == null || commandName.length() == 0)
            {
               if (methodName.length() < 3
                   || !methodName.startsWith("on")
                   || Character.isLowerCase(methodName.charAt(2)))
               {
                  throw new IllegalArgumentException(
                        "Invalid handler method name " + methodName);
               }

               commandName = Character.toLowerCase(methodName.charAt(2))
                     + methodName.substring(3);
            }

            w.print("regs.add(commands." + commandName + "().addHandler(");
            w.println("new org.rstudio.core.client.command.CommandHandler() {");
            w.indent();
            w.println("public void onCommand(org.rstudio.core.client.command.AppCommand command) {");
            w.indent();
            w.println("handlers." + methodName + "();");
            w.outdent();
            w.println("}");
            w.outdent();
            w.println("}));");
         }

         w.println("return new HandlerRegistration() {");
         w.indent();
         w.println("public void removeHandler() {");
         w.indent();
         w.println("for (HandlerRegistration h : regs)");
         w.indentln("h.removeHandler();");
         w.outdent();
         w.println("}");
         w.outdent();
         w.println("};");

         w.outdent();
         w.println("}");
      }

      private final TreeLogger logger_;
      private final GeneratorContext context_;
      private final JClassType binderType_;
      private final String packageName_;
      private final JClassType commandsType_;
      private final JClassType handlersType_;
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
