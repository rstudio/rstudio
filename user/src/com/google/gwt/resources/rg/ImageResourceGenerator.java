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
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.resources.client.impl.ImageResourcePrototype;
import com.google.gwt.resources.ext.ClientBundleFields;
import com.google.gwt.resources.ext.ClientBundleRequirements;
import com.google.gwt.resources.ext.ResourceContext;
import com.google.gwt.resources.ext.ResourceGeneratorUtil;
import com.google.gwt.resources.rg.ImageBundleBuilder.Arranger;
import com.google.gwt.resources.rg.ImageBundleBuilder.ImageRect;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.StringSourceWriter;

import java.awt.geom.AffineTransform;
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
  private static final String[] DEFAULT_EXTENSIONS = new String[] {
      ".png", ".jpg", ".gif", ".bmp"};
  private Map<String, ImageRect> imageRectsByName;
  private Map<ImageRect, ImageBundleBuilder> buildersByImageRect;
  private Map<RepeatStyle, ImageBundleBuilder> buildersByRepeatStyle;
  private Map<ImageBundleBuilder, String[]> urlsByBuilder;
  private Map<ImageRect, String[]> urlsByExternalImageRect;
  private Map<ImageRect, ImageBundleBuilder> rtlImages;

  @Override
  public String createAssignment(TreeLogger logger, ResourceContext context,
      JMethod method) throws UnableToCompleteException {
    String name = method.getName();

    SourceWriter sw = new StringSourceWriter();
    sw.println("new " + ImageResourcePrototype.class.getName() + "(");
    sw.indent();
    sw.println('"' + name + "\",");

    ImageRect rect = imageRectsByName.get(name);
    assert rect != null : "No ImageRect ever computed for " + name;

    String[] urlExpressions;
    {
      ImageBundleBuilder builder = buildersByImageRect.get(rect);
      if (builder == null) {
        urlExpressions = urlsByExternalImageRect.get(rect);
      } else {
        urlExpressions = urlsByBuilder.get(builder);
      }
    }
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

  @Override
  public void createFields(TreeLogger logger, ResourceContext context,
      ClientBundleFields fields) throws UnableToCompleteException {

    TypeOracle typeOracle = context.getGeneratorContext().getTypeOracle();
    JClassType stringType = typeOracle.findType(String.class.getName());
    assert stringType != null;

    Map<ImageBundleBuilder, String> prettyNames = new IdentityHashMap<ImageBundleBuilder, String>();

    for (Map.Entry<RepeatStyle, ImageBundleBuilder> entry : buildersByRepeatStyle.entrySet()) {
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

      String bundleUrlExpression = builder.writeBundledImage(logger.branch(
          TreeLogger.DEBUG, "Writing image strip", null), context, arranger);

      if (bundleUrlExpression == null) {
        continue;
      }

      String prettyName = "imageUrl" + repeatStyle;
      prettyNames.put(builder, prettyName);
      String fieldName = fields.define(stringType, prettyName,
          bundleUrlExpression, true, true);
      String[] strings = {fieldName, null};
      urlsByBuilder.put(builder, strings);
    }

    if (rtlImages.size() > 0) {
      Set<ImageBundleBuilder> rtlBuilders = new HashSet<ImageBundleBuilder>();

      for (Map.Entry<ImageRect, ImageBundleBuilder> entry : rtlImages.entrySet()) {
        ImageRect rtlImage = entry.getKey();

        AffineTransform tx = new AffineTransform();
        tx.setTransform(-1, 0, 0, 1, rtlImage.getWidth(), 0);

        rtlImage.setTransform(tx);

        if (buildersByImageRect.containsKey(rtlImage)) {
          rtlBuilders.add(buildersByImageRect.get(rtlImage));
        } else {
          String[] strings = urlsByExternalImageRect.get(rtlImage);
          assert strings != null;
          byte[] imageBytes = ImageBundleBuilder.toPng(logger, rtlImage);
          strings[1] = context.deploy(rtlImage.getName() + "_rtl.png",
              "image/png", imageBytes, false);
        }
      }

      for (ImageBundleBuilder builder : rtlBuilders) {
        String bundleUrlExpression = builder.writeBundledImage(logger.branch(
            TreeLogger.DEBUG, "Writing image strip", null), context,
            new ImageBundleBuilder.IdentityArranger());

        if (bundleUrlExpression == null) {
          continue;
        }

        String prettyName = prettyNames.get(builder);
        String[] strings = urlsByBuilder.get(builder);
        assert strings != null;

        strings[1] = fields.define(stringType, prettyName + "_rtl",
            bundleUrlExpression, true, true);
      }
    }
  }

  @Override
  public void init(TreeLogger logger, ResourceContext context) {
    imageRectsByName = new HashMap<String, ImageRect>();
    buildersByImageRect = new IdentityHashMap<ImageRect, ImageBundleBuilder>();
    buildersByRepeatStyle = new EnumMap<RepeatStyle, ImageBundleBuilder>(
        RepeatStyle.class);
    rtlImages = new IdentityHashMap<ImageRect, ImageBundleBuilder>();
    urlsByBuilder = new IdentityHashMap<ImageBundleBuilder, String[]>();
    urlsByExternalImageRect = new IdentityHashMap<ImageRect, String[]>();
  }

  @Override
  public void prepare(TreeLogger logger, ResourceContext context,
      ClientBundleRequirements requirements, JMethod method)
      throws UnableToCompleteException {
    URL[] resources = ResourceGeneratorUtil.findResources(logger, context,
        method, DEFAULT_EXTENSIONS);

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
      buildersByImageRect.put(rect, builder);
    } catch (UnsuitableForStripException e) {
      // Add the image to the output as a separate resource
      rect = e.getImageRect();

      String urlExpression;
      if (rect.isAnimated()) {
        // Can't re-encode animated images, so we emit it as-is
        urlExpression = context.deploy(resource, false);
      } else {
        // Re-encode the image as a PNG to strip random header data
        byte[] imageBytes = ImageBundleBuilder.toPng(logger, rect);
        urlExpression = context.deploy(rect.getName() + ".png", "image/png",
            imageBytes, false);
      }
      urlsByExternalImageRect.put(rect, new String[] {urlExpression, null});
    }

    imageRectsByName.put(name, rect);

    if (getFlipRtl(method)) {
      rtlImages.put(rect, null);
    }
  }

  private ImageBundleBuilder getBuilder(JMethod method) {
    RepeatStyle repeatStyle = getRepeatStyle(method);
    ImageBundleBuilder builder = buildersByRepeatStyle.get(repeatStyle);
    if (builder == null) {
      builder = new ImageBundleBuilder();
      buildersByRepeatStyle.put(repeatStyle, builder);
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
}
