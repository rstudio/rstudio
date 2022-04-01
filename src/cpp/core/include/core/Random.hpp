/*
 * Random.hpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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

#ifndef CORE_RANDOM_HPP
#define CORE_RANDOM_HPP

#ifndef _WIN32
# include <unistd.h>
#else
# include <process.h>
#endif

#include <limits>
#include <ctime>

#include <boost/random.hpp>

#include <core/system/System.hpp>

namespace rstudio {
namespace core {
namespace random {

template <typename T>
T uniformRandomInteger(
      T minValue = std::numeric_limits<T>::min(),
      T maxValue = std::numeric_limits<T>::max())
{
   boost::random::mt19937 gen(getpid() + std::time(NULL));
   boost::random::uniform_int_distribution<> dist(minValue, maxValue);
   return dist(gen);
}
} // namespace random
} // namespace core
} // namespace rstudio


#endif // CORE_RANDOM_HPP
