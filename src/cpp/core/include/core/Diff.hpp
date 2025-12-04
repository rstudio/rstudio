/*
 * Diff.hpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_DIFF_HPP
#define CORE_DIFF_HPP

#include <string>
#include <vector>

#include <dtl/dtl.hpp>

#include <core/StringUtils.hpp>

namespace rstudio {
namespace core {
namespace diff {

struct Edit
{
   std::size_t offset;
   std::size_t size;
   std::string value;
};

// Given two strings, a 'source' and a 'target' string, compute a
// set of 'edits' that could be used to transform from the 'source' to
// the 'target' string. Note that the edits should be applied in reverse
// order, as any edit could invalidate the offsets of edits performed
// later in the string.
inline std::vector<Edit> computeEdits(
   const std::string& source,
   const std::string& target)
{
   // The vector of edits we've discovered.
   std::vector<Edit> result;

   // The current edit. This will be updated as we iterate over
   // all of the computed edits in the diff.
   Edit edit;

   // The current position and offset; updated as we iterate over edits.
   std::size_t offset = 0;
   bool isBuildingEdit = false;

   // Compute the diff.
   dtl::Diff<char, std::string> diff(source, target);
   diff.compose();

   // Get a reference to the edits.
   auto ses = diff.getSes();
   auto items = ses.getSequence();

   // Keep track of whether we're actively building an edit.
   for (std::size_t i = 0, n = items.size(); i < n; i++)
   {
      auto&& item = items[i];

      char ch = item.first;
      auto&& info = item.second;

      if (isBuildingEdit)
      {
         if (info.type == dtl::SES_COMMON)
         {
            // If the next character would begin a new edit, just continue
            // this one. This avoids collecting a series of tiny diffs.
            if (i + 1 < n && items[i + 1].second.type != dtl::SES_COMMON)
            {
               edit.value += ch;
               edit.size += 1;
            }
            else
            {
               isBuildingEdit = false;
               result.push_back(edit);
            }
         }

         else if (info.type == dtl::SES_ADD)
         {
            edit.value += ch;
         }

         else if (info.type == dtl::SES_DELETE)
         {
            edit.size += 1;
         }
      }
      else
      {
         if (info.type != dtl::SES_COMMON)
         {
            isBuildingEdit = true;
            edit.size = 0;
            edit.offset = offset;
            edit.value = ch;
         }
      }

      // Update the position and offset for non-addition edits.
      if (info.type != dtl::SES_ADD)
      {
         offset += 1;
      }
   }

   // If we have a leftover edit at the end, add it.
   if (isBuildingEdit)
   {
      isBuildingEdit = false;
      result.push_back(edit);
   }

   // Debugging.
   for (auto&& item : result)
   {
      std::string value = string_utils::jsonLiteralEscape(item.value);
      std::cerr << "(" << item.offset << ", " << item.size << ", \"" << value << "\")" << std::endl;
   }

   // Return the edits.
   return result;

}

} // end namespace diff
} // end namespace core
} // end namespace rstudio

#endif /* CORE_DIFF_HPP */