/*
 * Random.hpp
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

#ifndef CORE_RANDOM_HPP
#define CORE_RANDOM_HPP

#include <limits>

#include <boost/chrono.hpp>
#include <boost/random.hpp>
#include <boost/thread.hpp>

namespace rstudio {
namespace core {
namespace random {

template <typename T>
T uniformRandomInteger(
      T minValue = std::numeric_limits<T>::min(),
      T maxValue = std::numeric_limits<T>::max())
{
   // choose random seed
   long long seed = boost::chrono::high_resolution_clock::now().time_since_epoch().count();

   // construct our generator
   boost::random::mt19937 gen(seed);
   boost::random::uniform_int_distribution<> dist(minValue, maxValue);

   // generate a value
   return dist(gen);
}

} // namespace random
} // namespace core
} // namespace rstudio


#endif // CORE_RANDOM_HPP
