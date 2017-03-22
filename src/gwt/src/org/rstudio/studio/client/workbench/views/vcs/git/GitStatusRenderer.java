/*
 * GitStatusRenderer.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.vcs.git;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.shared.SafeHtmlRenderer;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.workbench.views.vcs.common.ChangelistTable.ChangelistTableCellTableResources;

public class GitStatusRenderer implements SafeHtmlRenderer<String>
{
   interface StatusResources extends ClientBundle
   {
      @Source("images/statusAdded_2x.png")
      ImageResource statusAdded2x();
      @Source("images/statusDeleted_2x.png")
      ImageResource statusDeleted2x();
      @Source("images/statusModified_2x.png")
      ImageResource statusModified2x();
      @Source("images/statusNone_2x.png")
      ImageResource statusNone2x();
      @Source("images/statusCopied_2x.png")
      ImageResource statusCopied2x();
      @Source("images/statusUntracked_2x.png")
      ImageResource statusUntracked2x();
      @Source("images/statusUnmerged_2x.png")
      ImageResource statusUnmerged2x();
      @Source("images/statusRenamed_2x.png")
      ImageResource statusRenamed2x();
   }

   public GitStatusRenderer()
   {
   }

   @Override
   public SafeHtml render(String str)
   {
      if (str.length() != 2)
         return null;

      ImageResource2x indexImg = imgForStatus(str.charAt(0));
      ImageResource2x treeImg = imgForStatus(str.charAt(1));

      SafeHtmlBuilder builder = new SafeHtmlBuilder();
      builder.append(SafeHtmlUtils.fromTrustedString(
            "<span " +
            "class=\"" + ctRes_.cellTableStyle().status() + "\" " +
            "title=\"" +
            SafeHtmlUtils.htmlEscape(descForStatus(str)) +
            "\">"));

      builder.append(indexImg.getSafeHtml());
      builder.append(treeImg.getSafeHtml());

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
   
   private ImageResource2x imgForStatus(char c)
   {
      switch (c)
      {
         case 'A':
            return new ImageResource2x(resources_.statusAdded2x());
         case 'M':
            return new ImageResource2x(resources_.statusModified2x());
         case 'D':
            return new ImageResource2x(resources_.statusDeleted2x());
         case 'R':
            return new ImageResource2x(resources_.statusRenamed2x());
         case 'C':
            return new ImageResource2x(resources_.statusCopied2x());
         case '?':
            return new ImageResource2x(resources_.statusUntracked2x());
         case 'U':
            return new ImageResource2x(resources_.statusUnmerged2x());
         case ' ':
            return new ImageResource2x(resources_.statusNone2x());
         default:
            return new ImageResource2x(resources_.statusNone2x());
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
