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
import com.google.gwt.dev.util.Util;
import com.google.gwt.resources.ext.ResourceContext;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

/**
 * Accumulates state for the bundled image.
 */
class ImageBundleBuilder {
  /**
   * Abstracts the process of arranging a number of images into a composite
   * image.
   */
  interface Arranger {
    /**
     * Determine the total area required to store a composite image.
     */
    Size arrangeImages(Collection<HasRect> rects);
  }

  /**
   * Arranges the images to try to decrease the overall area of the resulting
   * bundle. This uses a strategy that is basically Next-Fit Decreasing Height
   * Decreasing Width (NFDHDW). The rectangles to be packed are sorted in
   * decreasing order by height. The tallest rectangle is placed at the far
   * left. We attempt to stack the remaining rectangles on top of one another to
   * construct as many columns as necessary. After finishing each column, we
   * also attempt to do some horizontal packing to fill up the space left due to
   * widths of rectangles differing in the column.
   */
  static class BestFitArranger implements Arranger {
    private static final Comparator<HasRect> decreasingHeightComparator = new Comparator<HasRect>() {
      public int compare(HasRect a, HasRect b) {
        final int c = b.getHeight() - a.getHeight();
        // If we encounter equal heights, use the name to keep things
        // deterministic.
        return (c != 0) ? c : b.getName().compareTo(a.getName());
      }
    };

    private static final Comparator<HasRect> decreasingWidthComparator = new Comparator<HasRect>() {
      public int compare(HasRect a, HasRect b) {
        final int c = b.getWidth() - a.getWidth();
        // If we encounter equal heights, use the name to keep things
        // deterministic.
        return (c != 0) ? c : b.getName().compareTo(a.getName());
      }
    };

    public Size arrangeImages(Collection<HasRect> rects) {
      if (rects.size() == 0) {
        return new Size(0, 0);
      }

      // Create a list of ImageRects ordered by decreasing height used for
      // constructing columns.
      final ArrayList<HasRect> rectsOrderedByHeight = new ArrayList<HasRect>(
          rects);
      Collections.sort(rectsOrderedByHeight, decreasingHeightComparator);

      // Create a list of ImageRects ordered by decreasing width used for
      // packing
      // individual columns.
      final ArrayList<HasRect> rectsOrderedByWidth = new ArrayList<HasRect>(
          rects);
      Collections.sort(rectsOrderedByWidth, decreasingWidthComparator);

      // Place the first, tallest image as the first column.
      final HasRect first = rectsOrderedByHeight.get(0);
      first.setPosition(0, 0);

      // Setup state for laying things cumulatively.
      int curX = first.getWidth();
      final int colH = first.getHeight();

      for (int i = 1, n = rectsOrderedByHeight.size(); i < n; i++) {
        // If this ImageRect has been positioned already, move on.
        if (rectsOrderedByHeight.get(i).hasBeenPositioned()) {
          continue;
        }

        int colW = 0;
        int curY = 0;

        final ArrayList<HasRect> rectsInColumn = new ArrayList<HasRect>();
        for (int j = i; j < n; j++) {
          final HasRect current = rectsOrderedByHeight.get(j);
          // Look for rects that have not been positioned with a small enough
          // height to go in this column.
          if (!current.hasBeenPositioned()
              && (curY + current.getHeight()) <= colH) {

            // Set the horizontal position here, the top field will be set in
            // arrangeColumn after we've collected a full set of ImageRects.
            current.setPosition(curX, 0);
            colW = Math.max(colW, current.getWidth());
            curY += current.getHeight();

            // Keep the ImageRects in this column in decreasing order by width.
            final int pos = Collections.binarySearch(rectsInColumn, current,
                decreasingWidthComparator);
            assert pos < 0;
            rectsInColumn.add(-1 - pos, current);
          }
        }

        // Having selected a set of ImageRects that fill out this column
        // vertical,
        // now we'll scan the remaining ImageRects to try to fit some in the
        // horizontal gaps.
        if (!rectsInColumn.isEmpty()) {
          arrangeColumn(rectsInColumn, rectsOrderedByWidth);
        }

        // We're done with that column, so move the horizontal accumulator by
        // the
        // width of the column we just finished.
        curX += colW;
      }

      return new Size(curX, colH);
    }

    /**
     * Companion method to {@link #arrangeImages()}. This method does a best
     * effort horizontal packing of a column after it was packed vertically.
     * This is the Decreasing Width part of Next-Fit Decreasing Height
     * Decreasing Width. The basic strategy is to sort the remaining rectangles
     * by decreasing width and try to fit them to the left of each of the
     * rectangles we've already picked for this column.
     * 
     * @param rectsInColumn the ImageRects that were already selected for this
     *          column
     * @param remainingRectsOrderedByWidth the sub list of ImageRects that may
     *          not have been positioned yet
     */
    private void arrangeColumn(List<HasRect> rectsInColumn,
        List<HasRect> remainingRectsOrderedByWidth) {
      final HasRect first = rectsInColumn.get(0);

      final int columnWidth = first.getWidth();
      int curY = first.getHeight();

      // Skip this first ImageRect because it is guaranteed to consume the full
      // width of the column.
      for (int i = 1, m = rectsInColumn.size(); i < m; i++) {
        final HasRect r = rectsInColumn.get(i);
        // The ImageRect was previously positioned horizontally, now set the top
        // field.
        r.setPosition(r.getLeft(), curY);
        int curX = r.getWidth();

        // Search for ImageRects that are shorter than the left most ImageRect
        // and
        // narrow enough to fit in the column.
        for (int j = 0, n = remainingRectsOrderedByWidth.size(); j < n; j++) {
          final HasRect current = remainingRectsOrderedByWidth.get(j);
          if (!current.hasBeenPositioned()
              && (curX + current.getWidth()) <= columnWidth
              && (current.getHeight() <= r.getHeight())) {
            current.setPosition(r.getLeft() + curX, r.getTop());
            curX += current.getWidth();
          }
        }

        // Update the vertical accumulator so we'll know where to place the next
        // ImageRect.
        curY += r.getHeight();
      }
    }
  }

  /**
   * A mockable interface to test the image arrangement algorithms.
   */
  interface HasRect {

    int getHeight();

    BufferedImage getImage();

    BufferedImage[] getImages();

    int getLeft();

    String getName();

    int getTop();

    AffineTransform getTransform();

    int getWidth();

    boolean hasBeenPositioned();

    void setPosition(int left, int top);

    AffineTransform transform();
  }

  /**
   * Performs a simple horizontal arrangement of rectangles. Images will be
   * tiled vertically to fill to fill the full height of the image.
   */
  static class HorizontalArranger implements Arranger {
    public Size arrangeImages(Collection<HasRect> rects) {
      int height = 1;
      int width = 0;

      for (HasRect rect : rects) {
        rect.setPosition(width, 0);
        width += rect.getWidth();
        height = lcm(height, rect.getHeight());
      }

      List<HasRect> toAdd = new ArrayList<HasRect>();
      for (HasRect rect : rects) {
        int y = rect.getHeight();
        while (y < height) {
          ImageRect newRect = new ImageRect(rect);
          newRect.setPosition(rect.getLeft(), y);
          y += rect.getHeight();
          toAdd.add(newRect);
        }
      }
      rects.addAll(toAdd);

      return new Size(width, height);
    }
  }

  /**
   * Does not rearrange the rectangles, but simply computes the size of the
   * canvas needed to hold the images in their current positions.
   */
  static class IdentityArranger implements Arranger {
    public Size arrangeImages(Collection<HasRect> rects) {
      int height = 0;
      int width = 0;

      for (HasRect rect : rects) {
        height = Math.max(height, rect.getTop() + rect.getHeight());
        width = Math.max(width, rect.getLeft() + rect.getWidth());
      }

      return new Size(width, height);
    }
  }

  /**
   * The rectangle at which the original image is placed into the composite
   * image.
   */
  static class ImageRect implements HasRect {

    private boolean hasBeenPositioned;
    private final int height, width;
    private final BufferedImage[] images;
    private int left, top;
    private final String name;
    private final AffineTransform transform = new AffineTransform();

    /**
     * Copy constructor.
     */
    public ImageRect(HasRect other) {
      this.name = other.getName();
      this.height = other.getHeight();
      this.width = other.getWidth();
      this.images = other.getImages();
      this.left = other.getLeft();
      this.top = other.getTop();
      setTransform(other.getTransform());
    }

    public ImageRect(String name, BufferedImage image) {
      this.name = name;
      this.images = new BufferedImage[] {image};
      this.width = image.getWidth();
      this.height = image.getHeight();
    }

    public ImageRect(String name, BufferedImage[] images) {
      this.name = name;
      this.images = images;
      this.width = images[0].getWidth();
      this.height = images[0].getHeight();
    }

    public int getHeight() {
      return height;
    }

    public BufferedImage getImage() {
      return images[0];
    }

    public BufferedImage[] getImages() {
      return images;
    }

    public int getLeft() {
      return left;
    }

    public String getName() {
      return name;
    }

    public int getTop() {
      return top;
    }

    public AffineTransform getTransform() {
      return new AffineTransform(transform);
    }

    public int getWidth() {
      return width;
    }

    public boolean hasBeenPositioned() {
      return hasBeenPositioned;
    }

    public boolean isAnimated() {
      return images.length > 1;
    }

    public void setPosition(int left, int top) {
      hasBeenPositioned = true;
      this.left = left;
      this.top = top;
    }

    public void setTransform(AffineTransform transform) {
      this.transform.setTransform(transform);
    }

    public AffineTransform transform() {
      AffineTransform toReturn = new AffineTransform();
      toReturn.translate(left, top);
      toReturn.concatenate(transform);

      assert checkTransform(toReturn);
      return toReturn;
    }

    private boolean checkTransform(AffineTransform tx) {
      double[] in = {0, 0, width, height};
      double[] out = {0, 0, 0, 0};

      tx.transform(in, 0, out, 0, 2);

      assert width == Math.abs(out[0] - out[2]);
      assert height == Math.abs(out[1] - out[3]);
      assert out[0] >= 0;
      assert out[1] >= 0;
      assert out[2] >= 0;
      assert out[3] >= 0;

      return true;
    }
  }

  /**
   * Used to return the size of the resulting image from the method
   * {@link ImageBundleBuilder#arrangeImages()}.
   */
  static class Size {
    private final int width, height;

    Size(int width, int height) {
      this.width = width;
      this.height = height;
    }
  }

  /**
   * Performs a simple vertical arrangement of rectangles. Images will be tiled
   * horizontally to fill the full width of the image.
   */
  static class VerticalArranger implements Arranger {
    public Size arrangeImages(Collection<HasRect> rects) {
      int height = 0;
      int width = 1;

      for (HasRect rect : rects) {
        rect.setPosition(0, height);
        width = lcm(width, rect.getWidth());
        height += rect.getHeight();
      }

      List<HasRect> toAdd = new ArrayList<HasRect>();
      for (HasRect rect : rects) {
        int x = rect.getWidth();
        while (x < width) {
          ImageRect newRect = new ImageRect(rect);
          newRect.setPosition(x, rect.getTop());
          x += rect.getWidth();
          toAdd.add(newRect);
        }
      }
      rects.addAll(toAdd);

      return new Size(width, height);
    }
  }

  /*
   * Only PNG is supported right now. In the future, we may be able to infer the
   * best output type, and get rid of this constant.
   */
  private static final String BUNDLE_FILE_TYPE = "png";
  private static final String BUNDLE_MIME_TYPE = "image/png";
  private static final int IMAGE_MAX_SIZE = Integer.getInteger(
      "gwt.imageResource.maxBundleSize", 256);

  public static byte[] toPng(TreeLogger logger, HasRect rect)
      throws UnableToCompleteException {
    // Create the bundled image.
    BufferedImage bundledImage = new BufferedImage(rect.getWidth(),
        rect.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
    Graphics2D g2d = bundledImage.createGraphics();

    g2d.drawImage(rect.getImage(), rect.transform(), null);
    g2d.dispose();

    byte[] imageBytes;

    try {
      ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
      ImageIO.write(bundledImage, BUNDLE_FILE_TYPE, byteOutputStream);
      imageBytes = byteOutputStream.toByteArray();
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR,
          "Unable to generate file name for image bundle file", null);
      throw new UnableToCompleteException();
    }
    return imageBytes;
  }

  /**
   * Compute the greatest common denominator of two numbers.
   */
  private static int gcd(int a, int b) {
    while (b != 0) {
      int t = b;
      b = a % b;
      a = t;
    }
    return a;
  }

  /**
   * Compute the least common multiple of two numbers. This is used by
   * {@link HorizontalArranger} and {@link VerticalArranger} to determine how
   * large the composite image should be to allow every image to line up when
   * repeated.
   */
  private static int lcm(int a, int b) {
    return b / gcd(a, b) * a;
  }

  private final Map<String, ImageRect> imageNameToImageRectMap = new HashMap<String, ImageRect>();

  /**
   * This map is used to de-duplicate images in generated image strips.
   */
  private final Map<String, String> strongNametoCanonicalImageNameMap = new HashMap<String, String>();

  public ImageBundleBuilder() {
  }

  /**
   * Copy constructor.
   */
  public ImageBundleBuilder(ImageBundleBuilder other) {
    for (Map.Entry<String, ImageRect> entry : other.imageNameToImageRectMap.entrySet()) {
      imageNameToImageRectMap.put(entry.getKey(), new ImageRect(
          entry.getValue()));
    }
  }

  /**
   * Assimilates the image associated with a particular image method into the
   * master composite. If the method names an image that has already been
   * assimilated, the existing image rectangle is reused.
   * 
   * @param logger a hierarchical logger which logs to the hosted console
   * @param imageName the name of an image that can be found on the classpath
   * @param resource the URL from which the image data wil be loaded
   * @throws UnableToCompleteException if the image with name
   *           <code>imageName</code> cannot be added to the master composite
   *           image
   */
  public ImageRect assimilate(TreeLogger logger, String imageName, URL resource)
      throws UnableToCompleteException, UnsuitableForStripException {

    /*
     * Decide whether or not we need to add to the composite image. Either way,
     * we associated it with the rectangle of the specified image as it exists
     * within the composite image. Note that the coordinates of the rectangle
     * aren't computed until the composite is written.
     */
    ImageRect rect = getMapping(imageName);

    if (rect == null) {
      String strongName = Util.computeStrongName(Util.readURLAsBytes(resource));
      if (strongNametoCanonicalImageNameMap.containsKey(strongName)) {
        String previousImageName = strongNametoCanonicalImageNameMap.get(strongName);
        rect = getMapping(previousImageName);
        assert rect != null;
        imageNameToImageRectMap.put(imageName, rect);
      } else {
        // Assimilate the image into the composite.
        rect = addImage(logger, imageName, resource);

        imageNameToImageRectMap.put(imageName, rect);
        strongNametoCanonicalImageNameMap.put(strongName, imageName);
      }
    }
    return rect;
  }

  public int getImageCount() {
    return imageNameToImageRectMap.size();
  }

  public ImageRect getMapping(String imageName) {
    return imageNameToImageRectMap.get(imageName);
  }

  /**
   * Remove an image from the builder.
   */
  public ImageRect removeMapping(String imageName) {
    strongNametoCanonicalImageNameMap.values().remove(imageName);
    return imageNameToImageRectMap.remove(imageName);
  }

  /**
   * Write all images into the output.
   * 
   * @param logger a hierarchical logger which logs to the hosted console
   * @param context the local ResourceContext that is being used to accumulate
   *          output files
   * @param arranger a provider of image layout logic
   * @return a Java expression which will evaluate to the location of the
   *         composite image at runtime
   */
  public String writeBundledImage(TreeLogger logger, ResourceContext context,
      Arranger arranger) throws UnableToCompleteException {

    if (imageNameToImageRectMap.isEmpty()) {
      return null;
    }

    // Create the bundled image from all of the constituent images.
    BufferedImage bundledImage = drawBundledImage(arranger);

    // Write the bundled image into a byte array, so that we can compute
    // its strong name.
    byte[] imageBytes;

    try {
      ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
      ImageIO.write(bundledImage, BUNDLE_FILE_TYPE, byteOutputStream);
      imageBytes = byteOutputStream.toByteArray();
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR,
          "Unable to generate file name for image bundle file", null);
      throw new UnableToCompleteException();
    }

    String bundleFileName = context.deploy(
        context.getClientBundleType().getQualifiedSourceName() + ".cache."
            + BUNDLE_FILE_TYPE, BUNDLE_MIME_TYPE, imageBytes, false);

    return bundleFileName;
  }

  private ImageRect addImage(TreeLogger logger, String imageName, URL imageUrl)
      throws UnableToCompleteException, UnsuitableForStripException {

    logger = logger.branch(TreeLogger.TRACE,
        "Adding image '" + imageName + "'", null);

    BufferedImage image = null;
    // Load the image
    try {
      String path = imageUrl.getPath();
      String suffix = path.substring(path.lastIndexOf('.') + 1);

      /*
       * ImageIO uses an SPI pattern API. We don't care about the particulars of
       * the implementation, so just choose the first ImageReader.
       */
      Iterator<ImageReader> it = ImageIO.getImageReadersBySuffix(suffix);
      if (it.hasNext()) {
        ImageReader reader = it.next();

        reader.setInput(new MemoryCacheImageInputStream(imageUrl.openStream()));

        int numImages = reader.getNumImages(true);
        if (numImages == 0) {
          // Fall through

        } else if (numImages == 1) {
          image = reader.read(0);

        } else {
          // Read all contained images
          BufferedImage[] images = new BufferedImage[numImages];
          for (int i = 0; i < numImages; i++) {
            images[i] = reader.read(i);
          }

          ImageRect rect = new ImageRect(imageName, images);
          throw new UnsuitableForStripException(rect);
        }
      }
    } catch (IllegalArgumentException iex) {
      if (imageName.toLowerCase().endsWith("png")
          && iex.getMessage() != null
          && iex.getStackTrace()[0].getClassName().equals(
              "javax.imageio.ImageTypeSpecifier$Indexed")) {
        logger.log(TreeLogger.ERROR,
            "Unable to read image. The image may not be in valid PNG format. "
                + "This problem may also be due to a bug in versions of the "
                + "JRE prior to 1.6. See "
                + "http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5098176 "
                + "for more information. If this bug is the cause of the "
                + "error, try resaving the image using a different image "
                + "program, or upgrade to a newer JRE.", null);
        throw new UnableToCompleteException();
      } else {
        throw iex;
      }
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to read image resource", e);
      throw new UnableToCompleteException();
    }

    if (image == null) {
      logger.log(TreeLogger.ERROR, "Unrecognized image file format", null);
      throw new UnableToCompleteException();
    }

    ImageRect toReturn = new ImageRect(imageName, image);

    if (toReturn.height > IMAGE_MAX_SIZE || toReturn.width > IMAGE_MAX_SIZE) {
      throw new UnsuitableForStripException(toReturn);
    }

    return toReturn;
  }

  /**
   * This method creates the bundled image through the composition of the other
   * images.
   * 
   * In this particular implementation, we use NFDHDW (see
   * {@link #arrangeImages()}) to get an approximate optimal image packing.
   * 
   * The most important aspect of drawing the bundled image is that it be drawn
   * in a deterministic way. The drawing of the image should not rely on
   * implementation details of the Generator system which may be subject to
   * change.
   */
  private BufferedImage drawBundledImage(Arranger arranger) {

    /*
     * There is no need to impose any order here, because arrangeImages will
     * position the ImageRects in a deterministic fashion, even though we might
     * paint them in a non-deterministic order.
     */
    Collection<HasRect> imageRects = new LinkedList<HasRect>(
        imageNameToImageRectMap.values());

    // Arrange images and determine the size of the resulting bundle.
    Size size = arranger.arrangeImages(imageRects);

    // Create the bundled image.
    BufferedImage bundledImage = new BufferedImage(size.width, size.height,
        BufferedImage.TYPE_INT_ARGB_PRE);
    Graphics2D g2d = bundledImage.createGraphics();

    for (HasRect imageRect : imageRects) {
      g2d.drawImage(imageRect.getImage(), imageRect.transform(), null);
    }
    g2d.dispose();

    return bundledImage;
  }
}
