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

template <typename StringType>
struct Edit
{
   std::size_t offset = 0;
   std::size_t size = 0;
   StringType value;
};


// Given two strings, a 'source' and a 'target' string, compute a
// set of 'edits' that could be used to transform from the 'source' to
// the 'target' string. Note that the edits should be applied in reverse
// order, as any edit could invalidate the offsets of edits performed
// later in the string.
template <typename StringType>
inline std::vector<Edit<StringType>> computeEdits(
   const StringType& source,
   const StringType& target)
{
   // The vector of edits we've discovered.
   std::vector<Edit<StringType>> result;

   // The current position and offset; updated as we iterate over edits.
   std::size_t offset = 0;

   // Compute the diff.
   dtl::Diff<typename StringType::value_type, StringType> diff(source, target);
   diff.compose();

   // Get a reference to the edits.
   auto ses = diff.getSes();
   auto items = ses.getSequence();

   // Iterate through and discover edits. We consider two types of edits:
   //
   // - A sequence of deletions, followed (optionally) by a sequence of additions,
   // - A sequences of additions.
   //
   std::size_t i = 0, n = items.size();
   while (i < n)
   {
      // Handle deletion (+ additions).
      if (items[i].second.type == dtl::SES_DELETE)
      {
         Edit<StringType> edit;
         edit.offset = offset;

         for (; i < n; i++)
         {
            if (items[i].second.type != dtl::SES_DELETE)
               break;

            offset += 1;
            edit.size += 1;
         }

         for (; i < n; i++)
         {
            if (items[i].second.type != dtl::SES_ADD)
               break;

            edit.value += items[i].first;
         }

         result.push_back(edit);
         continue;
      }

      // Handle additions.
      else if (items[i].second.type == dtl::SES_ADD)
      {
         Edit<StringType> edit;
         edit.offset = offset;

         for (; i < n; i++)
         {
            if (items[i].second.type != dtl::SES_ADD)
               break;

            edit.value += items[i].first;
         }

         result.push_back(edit);
         continue;
      }

      // No edit in this location.
      else
      {
         i += 1;
         offset += 1;
         continue;
      }
   }

   // Return the edits.
   return result;

}

} // end namespace diff
} // end namespace core
} // end namespace rstudio

#endif /* CORE_DIFF_HPP */
