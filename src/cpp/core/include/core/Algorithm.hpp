/*
 * Algorithm.hpp
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


#ifndef CORE_ALGORITHM_HPP
#define CORE_ALGORITHM_HPP

namespace core {
namespace algorithm {

template<typename InputIterator, typename OutputIterator, typename Predicate>
OutputIterator copy_if(InputIterator begin,
                       InputIterator end,
                       OutputIterator destBegin,
                       Predicate p)
{
   while (begin != end)
   {
      if (p(*begin))
        *destBegin++ = *begin;
      ++begin;
   }
   return destBegin;
}

template<typename InputIterator,
         typename OutputIterator,
         typename Predicate,
         typename UnaryOperator>
OutputIterator copy_transformed_if(InputIterator begin,
                                   InputIterator end,
                                   OutputIterator destBegin,
                                   Predicate p,
                                   UnaryOperator op)
{
   while (begin != end)
   {
      if (p(*begin))
        *destBegin++ = op(*begin);
      ++begin;
   }
   return destBegin;
}


} // namespace algorithm
} // namespace core


#endif // CORE_ALGORITHM_HPP
