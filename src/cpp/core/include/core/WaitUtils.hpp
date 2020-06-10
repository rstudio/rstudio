/*
 * WaitUtils.hpp
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

#ifndef CORE_WAITUTILS_HPP
#define CORE_WAITUTILS_HPP

#include <boost/function.hpp>

#include <shared_core/Error.hpp>

namespace rstudio {
namespace core {


enum WaitResultType {
   WaitSuccess,
   WaitContinue,
   WaitError
};

struct WaitResult
{
   WaitResult(WaitResultType type, Error error)
      : type(type), error(error)
   {
   }

   WaitResultType type;
   Error error;
};

Error waitWithTimeout(const boost::function<WaitResult()>& connectFunction,
                      int initialWaitMs = 30,
                      int incrementWaitMs = 10,
                      int maxWaitSec = 10);

} // namespace core
} // namespace rstudio

#endif // CORE_WAITUTILS_HPP
