/*
 * PresentationEditorSync.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.common.presentation2;

import org.rstudio.studio.client.common.presentation2.model.PresentationEditorLocation;
import org.rstudio.studio.client.common.presentation2.model.PresentationEditorLocationItem;
import org.rstudio.studio.client.common.presentation2.model.PresentationEditorSlide;

import com.google.gwt.core.client.JsArray;


// (1) PresentationEditorLocation includes a "auto_slide_level" detected as per user guide:
//      By default, the slide level is the highest heading level in the hierarchy that is 
//      followed immediately by content, and not another heading, somewhere in the document. 
// (2) Explicit "slide-level" still detected from output and passed in here
// (3) Use the above two to always know the slide level 
// (4) When cleaving up into slides the following create slide breaks:
//       - slide-level
//       - slide-level - 1
//       - horizontal rule


public class PresentationEditorSync
{
   
   // compute the preview slide index from the current editor state and pandoc slide level
   public static int slideIndexForLocation(PresentationEditorLocation location, int slideLevel)
   {
      // determine the slide level (either the arg, which is explicitly from the user, or 
      // from the location, which we auto-detected with the same logic as pandoc)
      if (slideLevel == -1)
         slideLevel = location.getAutoSlideLevel();
      
      // break into slides
      JsArray<PresentationEditorSlide> slides = asSlides(location, slideLevel, true);
      
      // which slide is the cursor on
      for (int i=0; i<slides.length(); i++)
      {
         PresentationEditorSlide slide = slides.get(i);
         for (int j=0; j<slide.getItems().length(); j++)
         {
            if (slide.getItems().get(j).getType().equals(PresentationEditorLocationItem.CURSOR))
               return i;
         }
      }
      
      // default to first slide
      return 0;
   }

   
   // re-compute the editor state with the cursor location implied by the passed slide index
   public static PresentationEditorLocation locationForSlideIndex(
      int slideIndex, PresentationEditorLocation location, int slideLevel)
   {
      
      // determine the slide level (either the arg, which is explicitly from the user, or 
      // from the location, which we auto-detected with the same logic as pandoc)
      if (slideLevel == -1)
         slideLevel = location.getAutoSlideLevel();
      
      // break into slides
      JsArray<PresentationEditorSlide> slides = asSlides(location, slideLevel, false);
    
      // insert the cursor in the requisite slide
      if (slideIndex < slides.length())
      {
         PresentationEditorSlide slide = slides.get(slideIndex);
         slide.getItems().unshift(
           PresentationEditorLocationItem.cursor(slide.getItems().get(0).getRow())
         );
      }
      
      // unroll the slides into a new location
      JsArray<PresentationEditorLocationItem> items = JsArray.createArray().cast();
      for (int i=0; i<slides.length(); i++)
      {
         PresentationEditorSlide slide = slides.get(i);
         for (int j=0; j<slide.getItems().length(); j++)
         {
            items.push(slide.getItems().get(j));
         }
      }
      
      return PresentationEditorLocation.create(items, location.getAutoSlideLevel());
   }
   
   
   private static JsArray<PresentationEditorSlide> asSlides(PresentationEditorLocation location, 
                                                            int slideLevel,
                                                            boolean includeCursor)
   {
      JsArray<PresentationEditorSlide> slides = JsArray.createArray().cast();
      
      JsArray<PresentationEditorLocationItem> items = location.getItems();
      
      for (int i=0; i<items.length(); i++)
      {
         PresentationEditorLocationItem item = items.get(i);
         boolean isSlideBreak = (slides.length() == 0) ||
                                item.getType().equals(PresentationEditorLocationItem.TITLE) ||
                                item.getType().equals(PresentationEditorLocationItem.HR) ||
                                (item.getType().equals(PresentationEditorLocationItem.HEADING) &&
                                 (item.getLevel() <= slideLevel));
         if (isSlideBreak)
         {
            PresentationEditorSlide slide = PresentationEditorSlide.create(JsArray.createArray().cast());
            slide.getItems().push(item);
            slides.push(slide);
         }
         else if (includeCursor || !item.getType().equals(PresentationEditorLocationItem.CURSOR))
         {
            slides.get(slides.length()-1).getItems().push(item);
         }
         
      }
      
      
      return slides;
   }
   
}
