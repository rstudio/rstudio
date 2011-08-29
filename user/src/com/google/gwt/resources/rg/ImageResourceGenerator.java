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

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.dev.util.StringKey;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.collect.Maps;
import com.google.gwt.dev.util.collect.Sets;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.resources.client.impl.ImageResourcePrototype;
import com.google.gwt.resources.ext.AbstractResourceGenerator;
import com.google.gwt.resources.ext.ClientBundleFields;
import com.google.gwt.resources.ext.ClientBundleRequirements;
import com.google.gwt.resources.ext.ResourceContext;
import com.google.gwt.resources.ext.ResourceGeneratorUtil;
import com.google.gwt.resources.ext.SupportsGeneratorResultCaching;
import com.google.gwt.resources.rg.ImageBundleBuilder.Arranger;
import com.google.gwt.resources.rg.ImageBundleBuilder.ImageRect;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.StringSourceWriter;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Builds an image strip for all ImageResources defined within an ClientBundle.
 */
public final class ImageResourceGenerator extends AbstractResourceGenerator
    implements SupportsGeneratorResultCaching {
  /**
   * Represents a file that contains multiple image regions.
   */
  static class BundledImage extends DisplayedImage {
    private static final String MIME_TYPE_IMAGE_PNG = "image/png";
    private final ImageBundleBuilder builder;
    private boolean dirty = false;
    private Map<LocalizedImage, ImageRect> images;
    private Set<LocalizedImage> rtlImages = Sets.create();
    private Map<ImageResourceDeclaration, LocalizedImage> localizedByImageResource;
    private String normalContentsUrlExpression;
    private String rtlContentsUrlExpression;

    public BundledImage() {
      builder = new ImageBundleBuilder();
      images = Maps.create();
      localizedByImageResource = Maps.create();
    }

    public LocalizedImage addImage(TreeLogger logger, ResourceContext context,
        ImageResourceDeclaration image) throws UnableToCompleteException,
        CannotBundleImageException {

      LocalizedImage localized = LocalizedImage.create(logger, context, image);
      localizedByImageResource = Maps.put(localizedByImageResource, image, localized);
      if (images.containsKey(localized)) {
        return localized;
      }

      dirty = true;
      ImageRect rect = null;
      try {
        rect = builder.assimilate(logger, image.get(), localized.getUrl());
        if (context.supportsDataUrls()) {
          // Treat the image as though it were external
          builder.removeMapping(image.get());
          throw new CannotBundleImageException(localized, rect);
        }
        images = Maps.put(images, localized, rect);
      } catch (UnsuitableForStripException e) {
        rect = e.getImageRect();
        throw new CannotBundleImageException(localized, rect);
      } finally {
        assert rect != null : "No ImageRect";
        rect.setHeight(image.getScaleHeight());
        rect.setWidth(image.getScaleWidth());
      }
      return localized;
    }

    public ImageBundleBuilder getImageBundleBuilder() {
      return builder;
    }

    @Override
    public ImageRect getImageRect(ImageResourceDeclaration image) {
      return images.get(localizedByImageResource.get(image));
    }

    @Override
    public void render(TreeLogger logger, ResourceContext context,
        ClientBundleFields fields, RepeatStyle repeatStyle)
        throws UnableToCompleteException {
      if (builder.getImageCount() == 0) {
        // No data
        return;
      }

      if (dirty) {
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
            arranger = new ImageBundleBuilder.IdentityArranger();
            break;
          default:
            logger.log(TreeLogger.ERROR, "Unknown RepeatStyle " + repeatStyle);
            throw new UnableToCompleteException();
        }
        URL normalContents = renderToTempPngFile(logger, builder, arranger);
        normalContentsUrlExpression = context.deploy(normalContents, MIME_TYPE_IMAGE_PNG, false);

        if (!rtlImages.isEmpty()) {
          for (LocalizedImage rtlImage : rtlImages) {
            // Create a transformation to mirror about the Y-axis and translate
            AffineTransform tx = new AffineTransform();
            ImageRect imageRect = images.get(rtlImage);
            tx.setTransform(-1, 0, 0, 1, imageRect.getWidth(), 0);
            imageRect.setTransform(tx);
          }
          URL rtlContents =
              renderToTempPngFile(logger, builder, new ImageBundleBuilder.IdentityArranger());
          assert rtlContents != null;
          rtlContentsUrlExpression = context.deploy(rtlContents, MIME_TYPE_IMAGE_PNG, false);
        }

        dirty = false;
        if (logger.isLoggable(TreeLogger.DEBUG)) {
          logger.log(TreeLogger.DEBUG, "Composited " + builder.getImageCount() + " images");
        }
      }

      JClassType stringType =
          context.getGeneratorContext().getTypeOracle().findType(String.class.getCanonicalName());

      // Create the field that holds the normal contents
      assert normalContentsUrlExpression != null;
      normalContentsFieldName = fields.define(stringType, "bundledImage_"
          + repeatStyle.name(), normalContentsUrlExpression, true, true);

      // Optionally create the field that holds the RTL contents
      if (rtlContentsUrlExpression != null) {
        rtlContentsFieldName = fields.define(stringType, "bundledImage_"
            + repeatStyle.name() + "_rtl", rtlContentsUrlExpression, true, true);
      }
    }

    @Override
    public void setRtlImage(LocalizedImage image) {
      rtlImages = Sets.add(rtlImages, image);
    }
  }

  /**
   * This key is used to determine which DisplayedImage a given set of image
   * bytes should be associated with.
   */
  static class BundleKey extends StringKey {
    private static String key(ImageResourceDeclaration image, boolean isExternal) {
      if (isExternal) {
        return "External: " + image.get();
      }
      if (image.isPreventInlining() || image.getRepeatStyle() == RepeatStyle.Both) {
        return "Unbundled: " + image.get();
      }
      return "Arranged: " + image.getRepeatStyle().toString();
    }

    private final RepeatStyle repeatStyle;

    public BundleKey(ImageResourceDeclaration image, boolean isExternal) {
      super(key(image, isExternal));
      this.repeatStyle = image.getRepeatStyle();
    }

    public RepeatStyle getRepeatStyle() {
      return repeatStyle;
    }

    public boolean isExternal() {
      return get().startsWith("External: ");
    }
  }

  /**
   * This is shared that can be shared across permutations for a given
   * ClientBundle .
   */
  static class CachedState {
    public final Map<BundleKey, BundledImage> bundledImages =
        new LinkedHashMap<BundleKey, BundledImage>();
    public final Map<BundleKey, ExternalImage> externalImages =
        new LinkedHashMap<BundleKey, ExternalImage>();
  }

  /**
   * Associates an ImageRect and a LocalizedImage.
   */
  static class CannotBundleImageException extends Exception {
    private final ImageRect imageRect;
    private final LocalizedImage localized;

    public CannotBundleImageException(LocalizedImage localized, ImageRect imageRect) {
      this.localized = localized;
      this.imageRect = imageRect;
    }

    public ImageRect getImageRect() {
      return imageRect;
    }

    public LocalizedImage getLocalizedImage() {
      return localized;
    }
  }

  /**
   * Represents a file that contains image data.
   */
  abstract static class DisplayedImage {
    protected String normalContentsFieldName;
    protected String rtlContentsFieldName;

    public abstract ImageRect getImageRect(ImageResourceDeclaration image);

    /**
     * Only valid after calling {@link #render}.
     */
    public String getNormalContentsFieldName() {
      return normalContentsFieldName;
    }

    /**
     * Only valid after calling {@link #render}, may be <code>null</code> if
     * there is no RTL version of the image.
     */
    public String getRtlContentsFieldName() {
      return rtlContentsFieldName;
    }

    public abstract void setRtlImage(LocalizedImage image);

    abstract void render(TreeLogger logger, ResourceContext context,
        ClientBundleFields fields, RepeatStyle repeatStyle)
        throws UnableToCompleteException;
  }

  /**
   * Represents a file that contains exactly one image.
   */
  static class ExternalImage extends DisplayedImage {
    private final ImageResourceDeclaration image;
    private boolean isRtl;
    private final LocalizedImage localized;
    private final ImageRect rect;

    /**
     * Create an unbundled image.
     */
    public ExternalImage(ImageResourceDeclaration image, LocalizedImage localized, ImageRect rect) {
      this.image = image;
      this.localized = localized;
      this.rect = rect;
    }

    @Override
    public ImageRect getImageRect(ImageResourceDeclaration image) {
      return this.image.equals(image) ? rect : null;
    }

    @Override
    public void render(TreeLogger logger, ResourceContext context,
        ClientBundleFields fields, RepeatStyle repeatStyle)
        throws UnableToCompleteException {
      JClassType stringType =
          context.getGeneratorContext().getTypeOracle().findType(String.class.getCanonicalName());

      String contentsExpression = context.deploy(
          localized.getUrl(), null, image.isPreventInlining());
      normalContentsFieldName = fields.define(stringType, "externalImage",
          contentsExpression, true, true);

      if (isRtl) {
        // Create a transformation to mirror about the Y-axis and translate
        AffineTransform tx = new AffineTransform();
        tx.setTransform(-1, 0, 0, 1, rect.getWidth(), 0);
        rect.setTransform(tx);

        byte[] rtlData = ImageBundleBuilder.toPng(logger, rect);
        String rtlContentsUrlExpression = context.deploy(image.getName()
            + "_rtl.png", "image/png", rtlData, image.isPreventInlining());
        rtlContentsFieldName = fields.define(stringType, "externalImage_rtl",
            rtlContentsUrlExpression, true, true);
      }
    }

    @Override
    public void setRtlImage(LocalizedImage localized) {
      if (this.localized.equals(localized)) {
        isRtl = true;
      }
    }
  }

  /**
   * This represent how the user described the image in the original Java
   * source. Its identity is based on the ImageResource JMethod.
   */
  static class ImageResourceDeclaration extends StringKey {
    private static String key(JMethod method) {
      return method.getEnclosingType().getQualifiedSourceName() + "." + method.getName();
    }

    private final String name;
    private final JMethod method;
    private final ImageOptions options;

    public ImageResourceDeclaration(JMethod method) {
      super(key(method));
      this.name = method.getName();
      this.method = method;
      this.options = method.getAnnotation(ImageOptions.class);
    }

    public JMethod getMethod() {
      return method;
    }

    public String getName() {
      return name;
    }

    public RepeatStyle getRepeatStyle() {
      return options == null ? RepeatStyle.None : options.repeatStyle();
    }

    public int getScaleHeight() {
      return options == null ? -1 : options.height();
    }

    public int getScaleWidth() {
      return options == null ? -1 : options.width();
    }

    public boolean isFlipRtl() {
      return options == null ? false : options.flipRtl();
    }
    
    public boolean isPreventInlining() {
      return options == null ? false : options.preventInlining();
    }
  }

  /**
   * This represents the particular collections of bits associated with a
   * localized resource that a permutation will use. Its identity is based on
   * the content hash of the resolved data and any transformations that will be
   * applied to the data.
   */
  static class LocalizedImage extends StringKey {
    public static LocalizedImage create(TreeLogger logger,
        ResourceContext context, ImageResourceDeclaration image)
        throws UnableToCompleteException {

      URL[] resources = ResourceGeneratorUtil.findResources(logger, context, image.getMethod());

      if (resources.length != 1) {
        logger.log(TreeLogger.ERROR, "Exactly one image may be specified", null);
        throw new UnableToCompleteException();
      }

      URL resource = resources[0];

      LocalizedImage toReturn = new LocalizedImage(image, resource);
      return toReturn;
    }

    private static String key(ImageResourceDeclaration image, URL url) {
      return Util.computeStrongName(Util.readURLAsBytes(url)) + ":"
          + image.getScaleHeight() + ":" + image.getScaleWidth();
    }

    private final ImageResourceDeclaration image;
    private final URL url;

    public LocalizedImage(LocalizedImage other, URL alternateUrl) {
      this(other.image, alternateUrl);
    }

    private LocalizedImage(ImageResourceDeclaration image, URL url) {
      super(key(image, url));
      this.image = image;
      this.url = url;
    }

    public URL getUrl() {
      return url;
    }
  }

  /**
   * Re-encode an image as a PNG to strip random header data.
   */
  private static URL renderToTempPngFile(TreeLogger logger,
      ImageBundleBuilder builder, Arranger arranger)
      throws UnableToCompleteException {
    try {
      byte[] imageBytes = builder.render(logger, arranger);
      if (imageBytes == null) {
        return null;
      }

      File file = File.createTempFile(ImageResourceGenerator.class.getSimpleName(), ".png");
      file.deleteOnExit();
      Util.writeBytesToFile(logger, file, imageBytes);
      return file.toURI().toURL();
    } catch (IOException ex) {
      logger.log(TreeLogger.ERROR, "Unable to write re-encoded PNG", ex);
      throw new UnableToCompleteException();
    }
  }

  /**
   * This is used to short-circuit the {@link #prepare} method.
   */
  private boolean prepared;
  private CachedState shared;

  @Override
  public String createAssignment(TreeLogger logger, ResourceContext context, JMethod method)
      throws UnableToCompleteException {
    String name = method.getName();

    SourceWriter sw = new StringSourceWriter();
    sw.println("new " + ImageResourcePrototype.class.getName() + "(");
    sw.indent();
    sw.println('"' + name + "\",");

    ImageResourceDeclaration image = new ImageResourceDeclaration(method);
    DisplayedImage bundle = getImage(image);
    ImageRect rect = bundle.getImageRect(image);
    if (rect == null) {
      throw new NullPointerException("No ImageRect ever computed for " + name);
    }

    String[] urlExpressions =
        new String[] {bundle.getNormalContentsFieldName(), bundle.getRtlContentsFieldName()};
    assert urlExpressions[0] != null : "No primary URL expression for " + name;

    if (urlExpressions[1] == null) {
      sw.println(UriUtils.class.getName() + ".fromTrustedString(" + urlExpressions[0] + "),");
    } else {
      sw.println(UriUtils.class.getName() + ".fromTrustedString("
          + "com.google.gwt.i18n.client.LocaleInfo.getCurrentLocale().isRTL() ?"
          + urlExpressions[1] + " : " + urlExpressions[0] + "),");
    }
    sw.println(rect.getLeft() + ", " + rect.getTop() + ", " + rect.getWidth() + ", "
        + rect.getHeight() + ", " + rect.isAnimated() + ", " + rect.isLossy());

    sw.outdent();
    sw.print(")");

    return sw.toString();
  }

  /**
   * We use this as a signal that we have received all image methods and can now
   * create the bundled images.
   */
  @Override
  public void createFields(TreeLogger logger, ResourceContext context, ClientBundleFields fields)
      throws UnableToCompleteException {
    renderImageMap(logger, context, fields, shared.bundledImages);
    renderImageMap(logger, context, fields, shared.externalImages);
  }

  @Override
  public void init(TreeLogger logger, ResourceContext context) {
    String key = createCacheKey(context);
    shared = context.getCachedData(key, CachedState.class);
    prepared = shared != null;
    if (prepared) {
      logger.log(TreeLogger.DEBUG, "Using cached data");
    } else {
      shared = new CachedState();
      context.putCachedData(key, shared);
    }
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
      logger.log(TreeLogger.DEBUG, "ImageResources already prepared");
      return;
    }

    ImageResourceDeclaration image = new ImageResourceDeclaration(method);

    boolean cannotBundle = false;
    DisplayedImage displayed = null;
    LocalizedImage localizedImage;
    ImageRect rect;
    try {
      BundledImage bundledImage = (BundledImage) getImage(image);
      localizedImage = bundledImage.addImage(logger, context, image);
      rect = bundledImage.getImageRect(image);
      displayed = bundledImage;
      if (image.isPreventInlining()) {
        cannotBundle = true;
      }
    } catch (CannotBundleImageException e) {
      cannotBundle = true;
      localizedImage = e.getLocalizedImage();
      rect = e.getImageRect();
    }

    // Store the image externally
    if (cannotBundle) {
      if (rect.isAnimated() || rect.isLossy()) {
        // Don't re-encode
      } else {
        /*
         * Try to re-compress the image, but only use the re-compressed bytes if
         * they actually offer a space-savings.
         */
        try {
          URL contentLocation = localizedImage.getUrl();
          int originalSize = contentLocation.openConnection().getContentLength();

          // Re-encode the data
          URL reencodedContents = reencodeToTempFile(logger, rect);
          int newSize = reencodedContents.openConnection().getContentLength();

          // But only use it if we did a better job on compression
          if (newSize < originalSize) {
            if (logger.isLoggable(TreeLogger.SPAM)) {
              logger.log(TreeLogger.SPAM, "Reencoded image and saved "
                  + (originalSize - newSize) + " bytes");
            }
            localizedImage = new LocalizedImage(localizedImage, reencodedContents);
          }
        } catch (IOException e2) {
          // Non-fatal, but weird
          logger.log(TreeLogger.WARN,
              "Unable to determine before/after size when re-encoding image " + "data", e2);
        }
      }
      ExternalImage externalImage = new ExternalImage(image, localizedImage, rect);
      shared.externalImages.put(new BundleKey(image, true), externalImage);
      displayed = externalImage;
    }

    if (image.isFlipRtl()) {
      displayed.setRtlImage(localizedImage);
    }
  }

  /**
   * Creates a cache key to be used with {@link ResourceContext#putCachedData}.
   * The key is based on the ClientBundle type, support for data URLs, and the
   * current locale.
   */
  private String createCacheKey(ResourceContext context) {
    StringBuilder sb = new StringBuilder();
    sb.append(context.getClientBundleType().getQualifiedSourceName());
    sb.append(":").append(context.supportsDataUrls());
    try {
      String locale = context.getGeneratorContext().getPropertyOracle().getSelectionProperty(
          TreeLogger.NULL, "locale").getCurrentValue();

      // add the locale selection property as a permuation axis for our requirements
      context.getRequirements().addPermutationAxis("locale");

      sb.append(locale);
    } catch (BadPropertyValueException e) {
      // OK, locale isn't defined
    }

    return sb.toString();
  }

  private DisplayedImage getImage(ImageResourceDeclaration image) {
    DisplayedImage toReturn = shared.externalImages.get(new BundleKey(image, true));
    if (toReturn != null) {
      return toReturn;
    }

    BundleKey key = new BundleKey(image, false);
    toReturn = shared.bundledImages.get(key);
    if (toReturn == null) {
      BundledImage bundled = new BundledImage();
      shared.bundledImages.put(key, bundled);
      toReturn = bundled;
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

      File file = File.createTempFile(ImageResourceGenerator.class.getSimpleName(), ".png");
      file.deleteOnExit();
      Util.writeBytesToFile(logger, file, imageBytes);
      return file.toURI().toURL();
    } catch (IOException ex) {
      logger.log(TreeLogger.ERROR, "Unable to write re-encoded PNG", ex);
      throw new UnableToCompleteException();
    }
  }

  private void renderImageMap(TreeLogger logger, ResourceContext context,
      ClientBundleFields fields, Map<BundleKey, ? extends DisplayedImage> map)
      throws UnableToCompleteException {
    for (Map.Entry<BundleKey, ? extends DisplayedImage> entry : map.entrySet()) {
      DisplayedImage bundle = entry.getValue();
      bundle.render(logger, context, fields, entry.getKey().getRepeatStyle());
    }
  }
}
