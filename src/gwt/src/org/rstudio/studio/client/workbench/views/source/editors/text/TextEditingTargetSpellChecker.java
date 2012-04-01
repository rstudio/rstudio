package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;
import java.util.Iterator;

import org.rstudio.core.client.CsvReader;
import org.rstudio.core.client.CsvWriter;
import org.rstudio.core.client.widget.NullProgressIndicator;
import org.rstudio.studio.client.common.spelling.SpellingDocument;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

public class TextEditingTargetSpellChecker implements SpellingDocument
{
   public TextEditingTargetSpellChecker(DocDisplay docDisplay,
                                        DocUpdateSentinel docUpdateSentinel)
   {
      docDisplay_ = docDisplay;
      docUpdateSentinel_ = docUpdateSentinel;
   }
   
   @Override
   public ArrayList<String> readDictionary()
   {
      ArrayList<String> ignoredWords = new ArrayList<String>();
      String ignored = docUpdateSentinel_.getProperty(IGNORED_WORDS);
      if (ignored != null)
      {
         Iterator<String[]> iterator = new CsvReader(ignored).iterator();
         if (iterator.hasNext())
         {
            String[] words = iterator.next();
            for (String word : words)
               ignoredWords.add(word);
         }
      }
      return ignoredWords;
   }
   
   @Override
   public void writeDictionary(ArrayList<String> ignoredWords)
   {
      CsvWriter csvWriter = new CsvWriter();
      for (String ignored : ignoredWords)
         csvWriter.writeValue(ignored);
      csvWriter.endLine();
      docUpdateSentinel_.setProperty(IGNORED_WORDS, 
                                     csvWriter.getValue(), 
                                     new NullProgressIndicator());
   }
   
   private final static String IGNORED_WORDS = "ignored_words";  
   
   
   @SuppressWarnings("unused")
   private final DocDisplay docDisplay_;
   private final DocUpdateSentinel docUpdateSentinel_;
}
