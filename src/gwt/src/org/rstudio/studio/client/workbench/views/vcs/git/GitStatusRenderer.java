/*
 * GitStatusRenderer.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs.git;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import org.rstudio.studio.client.workbench.views.vcs.common.ChangelistTable.ChangelistTableCellTableResources;

public class GitStatusRenderer implements SafeHtmlRenderer<String>
{
   interface StatusResources extends ClientBundle
   {
      @Source("images/statusAdded.png")
      ImageResource statusAdded();
      @Source("images/statusDeleted.png")
      ImageResource statusDeleted();
      @Source("images/statusModified.png")
      ImageResource statusModified();
      @Source("images/statusNone.png")
      ImageResource statusNone();
      @Source("images/statusCopied.png")
      ImageResource statusCopied();
      @Source("images/statusUntracked.png")
      ImageResource statusUntracked();
      @Source("images/statusUnmerged.png")
      ImageResource statusUnmerged();
      @Source("images/statusRenamed.png")
      ImageResource statusRenamed();
   }

   public GitStatusRenderer()
   {
   }

   @Override
   public SafeHtml render(String str)
   {
      if (str.length() != 2)
         return null;

      ImageResource indexImg = imgForStatus(str.charAt(0));
      ImageResource treeImg = imgForStatus(str.charAt(1));

      SafeHtmlBuilder builder = new SafeHtmlBuilder();
      builder.append(SafeHtmlUtils.fromTrustedString(
            "<span " +
            "class=\"" + ctRes_.cellTableStyle().status() + "\" " +
            "title=\"" +
            SafeHtmlUtils.htmlEscape(descForStatus(str)) +
            "\">"));

      builder.append(SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(
            indexImg).getHTML()));
      builder.append(SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(treeImg).getHTML()));

      builder.appendHtmlConstant("</span>");

      return builder.toSafeHtml();
   }

   private String descForStatus(String str)
   {
      String indexDesc = descForStatus(str.charAt(0));
      String treeDesc = descForStatus(str.charAt(1));
      
      if (indexDesc.length() > 0 && treeDesc.length() > 0)
         return indexDesc + "/" + treeDesc;
      else if (indexDesc.length() > 0)
         return indexDesc;
      else if (treeDesc.length() > 0)
         return treeDesc;
      else
         return "";
   }

   private String descForStatus(char c)
   {
      switch (c)
      {
         case 'A':
            return "Added";
         case 'M':
            return "Modified";
         case 'D':
            return "Deleted";
         case 'R':
            return "Renamed";
         case 'C':
            return "Copied";
         case '?':
            return "Untracked";
         case 'U':
            return "Unmerged";
         case ' ':
            return "";
         default:
            return "";
      }
   }
   
   private ImageResource imgForStatus(char c)
   {
      switch (c)
      {
         case 'A':
            return resources_.statusAdded();
         case 'M':
            return resources_.statusModified();
         case 'D':
            return resources_.statusDeleted();
         case 'R':
            return resources_.statusRenamed();
         case 'C':
            return resources_.statusCopied();
         case '?':
            return resources_.statusUntracked();
         case 'U':
            return resources_.statusUnmerged();
         case ' ':
            return resources_.statusNone();
         default:
            return resources_.statusNone();
      }
   }

   @Override
   public void render(String str, SafeHtmlBuilder builder)
   {
      SafeHtml safeHtml = render(str);
      if (safeHtml != null)
         builder.append(safeHtml);
   }

   private static final StatusResources resources_ = GWT.create(StatusResources.class);
   private static final ChangelistTableCellTableResources ctRes_ = GWT.create(ChangelistTableCellTableResources.class);
}
