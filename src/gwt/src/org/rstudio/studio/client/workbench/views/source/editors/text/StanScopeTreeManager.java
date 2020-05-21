/*
 * StanScopeTreeManager.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;

public class StanScopeTreeManager extends ScopeTreeManager
{
   @Override
   public void onToken(Token token,
                       Position position,
                       ScopeManager manager)
   {
      if (token.hasType("comment.line"))
      {
         String line = docDisplay_.getLine(position.getRow());
         Match match = RE_SECTION.match(line, 0);
         if (match == null)
            return;
         
         String label = match.getGroup(1).trim();
         manager.onSectionStart(label, position);
      }
      
      else if (token.valueEquals("{"))
      {
         String line = docDisplay_.getLine(position.getRow());
         for (String block : STAN_BLOCKS)
         {
            if (line.startsWith(block))
            {
               String label = StringUtil.capitalizeAllWords(block);
               Position blockPosition = Position.create(position.getRow(), 0);
               manager.onSectionStart(label, blockPosition);
               return;
            }
         }
         
         manager.onScopeStart(position);
      }
      
      else if (token.valueEquals("}"))
      {
         Scope scope = manager.getScopeAt(position);
         Position endPosition = Position.create(position.getRow(), position.getColumn() + 1);
         if (scope.isSection())
            manager.onSectionEnd(endPosition);
         else
            manager.onScopeEnd(endPosition);
      }
   }
   
   public StanScopeTreeManager(DocDisplay docDisplay)
   {
      super(docDisplay);
   }
   
   private static final Pattern RE_SECTION = Pattern.create("^\\s*//(.*)(?:====|----)\\s*", "");
   
   private static final String[] STAN_BLOCKS = new String[] {
         "functions",
         "data", "transformed data",
         "parameters", "transformed parameters",
         "model",
         "generated quantities"
   };
}
