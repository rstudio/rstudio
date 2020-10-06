/*
 * JavaScriptSerializerGenerator.java
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

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.core.client.js.JavaScriptSerializer;


import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

public class JavaScriptSerializerGenerator extends Generator
{    
    @Override
    public String generate (TreeLogger logger, GeneratorContext context, 
                            String typeName) throws UnableToCompleteException
    {
       TypeOracle oracle = context.getTypeOracle();
       List<JClassType> classes = new ArrayList<JClassType>();
       
       // locate all the types annotated with JavaScriptSerializable
       for (JClassType classType : oracle.getTypes())
       {
          if (isAnnotatedSerializable(classType))
             classes.add(classType);
       }
       
       ClassSourceFileComposerFactory sourceFile = 
             new ClassSourceFileComposerFactory(genPackageName, 
                                                genClassName );
       sourceFile.addImplementedInterface(
             JavaScriptSerializer.class.getCanonicalName());
       sourceFile.addImport("com.google.gwt.core.client.JavaScriptObject");
       sourceFile.addImport("org.rstudio.core.client.js.JsObject;");

       PrintWriter printWriter = context.tryCreate(logger, genPackageName, 
             genClassName);
       if (printWriter != null)
       {
           SourceWriter sourceWriter = sourceFile.createSourceWriter(context, 
                 printWriter);
           sourceWriter.println(genClassName + "() {");
           sourceWriter.println("}");

           printSerializers(classes, sourceWriter);
           printDeserializers(classes, sourceWriter);

           sourceWriter.commit(logger);
       }
       return sourceFile.getCreatedClassName();
    }

    private void printSerializers(List<JClassType> classes, SourceWriter w)
    {
       // print the method that dispatches to the appropriate serializer
       w.println("public <T> JavaScriptObject serialize(T source)");
       w.println("{");
       w.indent();
       for (JClassType classType : classes)
       {
          if (classType.isAbstract())
              continue;
          w.println();
          w.println("if (source.getClass().getName() == " +
                classType.getQualifiedSourceName() + ".class.getName())");
          w.println("{");
          w.indent();
          w.println("return serializeJso((" + 
             classType.getQualifiedSourceName() + ") source);");
          w.outdent();
          w.println("}");
          w.println();
       }
       w.println("return null;");
       w.outdent();
       w.println("}");
       // print individual serializers
       for (JClassType classType : classes)
       {
          w.print("private final native JavaScriptObject serializeJso(");
          w.println(classType.getQualifiedSourceName() + " source) /*-{");
          w.indent();
          w.println("return {");
          w.indent();
          w.println("\"class_name\":\"" +  
                    classType.getQualifiedSourceName() + "\",");
          w.println("\"class_data\": {");
          w.indent();
          JField[] fields = classType.getFields();
          for (int i = 0; i < fields.length; i++) 
          {
             JField field = fields[i];
             if (!field.isStatic())
             {
                w.print("\"" + field.getName() + "\": ");
                if (isAnnotatedSerializable(field))
                {
                   w.print("this.@" + genPackageName + "." + genClassName + 
                           "::serializeJso(L");
                   w.print(field.getType().getQualifiedSourceName()
                                          .replace(".", "/"));
                   w.print(";)(");
                }
                w.println("source.@" + classType.getQualifiedSourceName() +
                          "::" + field.getName());
                if (isAnnotatedSerializable(field))
                {
                   w.print(")");
                }
                if (i < (fields.length - 1))
                   w.print(", ");
                w.println();
             }
          }
          w.outdent();
          w.println("}");
          w.outdent();
          w.println("};");
          w.outdent();
          w.println("}-*/;");
          w.println();
       }
    }

    private void printDeserializers(List<JClassType> classes, 
          SourceWriter w)
    {
       w.println("private final native String classFromJso(" + 
                      "JavaScriptObject jso) /*-{");
       w.indent();
       w.println("return jso.class_name;");
       w.outdent();
       w.println("}-*/;");
       w.println();
       
       // print the method that dispatches to the appropriate deserializer
       w.println("public <T> T deserialize (JavaScriptObject jso)");
       w.println("{");
       w.indent();
       for (JClassType classType : classes)
       {
          // ignore abstract classes
          if (classType.isAbstract())
              continue;

          // determine class name from string
          w.println();
          w.println("if (classFromJso(jso) == \"" + 
                classType.getQualifiedSourceName() + "\")");
          w.println("{");
          w.indent();
          w.println(classType.getQualifiedSourceName()  + " ret = new " + 
                classType.getQualifiedSourceName() + "();");
          w.println("deserializeJso(ret, jso);");
          w.println("return (T) ret;");
          w.outdent();
          w.println("}");
          w.println();
       }
       w.println("return null;");
       w.outdent();
       w.println("}");
       
       // emit individual deserializer methods (overloads)
       for (JClassType classType : classes)
       {
          if (classType.isAbstract())
              continue;
          w.println();
          w.println("private final native void deserializeJso("  +
                    classType.getQualifiedSourceName() + " dest, " +
                    "JavaScriptObject source) /*-{");
          w.indent();
          for (JField field : classType.getFields())
          {
             if (!field.isStatic())
             {
                w.print("dest.@" + classType.getQualifiedSourceName() + "::");
                w.print(field.getName() + " = ");
                if (isAnnotatedSerializable(field))
                {
                   w.print("this.@" + genPackageName + "." + genClassName + 
                             "::deserialize(");
                   w.print("Lcom/google/gwt/core/client/JavaScriptObject;)(");
                }
                w.print("source.class_data[\"" + field.getName() + 
                           "\"]");
                if (isAnnotatedSerializable(field))
                   w.print(")");
                w.println(";");
             }
          }
          w.outdent();
          w.println("}-*/;");
       }
    }

    private boolean isAnnotatedSerializable(JField field)
    {
       JClassType classType = field.getType().isClass();
       if (classType == null)
          return false;
       return isAnnotatedSerializable(classType);
    }
    
    private boolean isAnnotatedSerializable(JClassType classType)
    {
       for (Annotation annotation: classType.getDeclaredAnnotations())
       {
          if (annotation.annotationType() == JavaScriptSerializable.class)
          {
             return true;
          }
       }
       return false;
    }

    private final String genPackageName = "org.rstudio.core.client.js";
    private final String genClassName = "JavaScriptSerializer__Impl";
}
