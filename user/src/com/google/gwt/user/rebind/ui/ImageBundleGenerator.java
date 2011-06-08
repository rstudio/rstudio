/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.user.rebind.ui;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.client.ui.ImageBundle;
import com.google.gwt.user.client.ui.ImageBundle.Resource;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates an implementation of a user-defined interface <code>T</code> that
 * extends {@link com.google.gwt.user.client.ui.ImageBundle}.
 * 
 * Each method in <code>T</code> must be declared to return
 * {@link com.google.gwt.user.client.ui.AbstractImagePrototype}, take no
 * parameters, and optionally specify the metadata tag <code>gwt.resource</code>
 * as the name of an image that can be found in the classpath. In the absence of
 * the metatadata tag, the method name with an extension of
 * <code>.png, .jpg, or .gif</code> defines the name of the image, and the
 * image file must be located in the same package as <code>T</code>.
 */
public class ImageBundleGenerator extends Generator {

  /**
   * Simple wrapper around JMethod that allows for unit test mocking.
   */
  interface JMethodOracle {

    @SuppressWarnings("deprecation")
    Resource getAnnotation(Class<Resource> clazz);

    String getName();

    String getPackageName();
  }

  /**
   * Indirection around the act of looking up a resource that allows for unit
   * test mocking.
   */
  /* private */interface ResourceLocator {
    /**
     * 
     * @param resName the resource name in a format that could be passed to
     *          <code>ClassLoader.getResource()</code>
     * @return <code>true</code> if the resource is present
     */
    boolean isResourcePresent(String resName);
  }

  private static class JMethodOracleImpl implements JMethodOracle {
    private final JMethod delegate;

    public JMethodOracleImpl(JMethod delegate) {
      this.delegate = delegate;
    }

    @SuppressWarnings("deprecation")
    public Resource getAnnotation(Class<Resource> clazz) {
      return delegate.getAnnotation(clazz);
    }

    public String getName() {
      return delegate.getName();
    }

    public String getPackageName() {
      return delegate.getEnclosingType().getPackage().getName();
    }
  }

  /* private */static final String MSG_NO_FILE_BASED_ON_METHOD_NAME = "No matching image resource was found; any of the following filenames would have matched had they been present:";

  private static final String ABSTRACTIMAGEPROTOTYPE_QNAME = "com.google.gwt.user.client.ui.AbstractImagePrototype";

  private static final String CLIPPEDIMAGEPROTOTYPE_QNAME = "com.google.gwt.user.client.ui.impl.ClippedImagePrototype";

  private static final String GWT_QNAME = "com.google.gwt.core.client.GWT";

  private static final String[] IMAGE_FILE_EXTENSIONS = {"png", "gif", "jpg"};

  private static final String IMAGEBUNDLE_QNAME = "com.google.gwt.user.client.ui.ImageBundle";

  /* private */static String msgCannotFindImageFromMetaData(String imgResName) {
    return "Unable to find image resource '" + imgResName + "'";
  }

  private final ResourceLocator resLocator;

  /**
   * Default constructor for image bundle. Locates resources using this class's
   * own class loader.
   */
  public ImageBundleGenerator() {
    this(new ResourceLocator() {
      public boolean isResourcePresent(String resName) {
        URL url = getClass().getClassLoader().getResource(resName);
        return url != null;
      }
    });
  }

  /**
   * Default access so that it can be accessed by unit tests.
   */
  /* private */ImageBundleGenerator(ResourceLocator resourceLocator) {
    assert (resourceLocator != null);
    this.resLocator = resourceLocator;
  }

  @Override
  public String generate(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {

    TypeOracle typeOracle = context.getTypeOracle();

    // Get metadata describing the user's class.
    JClassType userType = getValidUserType(logger, typeName, typeOracle);

    // Write the new class.
    JMethod[] imgMethods = userType.getOverridableMethods();
    String resultName = generateImplClass(logger, context, userType, imgMethods);

    // Return the complete name of the generated class.
    return resultName;
  }

  /**
   * Gets the resource name of the image associated with the specified image
   * bundle method in a form that can be passed to
   * <code>ClassLoader.getResource()</code>.
   * 
   * @param logger the main logger
   * @param method the image bundle method whose image name is being sought
   * @return a resource name that is suitable to be passed into
   *         <code>ClassLoader.getResource()</code>; never returns
   *         <code>null</code>
   * @throws UnableToCompleteException thrown if a resource was specified but
   *           could not be found on the classpath
   */
  /* private */String getImageResourceName(TreeLogger logger,
      JMethodOracle method) throws UnableToCompleteException {
    String imgName = tryGetImageNameFromMetaData(logger, method);
    if (imgName != null) {
      return imgName;
    } else {
      return getImageNameFromMethodName(logger, method);
    }
  }

  private String computeSubclassName(JClassType userType) {
    String baseName = userType.getName().replace('.', '_');
    return baseName + "_generatedBundle";
  }

  private void generateImageMethod(ImageBundleBuilder compositeImage,
      SourceWriter sw, JMethod method, String imgResName) {

    String decl = method.getReadableDeclaration(false, true, true, true, true);

    {
      sw.indent();

      // Create a singleton that this method can return. There is no need to
      // create a new instance every time this method is called, since
      // ClippedImagePrototype is immutable

      ImageBundleBuilder.ImageRect imageRect = compositeImage.getMapping(imgResName);
      String singletonName = method.getName() + "_SINGLETON";

      sw.print("private static final ClippedImagePrototype ");
      sw.print(singletonName);
      sw.print(" = new ClippedImagePrototype(IMAGE_BUNDLE_URL, ");
      sw.print(Integer.toString(imageRect.getLeft()));
      sw.print(", ");
      sw.print(Integer.toString(imageRect.getTop()));
      sw.print(", ");
      sw.print(Integer.toString(imageRect.getWidth()));
      sw.print(", ");
      sw.print(Integer.toString(imageRect.getHeight()));
      sw.println(");");

      sw.print(decl);
      sw.println(" {");

      {
        sw.indent();
        sw.print("return ");
        sw.print(singletonName);
        sw.println(";");
        sw.outdent();
      }

      sw.println("}");
      sw.outdent();
    }
  }

  /**
   * Generates the image bundle implementation class, checking each method for
   * validity as it is encountered.
   */
  private String generateImplClass(TreeLogger logger, GeneratorContext context,
      JClassType userType, JMethod[] imageMethods)
      throws UnableToCompleteException {
    // Lookup the type info for AbstractImagePrototype so that we can check for
    // the proper return type
    // on image bundle methods.
    final JClassType abstractImagePrototype;
    try {
      abstractImagePrototype = userType.getOracle().getType(
          ABSTRACTIMAGEPROTOTYPE_QNAME);
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, "GWT " + ABSTRACTIMAGEPROTOTYPE_QNAME
          + " class is not available", e);
      throw new UnableToCompleteException();
    }

    // Compute the package and class names of the generated class.
    String pkgName = userType.getPackage().getName();
    String subName = computeSubclassName(userType);

    // Begin writing the generated source.
    ClassSourceFileComposerFactory f = new ClassSourceFileComposerFactory(
        pkgName, subName);
    f.addImport(ABSTRACTIMAGEPROTOTYPE_QNAME);
    f.addImport(CLIPPEDIMAGEPROTOTYPE_QNAME);
    f.addImport(GWT_QNAME);
    f.addImplementedInterface(userType.getQualifiedSourceName());

    PrintWriter pw = context.tryCreate(logger, pkgName, subName);
    if (pw != null) {
      SourceWriter sw = f.createSourceWriter(context, pw);

      // Build a compound image from each individual image.
      ImageBundleBuilder bulder = new ImageBundleBuilder();

      // Store the computed image names so that we don't have to lookup them up
      // again.
      List<String> imageResNames = new ArrayList<String>();

      for (JMethod method : imageMethods) {
        String branchMsg = "Analyzing method '" + method.getName()
            + "' in type " + userType.getQualifiedSourceName();
        TreeLogger branch = logger.branch(TreeLogger.DEBUG, branchMsg, null);

        // Verify that this method is valid on an image bundle.
        if (method.getReturnType() != abstractImagePrototype) {
          branch.log(TreeLogger.ERROR, "Return type must be "
              + ABSTRACTIMAGEPROTOTYPE_QNAME, null);
          throw new UnableToCompleteException();
        }

        if (method.getParameters().length > 0) {
          branch.log(TreeLogger.ERROR, "Method must have zero parameters", null);
          throw new UnableToCompleteException();
        }

        // Find the associated imaged resource.
        String imageResName = getImageResourceName(branch,
            new JMethodOracleImpl(method));
        assert (imageResName != null);
        imageResNames.add(imageResName);
        bulder.assimilate(logger, imageResName);
      }

      // Write the compound image into the output directory.
      String bundledImageUrl = bulder.writeBundledImage(logger, context);

      // Emit a constant for the composite URL. Note that we prepend the
      // module's base URL so that the module can reference its own resources
      // independently of the host HTML page.
      sw.print("private static final String IMAGE_BUNDLE_URL = GWT.getModuleBaseURL() + \"");
      sw.print(escape(bundledImageUrl));
      sw.println("\";");

      // Generate an implementation of each method.
      int imageResNameIndex = 0;
      for (JMethod method : imageMethods) {
        generateImageMethod(bulder, sw, method,
            imageResNames.get(imageResNameIndex++));
      }

      // Finish.
      sw.commit(logger);
    }

    return f.getCreatedClassName();
  }

  /**
   * Attempts to get the image name from the name of the method itself by
   * speculatively appending various image-like file extensions in a prioritized
   * order. The first image found, if any, is used.
   * 
   * @param logger if no matching image resource is found, an explanatory
   *          message will be logged
   * @param method the method whose name is being examined for matching image
   *          resources
   * @return a resource name that is suitable to be passed into
   *         <code>ClassLoader.getResource()</code>; never returns
   *         <code>null</code>
   * @throws UnableToCompleteException thrown when no image can be found based
   *           on the method name
   */
  private String getImageNameFromMethodName(TreeLogger logger,
      JMethodOracle method) throws UnableToCompleteException {
    String pkgName = method.getPackageName();
    String pkgPrefix = pkgName.replace('.', '/');
    if (pkgPrefix.length() > 0) {
      pkgPrefix += "/";
    }
    String methodName = method.getName();
    String pkgAndMethodName = pkgPrefix + methodName;
    List<String> testImgNames = new ArrayList<String>();
    for (int i = 0; i < IMAGE_FILE_EXTENSIONS.length; i++) {
      String testImgName = pkgAndMethodName + '.' + IMAGE_FILE_EXTENSIONS[i];
      if (resLocator.isResourcePresent(testImgName)) {
        return testImgName;
      }
      testImgNames.add(testImgName);
    }

    TreeLogger branch = logger.branch(TreeLogger.ERROR,
        MSG_NO_FILE_BASED_ON_METHOD_NAME, null);
    for (String testImgName : testImgNames) {
      branch.log(TreeLogger.ERROR, testImgName, null);
    }

    throw new UnableToCompleteException();
  }

  private JClassType getValidUserType(TreeLogger logger, String typeName,
      TypeOracle typeOracle) throws UnableToCompleteException {
    try {
      // Get the type that the user is introducing.
      JClassType userType = typeOracle.getType(typeName);

      // Get the type this generator is designed to support.
      JClassType markerType = typeOracle.findType(IMAGEBUNDLE_QNAME);

      // Ensure it's an interface.
      if (userType.isInterface() == null) {
        logger.log(TreeLogger.ERROR, userType.getQualifiedSourceName()
            + " must be an interface", null);
        throw new UnableToCompleteException();
      }

      // Ensure proper derivation.
      if (!userType.isAssignableTo(markerType)) {
        logger.log(TreeLogger.ERROR, userType.getQualifiedSourceName()
            + " must be assignable to " + markerType.getQualifiedSourceName(),
            null);
        throw new UnableToCompleteException();
      }

      return userType;

    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, "Unable to find required type(s)", e);
      throw new UnableToCompleteException();
    }
  }

  /**
   * Attempts to get the image name (verbatim) from an annotation.
   * 
   * @return the string specified in in the {@link ImageBundle.Resource}
   *         annotation, or <code>null</code>
   */
  @SuppressWarnings("deprecation")
  private String tryGetImageNameFromAnnotation(JMethodOracle method) {
    ImageBundle.Resource imgResAnn = method.getAnnotation(ImageBundle.Resource.class);
    String imgName = null;
    if (imgResAnn != null) {
      imgName = imgResAnn.value();
    }
    return imgName;
  }

  /**
   * Attempts to get the image name from an annotation.
   * 
   * @param logger if an annotation is found but the specified resource isn't
   *          available, an error is logged
   * @param method the image bundle method whose associated image resource is
   *          being sought
   * @return a resource name that is suitable to be passed into
   *         <code>ClassLoader.getResource()</code>, or <code>null</code>
   *         if metadata wasn't provided
   * @throws UnableToCompleteException thrown when metadata is provided but the
   *           resource cannot be found
   */
  private String tryGetImageNameFromMetaData(TreeLogger logger,
      JMethodOracle method) throws UnableToCompleteException {
    String imgFileName = tryGetImageNameFromAnnotation(method);
    if (imgFileName == null) {
      // Exit early because neither an annotation nor javadoc was found.
      return null;
    }

    // If the name has no slashes (that is, it isn't a fully-qualified resource
    // name), then prepend the enclosing package name automatically, being
    // careful about the default package.
    if (imgFileName.indexOf("/") == -1) {
      String pkgName = method.getPackageName();
      if (!"".equals(pkgName)) {
        imgFileName = pkgName.replace('.', '/') + "/" + imgFileName;
      }
    }

    if (!resLocator.isResourcePresent(imgFileName)) {
      // Not found.
      logger.log(TreeLogger.ERROR, msgCannotFindImageFromMetaData(imgFileName),
          null);
      throw new UnableToCompleteException();
    }

    // Success.
    return imgFileName;
  }
}
