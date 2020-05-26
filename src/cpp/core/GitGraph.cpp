/*
 * GitGraph.cpp
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

#include <limits>
#include <algorithm>

#include <core/GitGraph.hpp>

#include <shared_core/SafeConvert.hpp>

namespace rstudio {
namespace core {
namespace gitgraph {

namespace {

// If true, then this column terminates in this row's nexus.
bool isColumnTerminating(const Column& column)
{
   return column.postCommit.empty();
}

// If true, then this column either starts in this row's nexus,
// ends there, or passes through it. If false, then the column
// has nothing going on in this row (well, it's possibly changing
// positions as a result of other columns being added/removed).
bool isColumnDynamic(const Column& column)
{
   return column.preCommit != column.postCommit;
}

} // namespace

size_t Line::nexus() const
{
   Line::const_iterator it = std::find_if(begin(), end(), &isColumnDynamic);
   if (it == end())
      return std::numeric_limits<size_t>::max();
   else
      return it - begin();
}

std::string Line::string() const
{
   std::string output;
   bool sawNexus = false;
   for (size_t i = 0; i < size(); i++)
   {
      const Column& c = at(i);
      if (isColumnDynamic(c))
      {
         if (!sawNexus)
         {
            sawNexus = true;
            output.append("*");
         }

         if (c.preCommit.empty())
            output.append("+");
         if (c.postCommit.empty())
            output.append("-");
      }

      output.append(safe_convert::numberToString(c.id));

      if (i < size() - 1)
         output.append(" ");
   }
   return output;
}

Line GitGraph::addCommit(const std::string& commit,
                         const std::vector<std::string>& parents)
{
   // If this commit is a merge (has multiple parents) then we'll want to
   // insert new columns immediately to the right of the existing column.
   // If this commit isn't the parent of a previously seen node, then we'll
   // definitely be adding one or more columns to the right of all the
   // existing columns.
   auto insertNewColumnsAt = pendingLine_.end();

   // Counts how many of the parents have been assigned to columns.
   size_t parentsUsed = 0;

   for (auto it = pendingLine_.begin();
        it != pendingLine_.end();
        it++)
   {
      if (it->preCommit == commit)
      {
         // This column was expecting the current commit. We can either
         // terminate the column here, or, we can take the first parent
         // and set that as the new commit (postCommit) for this column.
         // We can only do the latter once, as we want all of the columns
         // for this commit to converge on one point in the graph.

         // This if clause is what ensures we'll only do this once.
         if (insertNewColumnsAt == pendingLine_.end())
         {
            // If this is a merge, we'll insert the other branches just
            // to the right of us.
            insertNewColumnsAt = it + 1;

            // Either assign the first parent to this, or if this is an
            // unparented commit (e.g. initial commit in a repo) then just
            // terminate here.
            if (parentsUsed == parents.size())
               it->postCommit = "";
            else
               it->postCommit = parents[parentsUsed++];
         }
         else
         {
            it->postCommit = "";
         }
      }
   }

   // Make new columns for any parents we haven't already used.
   while (parentsUsed != parents.size())
   {
      insertNewColumnsAt = 1 + pendingLine_.insert(
            insertNewColumnsAt,
            Column(nextColumnId_++, "", parents[parentsUsed++]));
   }

   // This line is ready. Make a copy of it.
   Line result = pendingLine_;

   /*
    * Now fix up pendingLine_ to get ready for the next call to addCommit.
    */

   // First remove all columns that have empty postCommit--these terminated.
   auto newEnd = std::remove_if(
         pendingLine_.begin(), pendingLine_.end(), &isColumnTerminating);
   pendingLine_.erase(newEnd, pendingLine_.end());

   // Now copy all of the postCommits to preCommit.
   for (auto& column : pendingLine_)
   {
      column.preCommit = column.postCommit;
   }

   return result;
}

} // namespace gitgraph
} // namespace core
} // namespace rstudio
