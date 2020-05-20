/*
 * Random.hpp
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

#ifndef CORE_RANDOM_HPP
#define CORE_RANDOM_HPP

#include <limits>
#include <ctime>

#include <boost/random.hpp>

namespace rstudio {
namespace core {
namespace random {

template <typename T>
T uniformRandomInteger()
{
  // setup generator and distribution
  typedef boost::mt19937 GeneratorType;
  typedef boost::uniform_int<T> DistributionType;
  GeneratorType generator(std::time(nullptr));
  DistributionType distribution(std::numeric_limits<T>::min(),
                                std::numeric_limits<T>::max());

  // create variate generator
  boost::variate_generator<GeneratorType, DistributionType> vg(generator,
                                                               distribution);

  // return random number
  return vg();
}

} // namespace random
} // namespace core
} // namespace rstudio


#endif // CORE_RANDOM_HPP
