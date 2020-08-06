/*
 * SpellingWordSource.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text.spelling;

import org.rstudio.core.client.Rectangle;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.spelling.CharClassifier;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.spelling.TokenPredicate;

public interface SpellingWordSource
{
   // words
   Iterable<Range> getWords(TokenPredicate tokenPredicate,
                            CharClassifier charClassifier,
                            Position start,
                            Position end);
   boolean shouldCheckSpelling(Range range);
   String getTextForRange(Range range);
   
   // selection
   void setSelectionRange(Range range);
   void replaceSelection(String code);
   Position getSelectionEnd();
   
   // cursor
   Rectangle getCursorBounds(); 
   Position getCursorPosition();
   void moveCursorNearTop();
}
