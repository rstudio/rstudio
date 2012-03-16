package org.rstudio.studio.client.common.spelling.view;

public interface SpellCheckerTarget
{
   void changeWord(String fromWord,String toWord);
   void changeAllWords(String fromWord,String toWord);
   String getNextWord();
   boolean isEmpty();
   void spellCheckComplete();
}
