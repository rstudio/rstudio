/*
 * VisualModeSpellChecker.java
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


package org.rstudio.studio.client.workbench.views.source.editors.text.visualmode;

import java.util.Iterator;

import org.rstudio.core.client.Rectangle;
import org.rstudio.studio.client.panmirror.spelling.PanmirrorAnchor;
import org.rstudio.studio.client.panmirror.spelling.PanmirrorRect;
import org.rstudio.studio.client.panmirror.spelling.PanmirrorSpellingDoc;
import org.rstudio.studio.client.panmirror.spelling.PanmirrorWordRange;
import org.rstudio.studio.client.panmirror.spelling.PanmirrorWordSource;
import org.rstudio.studio.client.workbench.views.source.editors.text.spelling.SpellingContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.spelling.SpellingDoc;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;


public class VisualModeSpelling extends SpellingContext
{
   public VisualModeSpelling(DocUpdateSentinel docUpdateSentinel)
   {
     super(docUpdateSentinel);  
   }
    
   public void checkSpelling(PanmirrorSpellingDoc doc)   
   {
      checkSpelling(new SpellingDoc() {

         @Override
         public Iterable<WordRange> getWordSource(int start, Integer end)
         {
            return new Iterable<WordRange>() {

               @Override
               public Iterator<WordRange> iterator()
               {
                  PanmirrorWordSource source = doc.getWordSource(start, end);
                  return new Iterator<WordRange>() {

                     @Override
                     public boolean hasNext()
                     {
                        return source.hasNext();
                     }

                     @Override
                     public WordRange next()
                     {
                        PanmirrorWordRange range = source.next();
                        return new WordRange(range.start, range.end);
                     } 
                  };
               }
            };
           
         }

         @Override
         public Anchor createAnchor(int position)
         {
            PanmirrorAnchor anchor = doc.createAnchor(position);
            return new Anchor() {
               @Override
               public int getPosition()
               {
                  return anchor.getPosition();
               }
            };
         }

         @Override
         public boolean shouldCheck(WordRange wordRange)
         {
            return doc.shouldCheck(toRange(wordRange));
         }

         @Override
         public void setSelection(WordRange wordRange)
         {
            doc.setSelection(toRange(wordRange));
         }

         @Override
         public String getText(WordRange wordRange)
         {
            return doc.getText(toRange(wordRange));
         }

         @Override
         public int getCursorPosition()
         {
            return doc.getCursorPosition();
         }

         @Override
         public void replaceSelection(String text)
         {
            doc.replaceSelection(text);
         }

         @Override
         public int getSelectionStart()
         {
            return doc.getSelectionStart();
         }

         @Override
         public int getSelectionEnd()
         {
            return doc.getSelectionEnd();
         }

         @Override
         public Rectangle getCursorBounds()
         {
            PanmirrorRect rect = doc.getCursorBounds();
            return new Rectangle(rect.x, rect.y, rect.width, rect.height);
         }

         @Override
         public void moveCursorNearTop()
         {
            doc.moveCursorNearTop();
         }
         
         private PanmirrorWordRange toRange(WordRange wordRange)
         {
            PanmirrorWordRange range = new PanmirrorWordRange();
            range.start = wordRange.start;
            range.end = wordRange.end;
            return range;
         }
         
      });
   }
   

   @Override
   public void invalidateAllWords()
   {
      
   }

   @Override
   public void invalidateMisspelledWords()
   {
      
   }

   @Override
   public void invalidateWord(String word)
   {
      
      
   }


   
}


