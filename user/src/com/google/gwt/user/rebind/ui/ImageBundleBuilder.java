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

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Accumulates state for the bundled image.
 */
class ImageBundleBuilder {
  /**
   * The rectangle at which the original image is placed into the composite
   * image.
   */
  public static class ImageRect implements HasRect {

    private final String name;
    private final int height, width;
    private final BufferedImage image;
    private int left, top;

    private boolean hasBeenPositioned;

    public ImageRect(String name, BufferedImage image) {
      this.name = name;
      this.image = image;
      this.width = image.getWidth();
      this.height = image.getHeight();
    }

    public int getHeight() {
      return height;
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

    public int getWidth() {
      return width;
    }

    public boolean hasBeenPositioned() {
      return hasBeenPositioned;
    }

    public void setPosition(int left, int top) {
      hasBeenPositioned = true;
      this.left = left;
      this.top = top;
    }
  }

  /**
   * A mockable interface to test the image arrangement algorithms.
   */
  interface HasRect {

    String getName();

    int getHeight();

    int getLeft();

    int getTop();

    int getWidth();

    boolean hasBeenPositioned();

    void setPosition(int left, int top);
  }

  /**
   * Used to return the size of the resulting image from the method
   * {@link ImageBundleBuilder#arrangeImages()}.
   */
  private static class Size {
    private final int width, height;

    Size(int width, int height) {
      this.width = width;
      this.height = height;
    }
  }

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

  /*
   * Only PNG is supported right now. In the future, we may be able to infer the
   * best output type, and get rid of this constant.
   */
  private static final String BUNDLE_FILE_TYPE = "png";

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
  static Size arrangeImages(Collection<? extends HasRect> rects) {
    if (rects.size() == 0) {
      return new Size(0, 0);
    }

    // Create a list of ImageRects ordered by decreasing height used for
    // constructing columns.
    final ArrayList<HasRect> rectsOrderedByHeight = new ArrayList<HasRect>(
        rects);
    Collections.sort(rectsOrderedByHeight, decreasingHeightComparator);

    // Create a list of ImageRects ordered by decreasing width used for packing
    // individual columns.
    final ArrayList<HasRect> rectsOrderedByWidth = new ArrayList<HasRect>(rects);
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

      // Having selected a set of ImageRects that fill out this column vertical,
      // now we'll scan the remaining ImageRects to try to fit some in the
      // horizontal gaps.
      if (!rectsInColumn.isEmpty()) {
        arrangeColumn(rectsInColumn, rectsOrderedByWidth);
      }

      // We're done with that column, so move the horizontal accumulator by the
      // width of the column we just finished.
      curX += colW;
    }

    return new Size(curX, colH);
  }

  /**
   * Companion method to {@link #arrangeImages()}. This method does a best
   * effort horizontal packing of a column after it was packed vertically. This
   * is the Decreasing Width part of Next-Fit Decreasing Height Decreasing
   * Width. The basic strategy is to sort the remaining rectangles by decreasing
   * width and try to fit them to the left of each of the rectangles we've
   * already picked for this column.
   *
   * @param rectsInColumn the ImageRects that were already selected for this
   *          column
   * @param remainingRectsOrderedByWidth the sub list of ImageRects that may not
   *          have been positioned yet
   */
  private static void arrangeColumn(List<HasRect> rectsInColumn,
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

      // Search for ImageRects that are shorter than the left most ImageRect and
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

  private final Map<String, ImageRect> imageNameToImageRectMap = new HashMap<String, ImageRect>();

  /**
   * Assimilates the image associated with a particular image method into the
   * master composite. If the method names an image that has already been
   * assimilated, the existing image rectangle is reused.
   *
   * @param logger a hierarchical logger which logs to the hosted console
   * @param imageName the name of an image that can be found on the classpath
   * @throws UnableToCompleteException if the image with name
   *           <code>imageName</code> cannot be added to the master composite
   *           image
   */
  public void assimilate(TreeLogger logger, String imageName)
      throws UnableToCompleteException {

    /*
     * Decide whether or not we need to add to the composite image. Either way,
     * we associated it with the rectangle of the specified image as it exists
     * within the composite image. Note that the coordinates of the rectangle
     * aren't computed until the composite is written.
     */
    ImageRect rect = getMapping(imageName);
    if (rect == null) {
      // Assimilate the image into the composite.
      rect = addImage(logger, imageName);

      // Map the URL to its image so that even if the same URL is used more than
      // once, we only include the referenced image once in the bundled image.
      putMapping(imageName, rect);
    }
  }

  public ImageRect getMapping(String imageName) {
    return imageNameToImageRectMap.get(imageName);
  }

  public String writeBundledImage(TreeLogger logger, GeneratorContext context)
      throws UnableToCompleteException {

    // Create the bundled image from all of the constituent images.
    BufferedImage bundledImage = drawBundledImage();

    // Write the bundled image into a byte array, so that we can compute
    // its strong name.
    byte[] imageBytes;

    try {
      ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
      boolean writerAvailable = ImageIO.write(bundledImage, BUNDLE_FILE_TYPE,
          byteOutputStream);
      if (!writerAvailable) {
        logger.log(TreeLogger.ERROR, "No " + BUNDLE_FILE_TYPE
            + " writer available");
        throw new UnableToCompleteException();
      }
      imageBytes = byteOutputStream.toByteArray();
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR,
          "An error occurred while trying to write the image bundle.", e);
      throw new UnableToCompleteException();
    }

    // Compute the file name. The strong name is generated from the bytes of
    // the bundled image. The '.cache' part indicates that it can be
    // permanently cached.
    String bundleFileName = Util.computeStrongName(imageBytes) + ".cache."
        + BUNDLE_FILE_TYPE;

    // Try and write the file to disk. If a file with bundleFileName already
    // exists, then the file will not be written.
    OutputStream outStream = context.tryCreateResource(logger, bundleFileName);

    if (outStream != null) {
      try {
        // Write the image bytes from the byte array to the pending stream.
        outStream.write(imageBytes);

        // Commit the stream.
        context.commitResource(logger, outStream);

      } catch (IOException e) {
        logger.log(TreeLogger.ERROR, "Failed while writing", e);
        throw new UnableToCompleteException();
      }
    } else {
      logger.log(TreeLogger.TRACE,
          "Generated image bundle file already exists; no need to rewrite it.",
          null);
    }

    return bundleFileName;
  }

  private ImageRect addImage(TreeLogger logger, String imageName)
      throws UnableToCompleteException {

    logger = logger.branch(TreeLogger.TRACE,
        "Adding image '" + imageName + "'", null);

    // Fetch the image.
    try {
      // Could turn this lookup logic into an externally-supplied policy for
      // increased generality.
      URL imageUrl = getClass().getClassLoader().getResource(imageName);
      if (imageUrl == null) {
        // This should never happen, because this check is done right after
        // the image name is retrieved from the metadata or the method name.
        // If there is a failure in obtaining the resource, it will happen
        // before this point.
        logger.log(TreeLogger.ERROR,
            "Resource not found on classpath (is the name specified as "
                + "Class.getResource() would expect?)", null);
        throw new UnableToCompleteException();
      }

      BufferedImage image;
      // Load the image
      try {
        image = ImageIO.read(imageUrl);
      } catch (IllegalArgumentException iex) {
        if (imageName.toLowerCase().endsWith("png")
            && iex.getMessage() != null
            && iex.getStackTrace()[0].getClassName().equals(
                "javax.imageio.ImageTypeSpecifier$Indexed")) {
          logger.log(
              TreeLogger.ERROR,
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
      }

      if (image == null) {
        logger.log(TreeLogger.ERROR, "Unrecognized image file format", null);
        throw new UnableToCompleteException();
      }

      return new ImageRect(imageName, image);

    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to read image resource", null);
      throw new UnableToCompleteException();
    }
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
  private BufferedImage drawBundledImage() {

    // There is no need to impose any order here, because arrangeImages
    // will position the ImageRects in a deterministic fashion, even though
    // we might paint them in a non-deterministic order.
    Collection<ImageRect> imageRects = imageNameToImageRectMap.values();

    // Arrange images and determine the size of the resulting bundle.
    final Size size = arrangeImages(imageRects);

    // Create the bundled image.
    BufferedImage bundledImage = new BufferedImage(size.width, size.height,
        BufferedImage.TYPE_INT_ARGB_PRE);
    SpeedTracerLogger.Event createGraphicsEvent =
      SpeedTracerLogger.start(CompilerEventType.GRAPHICS_INIT,
          "java.awt.headless", System.getProperty("java.awt.headless"));
    Graphics2D g2d = bundledImage.createGraphics();
    createGraphicsEvent.end();

    for (ImageRect imageRect : imageRects) {

      // We do not need to pass in an ImageObserver, because we are working
      // with BufferedImages. ImageObservers only need to be used when
      // the image to be drawn is being loaded asynchronously. See
      // http://java.sun.com/docs/books/tutorial/2d/images/drawimage.html
      // for more information.
      g2d.drawImage(imageRect.image, imageRect.left, imageRect.top, null);
    }
    g2d.dispose();

    return bundledImage;
  }

  private void putMapping(String imageName, ImageRect rect) {
    imageNameToImageRectMap.put(imageName, rect);
  }
}
