/*
 * Copyright 2007 Google Inc.
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

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.imageio.ImageIO;

/**
 * Accumulates state for the bundled image.
 */
class ImageBundleBuilder {

  /**
   * The rectangle at which the original image is placed into the composite
   * image.
   */
  public static class ImageRect {

    public final int height;
    public final BufferedImage image;
    public int left;
    public final int width;

    public ImageRect(BufferedImage image) {
      this.image = image;
      this.width = image.getWidth();
      this.height = image.getHeight();
    }
  }

  /*
   * Only PNG is supported right now. In the future, we may be able to infer the
   * best output type, and get rid of this constant.
   */
  private static final String BUNDLE_FILE_TYPE = "png";

  private final Map imageNameToImageRectMap = new HashMap();

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
    return (ImageRect) imageNameToImageRectMap.get(imageName);
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
      ImageIO.write(bundledImage, BUNDLE_FILE_TYPE, byteOutputStream);
      imageBytes = byteOutputStream.toByteArray();
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR,
          "Unable to generate file name for image bundle file", null);
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

      // Load the image
      BufferedImage image = ImageIO.read(imageUrl);
      if (image == null) {
        logger.log(TreeLogger.ERROR, "Unrecognized image file format", null);
        throw new UnableToCompleteException();
      }

      return new ImageRect(image);

    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to read image resource", null);
      throw new UnableToCompleteException();
    }
  }

  /*
   * This method creates the bundled image through the composition of the other
   * images.
   * 
   * This method could be implemented in a variety of ways. For example, one
   * could use a knapsack algorithm to draw these images in an optimal amount of
   * space.
   * 
   * In this particular implementation, we iterate through the image rectangles
   * in ascending order of associated filename, and draw the rectangles from
   * left to right in a single row.
   * 
   * The most important aspect of drawing the bundled image is that it be drawn
   * in a deterministic way. The drawing of the image should not rely on
   * implementation details of the Generator system which may be subject to
   * change. For example, at the time of this writing, the image names are added
   * to imageNameToImageRectMap based on the alphabetical ordering of their
   * associated methods. This behavior is the result of the oracle returning the
   * list of a type's methods in alphabetical order. However, this behavior is
   * undocumented, and should not be relied on. If this behavior were to change,
   * it would inadvertently affect the generation of bundled images.
   */
  private BufferedImage drawBundledImage() {

    // Impose an ordering on the image rectangles, so that we construct
    // the bundled image in a deterministic way.
    SortedMap sortedImageNameToImageRectMap = new TreeMap();
    sortedImageNameToImageRectMap.putAll(imageNameToImageRectMap);
    Collection orderedImageRects = sortedImageNameToImageRectMap.values();

    // Determine how big the composited image should be by taking the
    // sum of the widths and the max of the heights.
    int nextLeft = 0;
    int maxHeight = 0;
    for (Iterator iter = orderedImageRects.iterator(); iter.hasNext();) {
      ImageRect imageRect = (ImageRect) iter.next();
      imageRect.left = nextLeft;
      nextLeft += imageRect.width;
      if (imageRect.height > maxHeight) {
        maxHeight = imageRect.height;
      }
    }

    // Create the bundled image.
    BufferedImage bundledImage = new BufferedImage(nextLeft, maxHeight,
        BufferedImage.TYPE_INT_ARGB_PRE);
    Graphics2D g2d = bundledImage.createGraphics();

    for (Iterator iter = orderedImageRects.iterator(); iter.hasNext();) {
      ImageRect imageRect = (ImageRect) iter.next();

      // We do not need to pass in an ImageObserver, because we are working
      // with BufferedImages. ImageObservers only need to be used when
      // the image to be drawn is being loaded asynchronously. See
      // http://java.sun.com/docs/books/tutorial/2d/images/drawimage.html
      // for more information.
      g2d.drawImage(imageRect.image, imageRect.left, 0, null);
    }
    g2d.dispose();

    return bundledImage;
  }

  private void putMapping(String imageName, ImageRect rect) {
    imageNameToImageRectMap.put(imageName, rect);
  }
}
