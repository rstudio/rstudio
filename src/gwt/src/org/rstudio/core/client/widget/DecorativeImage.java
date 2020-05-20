/*
 * DecorativeImage.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.user.client.ui.Image;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.a11y.A11y;

/**
 * An image that is invisible to assistive technology such as screen readers. If an image
 * is important in conveying context, then use a regular Image and setAltText.
 */
public class DecorativeImage extends Image
{
   /**
    * Creates an empty decorative image.
    */
   public DecorativeImage()
   {
      super();
      setDecorative();
   }

   /**
    * Creates a decorative image whose size and content are defined by an ImageResource.
    *
    * @param resource the ImageResource to be displayed
    */
   public DecorativeImage(ImageResource resource)
   {
      super(resource);
      setDecorative();
   }

   /**
    * Creates a decorative image with a specified URL. The load event will be fired once
    * the image at the given URL has been retrieved by the browser.
    *
    * @param url the URL of the image to be displayed
    */
   public DecorativeImage(String url)
   {
     super(url);
     setDecorative(); 
   }

   /**
    * Creates a decorative image with a specified URL. The load event will be fired once
    * the image at the given URL has been retrieved by the browser.
    *
    * @param url the URL of the image to be displayed
    */
   public DecorativeImage(SafeUri url)
   {
      super(url);
      setDecorative();
   }

   /**
    * Creates a clipped decorative image with a specified URL and visibility rectangle. The
    * visibility rectangle is declared relative to the rectangle which
    * encompasses the entire image, which has an upper-left vertex of (0,0). The
    * load event will be fired immediately after the object has been constructed
    * (i.e. potentially before the image has been loaded in the browser). Since
    * the width and height are specified explicitly by the user, this behavior
    * will not cause problems with retrieving the width and height of a clipped
    * image in a load event handler.
    *
    * @param url the URL of the image to be displayed
    * @param left the horizontal co-ordinate of the upper-left vertex of the
    *          visibility rectangle
    * @param top the vertical co-ordinate of the upper-left vertex of the
    *          visibility rectangle
    * @param width the width of the visibility rectangle
    * @param height the height of the visibility rectangle
    */
   public DecorativeImage(String url, int left, int top, int width, int height)
   {
      super(url, left, top, width, height);
      setDecorative();
   }

   /**
    * Creates a clipped decorative image with a specified URL and visibility rectangle. The
    * visibility rectangle is declared relative to the rectangle which
    * encompasses the entire image, which has an upper-left vertex of (0,0). The
    * load event will be fired immediately after the object has been constructed
    * (i.e. potentially before the image has been loaded in the browser). Since
    * the width and height are specified explicitly by the user, this behavior
    * will not cause problems with retrieving the width and height of a clipped
    * image in a load event handler.
    *
    * @param url the URL of the image to be displayed
    * @param left the horizontal co-ordinate of the upper-left vertex of the
    *          visibility rectangle
    * @param top the vertical co-ordinate of the upper-left vertex of the
    *          visibility rectangle
    * @param width the width of the visibility rectangle
    * @param height the height of the visibility rectangle
    */
   public DecorativeImage(SafeUri url, int left, int top, int width, int height)
   {
      super(url, left, top, width, height);
      setDecorative();
   }

   /**
    * This constructor may be used by subclasses to explicitly use an existing
    * element. This element must be an &lt;img&gt; element.
    *
    * @param element the element to be used
    */
   protected DecorativeImage(Element element)
   {
      super(element);
      setDecorative();
   }

   private void setDecorative()
   {
      A11y.setDecorativeImage(getElement());

      // aria-hidden shouldn't be needed, but have seen macOS VoiceOver
      // putting screen-reader cursor onto the decorative image (and reading nothing) in
      // some situations, and this prevents it
      A11y.setARIAHidden(getElement());
   }

   /**
    * Prevent setting altText (defeats purpose of being decorative image)
    * @param altText
    */
   public void setAltText(String altText)
   {
      Debug.logWarning("Trying to set alt text on a decorative image");
   }
}
