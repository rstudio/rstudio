/*
 * SVNDiffParser.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs.svn;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.*;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.Line.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Provides parsing of SVN diffs, which may contain property changes. Property
 * changes are treated as Info lines in an ignorable chunk, whereas item diffs
 * are treated as usual using UnifiedParser.
 */
public class SVNDiffParser implements DiffParser
{
   private static class Section
   {
      private Section(boolean property, String filename, String data)
      {
         isProperty = property;
         this.filename = filename;
         this.data = data;
      }

      public boolean isProperty;
      public String filename;
      public String data;
   }

   public SVNDiffParser(String data)
   {
      Pattern sectionHeaderPattern = Pattern.create(
            "^((Index: ([^\\r\\n]+)\\r?\\n=+)|(\\r?\\nProperty changes on: ([^\\r\\n]+)\\r?\\n_+))$");

      ArrayList<String> sectionData = new ArrayList<String>();
      ArrayList<Match> sectionMatches = new ArrayList<Match>();

      int pos = 0;
      int lastHeaderStart = -1;
      Match m;
      while (null != (m = sectionHeaderPattern.match(data, pos)))
      {
         sectionMatches.add(m);

         if (lastHeaderStart >= 0)
            sectionData.add(data.substring(lastHeaderStart, m.getIndex()));

         lastHeaderStart = m.getIndex();
         pos = m.getIndex() + m.getValue().length();
      }
      if (lastHeaderStart >= 0)
         sectionData.add(data.substring(lastHeaderStart));

      sections_ = new ArrayList<Section>();
      for (int i = 0; i < sectionData.size(); i++)
      {
         sections_.add(parseSection(sectionMatches.get(i), sectionData.get(i)));
      }
   }

   private Section parseSection(Match m, String sectionData)
   {
      String filename = m.getGroup(3) != null ? m.getGroup(3)
                                              : m.getGroup(5);
      if (filename == null)
      {
         throw new RuntimeException(
               "Programmer error: Filename not found in diff section header");
      }

      boolean isProperty = m.getGroup(4) != null;

      return new Section(isProperty, filename, sectionData);
   }

   @Override
   public DiffFileHeader nextFilePair()
   {
      pendingDiffChunks_.clear();

      if (sections_.size() == 0)
         return null;

      ArrayList<Section> sectionsToUse = new ArrayList<Section>();

      sectionsToUse.add(sections_.remove(0));
      String filename = sectionsToUse.get(0).filename;
      while (sections_.size() > 0 && sections_.get(0).filename.equals(filename))
      {
         sectionsToUse.add(sections_.remove(0));
      }

      // Put the properties above the item diffs.
      Collections.sort(sectionsToUse, new Comparator<Section>()
      {
         @Override
         public int compare(Section a, Section b)
         {
            return a.isProperty == b.isProperty ? 0 :
                   a.isProperty ? -1 :
                   1;
         }
      });

      for (Section section : sectionsToUse)
      {
         if (section.isProperty)
         {
            int chunkDiffIndex = diffIndex_++;

            ArrayList<Line> lines = new ArrayList<Line>();
            String trimmed = StringUtil.trimBlankLines(section.data);
            for (String line : StringUtil.getLineIterator(trimmed))
            {
               lines.add(new Line(Type.Info,
                                  new boolean[] {false, false},
                                  new int[] {-1, -1},
                                  StringUtil.isNullOrEmpty(line) ? "\n" : line,
                                  diffIndex_++));
            }
            pendingDiffChunks_.add(new DiffChunk(null, null, lines,
                                                 chunkDiffIndex));
         }
         else
         {
            UnifiedParser parser = new UnifiedParser(section.data, diffIndex_);
            parser.nextFilePair();
            DiffChunk chunk;
            while (null != (chunk = parser.nextChunk()))
            {
               pendingDiffChunks_.add(chunk);
            }

            diffIndex_ = parser.getDiffIndex();
         }
      }

      return new DiffFileHeader(new ArrayList<String>(), filename, filename);
   }

   @Override
   public DiffChunk nextChunk()
   {
      if (pendingDiffChunks_.size() == 0)
         return null;

      return pendingDiffChunks_.remove(0);
   }

   private final ArrayList<DiffChunk> pendingDiffChunks_ = new ArrayList<DiffChunk>();
   private final ArrayList<Section> sections_;
   private int diffIndex_;
}
