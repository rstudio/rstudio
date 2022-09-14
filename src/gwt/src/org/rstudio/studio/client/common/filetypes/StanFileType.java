/*
 * StanFileType.java
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

package org.rstudio.studio.client.common.filetypes;

import java.util.HashSet;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.StudioClientCommonConstants;
import org.rstudio.studio.client.common.reditor.EditorLanguage;
import org.rstudio.studio.client.workbench.commands.Commands;

public class StanFileType extends PreviewableFromRFileType
{
   public StanFileType()
   {
      super("stan", "Stan", EditorLanguage.LANG_STAN, ".stan",
            new ImageResource2x(FileIconResources.INSTANCE.iconStan2x()), "rstan:::rstudio_stanc",
            true);
   }
   
   public String getPreviewButtonText()
   {
      return constants_.checkPreviewButtonText();
   }
   
   @Override
   public boolean getWordWrap()
   {
      return RStudioGinjector.INSTANCE.getUserPrefs().softWrapRFiles().getValue();
   }
   
   @Override
   public HashSet<AppCommand> getSupportedCommands(Commands commands)
   {
      HashSet<AppCommand> result = super.getSupportedCommands(commands);
      result.add(commands.commentUncomment());
      result.add(commands.reindent());
      return result;
   }
   private static final StudioClientCommonConstants constants_ = GWT.create(StudioClientCommonConstants.class);
}
