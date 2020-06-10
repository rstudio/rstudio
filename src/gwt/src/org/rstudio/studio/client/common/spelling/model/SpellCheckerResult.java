/*
 * SpellCheckerResult.java
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

package org.rstudio.studio.client.common.spelling.model;

import java.util.ArrayList;
import java.util.List;

public class SpellCheckerResult
{
   public SpellCheckerResult()
   {
      correct_ = new ArrayList<>();
      incorrect_ = new ArrayList<>();
   }
   
   public List<String> getCorrect()
   {
      return correct_;
   }
   public List<String> getIncorrect()
   {
      return incorrect_;
   }
    
   private final List<String> correct_;
   private final List<String> incorrect_;
}
