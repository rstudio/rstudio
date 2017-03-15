/*
 * CommandBundleGenerator.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This generator runs at compile time (like all GWT Generators) and creates
 * implementations for any subinterfaces of CommandBundle that are found in
 * client code.
 */
public class CommandBundleGenerator extends Generator
{
   @Override
   public String generate(TreeLogger logger,
                          GeneratorContext context,
                          String typeName) throws UnableToCompleteException
   {
      try
      {
         return new CommandBundleGeneratorHelper(logger,
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

/**
 * The actual logic for type generation is moved into this separate class
 * so we can access a lot of the common info as fields instead of passing
 * them around.
 */
class CommandBundleGeneratorHelper
{
   CommandBundleGeneratorHelper(TreeLogger logger,
                                GeneratorContext context,
                                String typeName) throws Exception
   {
      logger_ = logger;
      context_ = context;
      bundleType_ = context_.getTypeOracle().getType(typeName);
      commandMethods_ = getMethods(true, false, false);
      menuMethods_ = getMethods(false, true, false);
      shortcutsMethods_ = getMethods(false, false, true);
      packageName_ = bundleType_.getPackage().getName();
   }

   /**
    * Generates the impl class and returns its name.
    */
   public String generate() throws Exception
   {
      ImageResourceInfo images = generateImageBundle();

      simpleName_ = bundleType_.getName().replace('.', '_') + "__Impl";

      PrintWriter printWriter = context_.tryCreate(
            logger_, packageName_, simpleName_);
      if (printWriter != null)
      {
         // I don't fully understand why images is sometimes null but we better
         // not get into a situation where we are generating this type but don't
         // know what images we can use. Empirically it seems like images is
         // always null only when printWriter is also null.
         assert images != null;

         ClassSourceFileComposerFactory factory =
               new ClassSourceFileComposerFactory(packageName_, simpleName_);
         factory.setSuperclass(bundleType_.getName());
         factory.addImport("org.rstudio.core.client.command.AppCommand");
         factory.addImport("org.rstudio.core.client.command.MenuCallback");
         factory.addImport("org.rstudio.core.client.command.ShortcutManager");
         factory.addImport("org.rstudio.core.client.resources.ImageResource2x");
         SourceWriter writer = factory.createSourceWriter(context_, printWriter);

         emitConstructor(writer, images);
         emitCommandFields(writer);
         emitMenus(writer);
         emitShortcuts(writer);
         emitCommandAccessors(writer);

         // Close the class and commit it
         writer.outdent();
         writer.println("}");
         context_.commit(logger_, printWriter);
      }
      return packageName_ + "." + simpleName_;
   }

   private void emitConstructor(SourceWriter writer, ImageResourceInfo images)
         throws UnableToCompleteException
   {
      writer.println("public " + simpleName_ + "() {");

      // Get additional properties from XML resource file, if exists
      Map<String, Element> props = getCommandProperties();
      // Implement the methods for the commands
      for (JMethod method : commandMethods_)
         emitCommandInitializers(writer, props, method, images);

      writer.println();
      writer.indentln("__registerShortcuts();");

      writer.println("}");
   }

   private void emitCommandFields(SourceWriter writer)
                             throws UnableToCompleteException
   {
      // Declare the fields for the commands
      for (JMethod method : commandMethods_)
      {
         String name = method.getName();
         writer.println("private AppCommand " + name + "_;");
      }
   }

   private void emitMenus(SourceWriter writer) throws UnableToCompleteException
   {
      for (JMethod method : menuMethods_)
      {
         String name = method.getName();
         NodeList nodes = getConfigDoc("/commands/menu[@id='" + name + "']");
         if (nodes.getLength() == 0)
         {
            logger_.log(TreeLogger.Type.ERROR,
                        "Unable to find config info for menu " + name);
            throw new UnableToCompleteException();
         }
         else if (nodes.getLength() > 1)
         {
            logger_.log(TreeLogger.Type.ERROR,
                        "Duplicate menu entries for menu " + name);
         }

         String menuClass = new MenuEmitter(logger_,
                                            context_,
                                            bundleType_,
                                            (Element) nodes.item(0)).generate();

         writer.println("public void " + name + "(MenuCallback callback) {");
         writer.indentln("new " + menuClass +
                         "(this).createMenu(callback);");
         writer.println("}");
      }
   }

   private void emitShortcuts(SourceWriter writer) throws UnableToCompleteException
   {
      writer.println("private void __registerShortcuts() {");
      writer.indent();
      NodeList nodes = getConfigDoc("/commands/shortcuts");
      for (int i = 0; i < nodes.getLength(); i++)
      {
         NodeList groups = nodes.item(i).getChildNodes();
         for (int j = 0; j < groups.getLength(); j++)
         {
            if (groups.item(j).getNodeType() != Node.ELEMENT_NODE)
               continue;
            String groupName = ((Element) groups.item(j)).getAttribute("name");
            new ShortcutsEmitter(logger_, groupName,
                                 (Element) groups.item(j)).generate(writer);
         }
      }
      writer.outdent();
      writer.println("}");
   }

   private JMethod[] getMethods(boolean includeCommands,
                                boolean includeMenus,
                                boolean includeShortcuts)
         throws UnableToCompleteException
   {
      ArrayList<JMethod> methods = new ArrayList<JMethod>();
      for (JMethod method : bundleType_.getMethods())
      {
         if (!method.isAbstract())
            continue;
         validateMethod(method);
         if (!includeCommands && isCommandMethod(method))
            continue;
         if (!includeMenus && isMenuMethod(method))
            continue;
         if (!includeShortcuts && isShortcutsMethod(method))
            continue;
         methods.add(method);
      }
      return methods.toArray(new JMethod[methods.size()]);
   }

   // Log and throw if anything is awry about the declaration
   private void validateMethod(JMethod method) throws UnableToCompleteException
   {
      if (isMenuMethod(method))
      {
         if (method.getParameters().length != 1)
         {
            logger_.log(TreeLogger.Type.ERROR,
                        "Method " + method +
                        " had the wrong number of parameters (expected 1)");
            throw new UnableToCompleteException();
         }

         String paramType =
               method.getParameters()[0].getType().getQualifiedSourceName();
         if (!paramType.equals("org.rstudio.core.client.command.MenuCallback"))
         {
            logger_.log(TreeLogger.Type.ERROR,
                        "Method " + method +
                        " had wrong parameter type (expected " +
                        "org.rstudio.core.client.command.MenuCallback)");
            throw new UnableToCompleteException();
         }
      }
      else
      {
         if (method.getParameters().length != 0)
         {
            logger_.log(TreeLogger.Type.ERROR,
                        "Method " + method +
                        " had parameters where none were expected");
            throw new UnableToCompleteException();
         }
      }

      if (!isCommandMethod(method)
          && !isMenuMethod(method)
          && !isShortcutsMethod(method))
      {
         logger_.log(TreeLogger.Type.ERROR,
                     "Method " + method +
                     " had an unexpected return type");
         throw new UnableToCompleteException();
      }
   }

   private boolean isCommandMethod(JMethod method)
   {
      return method.getReturnType().getQualifiedSourceName().equals(
            "org.rstudio.core.client.command.AppCommand");
   }

   private boolean isMenuMethod(JMethod method)
   {
      String sourceName = method.getReturnType().getQualifiedSourceName();
      return sourceName.equals("void");
   }

   private boolean isShortcutsMethod(JMethod method)
   {
      String sourceName = method.getReturnType().getQualifiedSourceName();
      return sourceName.equals("org.rstudio.core.client.command.ShortcutManager");
   }

   /**
    * Emit the getter method for the command--it is implemented as a cached
    * lazy-load. e.g.:
    *
    * public AppCommand newSourceDoc() {
    *   if (newSourceDoc_ == null) {
    *     newSourceDoc_ = new AppCommand();
    *     // call various setters...
    *   }
    *   return newSourceDoc_;
    * }
    */
   private void emitCommandInitializers(SourceWriter writer,
                                  Map<String, Element> props,
                                  JMethod method,
                                  ImageResourceInfo images)
   {
      String name = method.getName();
      writer.println(name + "_ = new AppCommand();");

      setProperty(writer, name, props.get(name), "id");
      setProperty(writer, name, props.get(name), "desc");
      setProperty(writer, name, props.get(name), "label");
      setProperty(writer, name, props.get(name), "buttonLabel");
      setProperty(writer, name, props.get(name), "menuLabel");
      setProperty(writer, name, props.get(name), "windowMode");
      setProperty(writer, name, props.get(name), "context");
      // Any additional textual properties would be added here...

      setPropertyBool(writer, name, props.get(name), "visible");
      setPropertyBool(writer, name, props.get(name), "enabled");
      setPropertyBool(writer, name, props.get(name), "checkable");
      setPropertyBool(writer, name, props.get(name), "checked");
      setPropertyBool(writer, name, props.get(name), "rebindable");
      
      if (images.hasImage(name))
      {
        String resourceName = images.getImageRef(name);
        if (images.hasImage(name + "2x"))
          resourceName = "new ImageResource2x(" + images.getImageRef(name + "2x") + ")";

        writer.println(name + "_.setImageResource(" + resourceName + ");");
      }

      writer.println("addCommand(\"" + Generator.escape(name) + "\", " + name + "_);");
      writer.println();
   }

   private void emitCommandAccessors(SourceWriter writer)
   {
      for (JMethod method : commandMethods_)
      {
         String name = method.getName();
         writer.println("public AppCommand " + name + "() {");
         writer.indent();
         writer.println("return " + name + "_;");
         writer.outdent();
         writer.println("}");
      }
   }

   private void setProperty(SourceWriter writer,
                            String name,
                            Element props,
                            String propertyName)
   {
      if (props == null)
         return;
      // This check is important because getAttribute() returns empty string
      // even if the attribute isn't present, which is not what we want. In
      // the command system, empty string is distinct from null.
      if (!props.hasAttribute(propertyName))
         return;

      String value = props.getAttribute(propertyName);

      String setter = "set" + Character.toUpperCase(propertyName.charAt(0))
            + propertyName.substring(1);
      writer.println(name + "_." + setter
                                   + "(\"" + Generator.escape(value) + "\");"); 
   }

   private void setPropertyBool(SourceWriter writer,
                                String name,
                                Element props,
                                String propertyName)
   {
      if (props == null)
         return;
      // This check is important because getAttribute() returns empty string
      // even if the attribute isn't present, which is not what we want. In
      // the command system, empty string is distinct from null.
      if (!props.hasAttribute(propertyName))
         return;

      String value = props.getAttribute(propertyName);

      String setter = "set" + Character.toUpperCase(propertyName.charAt(0))
            + propertyName.substring(1);
      writer.println(name + "_." + setter
                     + "(" + value + ");");
   }

   private ImageResourceInfo generateImageBundle()
   {
      String className = bundleType_.getSimpleSourceName() + "__AutoGenResources";
      String pathToInstance = packageName_ + "." + className + ".INSTANCE";
      ImageResourceInfo iri = new ImageResourceInfo(pathToInstance);

      PrintWriter printWriter = context_.tryCreate(logger_,
                                                  packageName_,
                                                  className);
      if (printWriter == null)
         return null;

      ClassSourceFileComposerFactory factory =
            new ClassSourceFileComposerFactory(packageName_, className);
      factory.addImport("com.google.gwt.core.client.GWT");
      factory.addImport("com.google.gwt.resources.client.*");
      factory.makeInterface();
      factory.addImplementedInterface("ClientBundle");
      SourceWriter writer = factory.createSourceWriter(context_, printWriter);

      Set<String> resourceNames = context_.getResourcesOracle().getPathNames();
      for (JMethod method : commandMethods_)
      {
         String commandId = method.getName();

         String key = packageName_.replace('.', '/') + "/" + commandId;

         if (resourceNames.contains(key + ".png"))
         {
            writer.println("ImageResource " + commandId + "();");
            iri.addImage(commandId);
         }

         if (resourceNames.contains(key + "_2x.png"))
         {
            writer.println("@Source(\"" + commandId + "_2x.png\")");
            writer.println("ImageResource " + commandId + "2x();");
            iri.addImage(commandId + "2x");
         }
      }
      writer.println("public static final " + className + " INSTANCE = " +
                     "(" + className + ")GWT.create(" + className + ".class);");

      writer.outdent();
      writer.println("}");
      context_.commit(logger_, printWriter);

      return iri;

   }

   public Map<String, Element> getCommandProperties() throws UnableToCompleteException
   {
      Map<String, Element> properties = new HashMap<String, Element>();

      NodeList nodes = getConfigDoc("/commands/cmd");

      for (int i = 0; i < nodes.getLength(); i++)
      {
         Element cmd = (Element) nodes.item(i);
         String id = cmd.getAttribute("id");

         properties.put(id, cmd);
      }

      return properties;
   }

   private NodeList getConfigDoc(String xpath) throws UnableToCompleteException
   {
      try
      {
         String resourceName =
               bundleType_.getQualifiedSourceName().replace('.', '/') + ".cmd.xml";
         Resource resource = context_.getResourcesOracle().getResource(resourceName);
         if (resource == null)
            return null;

         Object result = XPathFactory.newInstance().newXPath().evaluate(
               xpath,
               new InputSource(resource.getLocation()),
               XPathConstants.NODESET);
         return (NodeList) result;
      }
      catch (Exception e)
      {
         logger_.log(TreeLogger.Type.ERROR, "Barf", e);
         throw new UnableToCompleteException();
      }
   }

   private final TreeLogger logger_;
   private final GeneratorContext context_;
   private final JClassType bundleType_;
   private final JMethod[] commandMethods_;
   private final JMethod[] menuMethods_;
   @SuppressWarnings("unused")
   private final JMethod[] shortcutsMethods_;
   private final String packageName_;
   private String simpleName_;
}

class ImageResourceInfo
{
   public ImageResourceInfo(String imagesRefPath)
   {
      this.imagesRefPath_ = imagesRefPath;
   }

   public void addImage(String commandId)
   {
      imageIds_.add(commandId);
   }

   public boolean hasImage(String commandId)
   {
      return imageIds_.contains(commandId);
   }

   public String getImageRef(String commandId)
   {
      assert hasImage(commandId);
      return imagesRefPath_ + "." + commandId + "()";
   }

   private final String imagesRefPath_;
   private final HashSet<String> imageIds_ = new HashSet<String>();
}
