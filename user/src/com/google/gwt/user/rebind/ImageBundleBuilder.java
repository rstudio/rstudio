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
package com.google.gwt.user.rebind;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.Util;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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

  private final Map imageNameToImageRectMap = new HashMap();
  private final MessageDigest md5;
  private final List orderedImageRects = new ArrayList();

  public ImageBundleBuilder() {
    try {
      md5 = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Error initializing MD5", e);
    }
  }

  /**
   * Assimilates the image associated with a particular image method into the
   * master composite. If the method names an image that has already been
   * assimilated, the existing image rectangle is reused.
   * 
   * @param logger a hierarchical logger which logs to the hosted console
   * @param imageName the name of an image that can be found on the classpath
   * @throws UnableToCompleteException if the image with name
   *         <code>imageName</code> cannot be added to the master composite image
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
      g2d.drawImage(imageRect.image, imageRect.left, 0, null);
    }
    g2d.dispose();

    // Compute the strong name as the hex version of the hash.
    byte[] hash = md5.digest();
    char[] strongName = new char[2 * hash.length];
    int j = 0;
    for (int i = 0; i < hash.length; i++) {
      strongName[j++] = Util.HEX_CHARS[(hash[i] & 0xF0) >> 4];
      strongName[j++] = Util.HEX_CHARS[hash[i] & 0x0F];
    }

    // Only PNG is supported right now, but still we introduce a variable to
    // anticipate an update when the best output file type is inferred.
    String bundleFileType = "png";

    // Compute the file name. The '.cache' part indicates that it can be
    // permanently cached.
    String bundleFileName = new String(strongName) + ".cache." + bundleFileType;

    OutputStream outStream = context.tryCreateResource(logger, bundleFileName);

    if (outStream != null) {
      try {
        // Write the image bytes to the pending stream.
        if (!ImageIO.write(bundledImage, bundleFileType, outStream)) {
          logger.log(TreeLogger.ERROR, "Unsupported output file type", null);
          throw new UnableToCompleteException();
        }

        // Commit the stream.
        context.commitResource(logger, outStream);

      } catch (IOException e) {
        logger.log(TreeLogger.ERROR, "Failed while writing", e);
        throw new UnableToCompleteException();
      }
    } else {
      logger.log(
          TreeLogger.TRACE,
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
    BufferedImage image = null;
    try {
      // Could turn this lookup logic into an externally-supplied policy for
      // increased generality.
      URL imageUrl = getClass().getClassLoader().getResource(imageName);
      if (imageUrl == null) {
        // This should never happen, because this check is done right after
        // the image name is retrieved from the metadata or the method name.
        // If there is a failure in obtaining the resource, it will happen
        // before this point.
        logger.log(
            TreeLogger.ERROR,
            "Resource not found on classpath (is the name specified as Class.getResource() would expect?)",
            null);
        throw new UnableToCompleteException();
      }

      // Assimilate this file's bytes into the MD5.
      InputStream is = imageUrl.openStream();
      BufferedInputStream bis = new BufferedInputStream(is);
      byte imgByte;
      while ((imgByte = (byte) bis.read()) != -1) {
        md5.update(imgByte);
      }
      is.close();

      // Load the image from the URL instead of the stream (with the assumption
      // that having the URL provides a tiny bit more context to the parser).
      image = ImageIO.read(imageUrl);
      if (image == null) {
        logger.log(TreeLogger.ERROR, "Unrecognized image file format", null);
        throw new UnableToCompleteException();
      }

    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to read image resource", null);
      throw new UnableToCompleteException();
    }

    ImageRect imageRect = new ImageRect(image);
    orderedImageRects.add(imageRect);
    return imageRect;
  }

  private void putMapping(String imageName, ImageRect rect) {
    imageNameToImageRectMap.put(imageName, rect);
  }

}
