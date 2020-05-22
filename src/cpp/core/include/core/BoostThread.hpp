/*
 * BoostThread.hpp
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

#ifndef CORE_BOOST_THREAD_HPP
#define CORE_BOOST_THREAD_HPP

#if defined(__GNUC__) && defined(_WIN32)
  // Boost attempts to use declspec(dllimport) in some inline
  // functions within boost::thread. This causes compiler warnings
  // on MinGW which we want to avoid.
  #undef BOOST_HAS_DECLSPEC
#endif

#include <boost/thread.hpp>
#include <boost/thread/condition.hpp>

#endif // CORE_BOOST_THREAD_HPP

