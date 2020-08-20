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
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.panmirror.spelling.PanmirrorAnchor;
import org.rstudio.studio.client.panmirror.spelling.PanmirrorRect;
import org.rstudio.studio.client.panmirror.spelling.PanmirrorSpellingDoc;
import org.rstudio.studio.client.panmirror.spelling.PanmirrorWordRange;
import org.rstudio.studio.client.panmirror.spelling.PanmirrorWordSource;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUISpelling;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.spelling.CharClassifier;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.spelling.CharClassifier.CharClass;
import org.rstudio.studio.client.workbench.views.source.editors.text.spelling.SpellingContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.spelling.SpellingDoc;
import org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.events.VisualModeSpellingAddToDictionaryEvent;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.inject.Inject;

import elemental2.core.JsArray;


public class VisualModeSpelling extends SpellingContext
{
   public interface Context
   {
      void invalidateAllWords();
      void invalidateWord(String word);
   }
   
   public VisualModeSpelling(DocUpdateSentinel docUpdateSentinel,
                             DocDisplay docDisplay,
                             Context context)
   {
     super(docUpdateSentinel);  
     RStudioGinjector.INSTANCE.injectMembers(this);
     docDisplay_ = docDisplay;
     context_ = context;
   }
   
   @Inject
   void initialize(EventBus eventBus)
   {
      eventBus_ = eventBus;
   }
    
   public void checkSpelling(PanmirrorSpellingDoc doc)   
   {
      checkSpelling(new SpellingDoc() {

         @Override
         public Iterable<WordRange> getWords(int start, int end)
         {
            return new Iterable<WordRange>() {

               @Override
               public Iterator<WordRange> iterator()
               {
                  PanmirrorWordSource source = doc.getWords(start, end);
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
         
         @Override
         public void dispose()
         {
            doc.dispose();
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
   
   public PanmirrorUISpelling uiSpelling()
   {
      CharClassifier classifier = docDisplay_.getFileType().getCharPredicate();
      
      PanmirrorUISpelling uiSpelling = new PanmirrorUISpelling();
      
      uiSpelling.realtimeEnabled = () -> {
         return typo().realtimeSpellcheckEnabled(); 
      };
      
      uiSpelling.checkWord = (word) -> {
         if (typo().shouldCheckWord(word))
            return typo().checkSpelling(word);
         else
            return true;
      };
      
      uiSpelling.suggestionList = (word) -> {
        String[] suggestions = typo().suggestionList(word);
        return new JsArray<String>(suggestions);
      };
      
      uiSpelling.isWordIgnored = (word) -> {
        return typo().isIgnoredWord(word); 
      };
      
      uiSpelling.ignoreWord = (word) -> {
         typo().addIgnoredWord(word);
      };
      
      uiSpelling.unignoreWord = (word) -> {
         typo().removeIgnoredWord(word);
      };
      
      uiSpelling.addToDictionary = (word) -> {
         typo().addToUserDictionary(word);
      };
      
      uiSpelling.breakWords = (String text) -> {
      
        
         JsArray<PanmirrorWordRange> words = new JsArray<PanmirrorWordRange>();
         
         int pos = 0;
         while (pos < text.length()) 
         {
            // advance pos until we get past non-word characters
            while (pos < text.length() && classifier.classify(text.charAt(pos)) != CharClass.Word)
            {
               pos++;
            }
            
            // break out of the loop if we got to the end
            if (pos == text.length())
               break;
            
            // set start of word
            int wordStart = pos++;
            
            // consume until a non-word is encourted
            while (pos < text.length() && classifier.classify(text.charAt(pos)) != CharClass.NonWord)
            {
               pos++;
            }
            
            // back over boundary (e.g. apostrophie) characters
            while (classifier.classify(text.charAt(pos - 1)) == CharClass.Boundary)
            {
               pos--;
            }
            
            // add word
            PanmirrorWordRange word = new PanmirrorWordRange();
            word.start = wordStart;
            word.end = pos;
            words.push(word);
         }
                  
         return words;
         
      };
      
      uiSpelling.classifyCharacter = (ch) -> {
         switch(classifier.classify(ch)) {
         case Word:
            return 0;
         case Boundary:
            return 1;
         case NonWord:
         default:
            return 2;
         }
      };
      
      return uiSpelling;
   }
   

   @Override
   public void invalidateAllWords()
   {
      context_.invalidateAllWords();
   }

   @Override
   public void invalidateWord(String word, boolean userDictionary)
   {
      context_.invalidateWord(word);
      
      if (userDictionary)
         eventBus_.fireEvent(new VisualModeSpellingAddToDictionaryEvent(word));
   }
   
   private final DocDisplay docDisplay_;
   private final Context context_;
   private EventBus eventBus_;
}


