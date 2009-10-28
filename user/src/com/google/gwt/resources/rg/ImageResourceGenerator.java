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
package com.google.gwt.resources.rg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.dev.util.Util;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.resources.client.impl.ImageResourcePrototype;
import com.google.gwt.resources.ext.AbstractResourceGenerator;
import com.google.gwt.resources.ext.ClientBundleFields;
import com.google.gwt.resources.ext.ClientBundleRequirements;
import com.google.gwt.resources.ext.ResourceContext;
import com.google.gwt.resources.ext.ResourceGeneratorUtil;
import com.google.gwt.resources.rg.ImageBundleBuilder.Arranger;
import com.google.gwt.resources.rg.ImageBundleBuilder.ImageRect;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.StringSourceWriter;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Builds an image strip for all ImageResources defined within an ClientBundle.
 */
public final class ImageResourceGenerator extends AbstractResourceGenerator {
  /**
   * This is shared that can be shared across permutations for a given
   * ClientBundle type.
   */
  static class CachedState {
    /**
     * Associates an ImageRect with the ImageBundleBuilder that will emit its
     * bytes.
     */
    public final Map<ImageRect, ImageBundleBuilder> buildersByImageRect = new IdentityHashMap<ImageRect, ImageBundleBuilder>();

    /**
     * Associates a layout constraint with an ImageBundleBuilder that can
     * satisfy that constraint.
     */
    public final Map<RepeatStyle, ImageBundleBuilder> buildersByRepeatStyle = new EnumMap<RepeatStyle, ImageBundleBuilder>(
        RepeatStyle.class);

    /**
     * Associates a method name with the ImageRect that contains the data for
     * that method.
     */
    public final Map<String, ImageRect> imageRectsByName = new HashMap<String, ImageRect>();

    /**
     * Records that ImageRects that also need to provide an RTL-flipped version.
     */
    public final Set<ImageRect> rtlImages = new HashSet<ImageRect>();

    public final Map<ImageBundleBuilder, URL[]> urlsByBuilder = new IdentityHashMap<ImageBundleBuilder, URL[]>();

    /**
     * Maps an ImageRect to two URLs that contain the normal and flipped
     * contents.
     */
    public final Map<ImageRect, URL[]> urlsByExternalImageRect = new IdentityHashMap<ImageRect, URL[]>();
  }

  /**
   * This data is specific to a particular permutation.
   */
  static class LocalState {
    /**
     * Maps resource URLs to field names within the generated ClientBundle type.
     * These fields will be statically initialized to an expression that can be
     * used to access the contents of the resource URL.
     * 
     * @see ImageResourceGenerator#maybeDeploy
     */
    public final Map<URL, String> fieldNamesByUrl = new HashMap<URL, String>();

    /**
     * Maps an ImageRect to a pair of Java expressions. The first can be used to
     * access the normal version of the resource, while the second, optional,
     * field is used to access an RTL-flipped version.
     */
    public final Map<ImageRect, String[]> urlExpressionsByImageRect = new HashMap<ImageRect, String[]>();
  }

  /**
   * This is set to <code>true</code> by {@link #init} if {@link #shared} was
   * initialized from cached data.
   */
  private boolean prepared;
  private CachedState shared;
  private LocalState local;
  private JType stringType;

  @Override
  public String createAssignment(TreeLogger logger, ResourceContext context,
      JMethod method) throws UnableToCompleteException {
    String name = method.getName();

    SourceWriter sw = new StringSourceWriter();
    sw.println("new " + ImageResourcePrototype.class.getName() + "(");
    sw.indent();
    sw.println('"' + name + "\",");

    ImageRect rect = shared.imageRectsByName.get(name);
    assert rect != null : "No ImageRect ever computed for " + name;

    String[] urlExpressions = local.urlExpressionsByImageRect.get(rect);
    assert urlExpressions != null : "No URL expression for " + name;
    assert urlExpressions.length == 2;

    if (urlExpressions[1] == null) {
      sw.println(urlExpressions[0] + ",");
    } else {
      sw.println("com.google.gwt.i18n.client.LocaleInfo.getCurrentLocale().isRTL() ?"
          + urlExpressions[1] + " : " + urlExpressions[0] + ",");
    }
    sw.println(rect.getLeft() + ", " + rect.getTop() + ", " + rect.getWidth()
        + ", " + rect.getHeight() + ", " + rect.isAnimated());

    sw.outdent();
    sw.print(")");

    return sw.toString();
  }

  /**
   * We use this as a signal that we have received all image methods and can now
   * create the bundled images.
   */
  @Override
  public void createFields(TreeLogger logger, ResourceContext context,
      ClientBundleFields fields) throws UnableToCompleteException {
    if (!prepared) {
      finalizeArrangements(logger, context);
    }

    for (ImageRect rect : shared.imageRectsByName.values()) {
      String[] urlExpressions;
      {
        URL[] contents;
        ImageBundleBuilder builder = shared.buildersByImageRect.get(rect);
        if (builder == null) {
          contents = shared.urlsByExternalImageRect.get(rect);
        } else {
          contents = shared.urlsByBuilder.get(builder);
        }
        assert contents != null && contents.length == 2;

        urlExpressions = new String[2];
        urlExpressions[0] = maybeDeploy(context, fields, contents[0]);
        urlExpressions[1] = maybeDeploy(context, fields, contents[1]);
      }
      local.urlExpressionsByImageRect.put(rect, urlExpressions);
    }
  }

  @Override
  public void finish(TreeLogger logger, ResourceContext context)
      throws UnableToCompleteException {
    local = null;
  }

  @Override
  public void init(TreeLogger logger, ResourceContext context) {
    // The images are bundled differently when data resources are supported
    String key = context.getClientBundleType().getQualifiedSourceName() + ":"
        + context.supportsDataUrls();
    shared = context.getCachedData(key, CachedState.class);
    prepared = shared != null;
    if (prepared) {
      logger.log(TreeLogger.DEBUG, "Using cached data");
    } else {
      shared = new CachedState();
      context.putCachedData(key, shared);
    }
    local = new LocalState();

    stringType = context.getGeneratorContext().getTypeOracle().findType(
        String.class.getCanonicalName());
    assert stringType != null : "No String type";
  }

  /**
   * Process each image method. This will either assign the image to an
   * ImageBundleBuilder or reencode an external image.
   */
  @Override
  public void prepare(TreeLogger logger, ResourceContext context,
      ClientBundleRequirements requirements, JMethod method)
      throws UnableToCompleteException {
    if (prepared) {
      return;
    }

    URL[] resources = ResourceGeneratorUtil.findResources(logger, context,
        method);

    if (resources.length != 1) {
      logger.log(TreeLogger.ERROR, "Exactly one image may be specified", null);
      throw new UnableToCompleteException();
    }

    ImageBundleBuilder builder = getBuilder(method);
    URL resource = resources[0];
    String name = method.getName();

    ImageRect rect;
    try {
      rect = builder.assimilate(logger, name, resource);
      if (context.supportsDataUrls()
          || getRepeatStyle(method) == RepeatStyle.Both) {
        // Just use the calculated meta-data
        builder.removeMapping(name);
        rect.setPosition(0, 0);
        throw new UnsuitableForStripException(rect);
      }
      shared.buildersByImageRect.put(rect, builder);
    } catch (UnsuitableForStripException e) {
      // Add the image to the output as a separate resource
      URL normalContents;
      rect = e.getImageRect();

      if (rect.isAnimated()) {
        // Can't re-encode animated images, so we emit it as-is
        normalContents = resource;
      } else {
        normalContents = reencodeToTempFile(logger, rect);
      }
      shared.urlsByExternalImageRect.put(rect, new URL[] {normalContents, null});
    }

    shared.imageRectsByName.put(name, rect);

    if (getFlipRtl(method)) {
      shared.rtlImages.add(rect);
    }
  }

  private void finalizeArrangements(TreeLogger logger, ResourceContext context)
      throws UnableToCompleteException {
    for (Map.Entry<RepeatStyle, ImageBundleBuilder> entry : shared.buildersByRepeatStyle.entrySet()) {
      RepeatStyle repeatStyle = entry.getKey();
      ImageBundleBuilder builder = entry.getValue();
      Arranger arranger;

      switch (repeatStyle) {
        case None:
          arranger = new ImageBundleBuilder.BestFitArranger();
          break;
        case Horizontal:
          arranger = new ImageBundleBuilder.VerticalArranger();
          break;
        case Vertical:
          arranger = new ImageBundleBuilder.HorizontalArranger();
          break;
        case Both:
          // This is taken care of when writing the external images;
          continue;
        default:
          logger.log(TreeLogger.ERROR, "Unknown RepeatStyle" + repeatStyle);
          throw new UnableToCompleteException();
      }
      URL normalContents = renderToTempFile(logger, builder, arranger);

      shared.urlsByBuilder.put(builder, new URL[] {normalContents, null});
    }

    if (shared.rtlImages.size() > 0) {
      Set<ImageBundleBuilder> rtlBuilders = new HashSet<ImageBundleBuilder>();

      for (ImageRect rtlImage : shared.rtlImages) {
        // Create a transformation to mirror about the Y-axis and translate
        AffineTransform tx = new AffineTransform();
        tx.setTransform(-1, 0, 0, 1, rtlImage.getWidth(), 0);
        rtlImage.setTransform(tx);

        if (shared.buildersByImageRect.containsKey(rtlImage)) {
          /*
           * This image is assigned to a builder, so we'll just remember to
           * regenerate that builder.
           */
          rtlBuilders.add(shared.buildersByImageRect.get(rtlImage));
        } else {
          // Otherwise, emit the external version
          URL[] contents = shared.urlsByExternalImageRect.get(rtlImage);
          assert contents != null;
          contents[1] = reencodeToTempFile(logger, rtlImage);
        }
      }

      for (ImageBundleBuilder builder : rtlBuilders) {
        URL[] contents = shared.urlsByBuilder.get(builder);
        assert contents != null && contents.length == 2;

        contents[1] = renderToTempFile(logger, builder,
            new ImageBundleBuilder.IdentityArranger());
      }
    }
  }

  private ImageBundleBuilder getBuilder(JMethod method) {
    RepeatStyle repeatStyle = getRepeatStyle(method);
    ImageBundleBuilder builder = shared.buildersByRepeatStyle.get(repeatStyle);
    if (builder == null) {
      builder = new ImageBundleBuilder();
      shared.buildersByRepeatStyle.put(repeatStyle, builder);
    }
    return builder;
  }

  private boolean getFlipRtl(JMethod method) {
    ImageOptions options = method.getAnnotation(ImageOptions.class);
    if (options == null) {
      return false;
    } else {
      return options.flipRtl();
    }
  }

  private RepeatStyle getRepeatStyle(JMethod method) {
    ImageOptions options = method.getAnnotation(ImageOptions.class);
    if (options == null) {
      return RepeatStyle.None;
    } else {
      return options.repeatStyle();
    }
  }

  /**
   * Create a field in the ClientBundle type that is used to intern the URL
   * expressions that can be used to access the contents of the given resource.
   * 
   * @return the name of the field that was created
   */
  private String maybeDeploy(ResourceContext context,
      ClientBundleFields fields, URL resource) throws UnableToCompleteException {
    if (resource == null) {
      return null;
    }

    String toReturn = local.fieldNamesByUrl.get(resource);
    if (toReturn == null) {
      String urlExpression = context.deploy(resource, false);
      toReturn = fields.define(stringType, "internedUrl"
          + local.fieldNamesByUrl.size(), urlExpression, true, true);
      local.fieldNamesByUrl.put(resource, toReturn);
    }
    return toReturn;
  }

  /**
   * Re-encode an image as a PNG to strip random header data.
   */
  private URL reencodeToTempFile(TreeLogger logger, ImageRect rect)
      throws UnableToCompleteException {
    try {
      byte[] imageBytes = ImageBundleBuilder.toPng(logger, rect);

      if (imageBytes == null) {
        return null;
      }

      File file = File.createTempFile(
          ImageResourceGenerator.class.getSimpleName(), ".png");
      file.deleteOnExit();
      Util.writeBytesToFile(logger, file, imageBytes);
      return file.toURI().toURL();
    } catch (IOException ex) {
      logger.log(TreeLogger.ERROR, "Unable to write re-encoded PNG", ex);
      throw new UnableToCompleteException();
    }
  }

  /**
   * Re-encode an image as a PNG to strip random header data.
   */
  private URL renderToTempFile(TreeLogger logger, ImageBundleBuilder builder,
      Arranger arranger) throws UnableToCompleteException {
    try {
      byte[] imageBytes = builder.render(logger, arranger);
      if (imageBytes == null) {
        return null;
      }

      File file = File.createTempFile(
          ImageResourceGenerator.class.getSimpleName(), ".png");
      file.deleteOnExit();
      Util.writeBytesToFile(logger, file, imageBytes);
      return file.toURI().toURL();
    } catch (IOException ex) {
      logger.log(TreeLogger.ERROR, "Unable to write re-encoded PNG", ex);
      throw new UnableToCompleteException();
    }
  }
}
