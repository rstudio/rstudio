/*
 * Macros.hpp
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

#ifndef CORE_MACROS_HPP
#define CORE_MACROS_HPP

#include <iostream>
#include <iomanip>

/* Utility Macros */

#if defined(__GNUC__)
# define LIKELY(x)   __builtin_expect(!!(x), 1)
# define UNLIKELY(x) __builtin_expect(!!(x), 0)
#else
# define LIKELY(x)   __builtin_expect(!!(x), 1)
# define UNLIKELY(x) __builtin_expect(!!(x), 0)
#endif

/* Logging Macros */

// re-define this in implementation files for labelled debugging
#ifndef RSTUDIO_DEBUG_LABEL
# define RSTUDIO_DEBUG_LABEL "rstudio"
#endif

/* Debug logging macros */
#ifndef RSTUDIO_ENABLE_DEBUG_MACROS

# define RSTUDIO_DEBUG(x) do {} while (0)
# define RSTUDIO_DEBUG_LINE(x) do {} while (0)
# define RSTUDIO_DEBUG_BLOCK if (false)

#else

# define RSTUDIO_DEBUG(x)                                                      \
   do                                                                          \
   {                                                                           \
      std::cerr << "(" << RSTUDIO_DEBUG_LABEL << ":"                           \
                << std::setw(4) << std::setfill('0') << __LINE__ << "): "      \
                << x << std::endl;                                             \
   } while (0)

# define RSTUDIO_DEBUG_LINE(x)                                                 \
   do                                                                          \
   {                                                                           \
      std::string file = std::string(__FILE__);                                \
      std::string shortFile = file.substr(file.rfind("/") + 1);                \
      std::cerr << "(" << RSTUDIO_DEBUG_LABEL << ")"                           \
                << "[" << shortFile << ":" << __LINE__ << "]: " << std::endl   \
                << x << std::endl;                                             \
   } while (0)

# define RSTUDIO_DEBUG_BLOCK if (true)

#endif /* Debug logging macros */

/* Profiling macros */
#ifndef RSTUDIO_ENABLE_PROFILING

# define RSTUDIO_PROFILE(x) if (false)
# define TIMER(x) do {} while (0)
# define REPORT(timer, message) do {} while (0)

#else

#include <boost/timer/timer.hpp>

template <typename T>
bool return_true(const T& object) { return true; }

#define TIMER(x)                                                               \
   ::boost::timer::cpu_timer x;                                                \
   x.start();

#define REPORT(timer, message)                                                 \
   std::cout << "(profile) " << message << std::endl;                          \
   std::cout << ::boost::timer::format(timer.elapsed(), 3) << std::endl;       \
   timer.stop();                                                               \
   timer = ::boost::timer::cpu_timer();

// Some insanity to ensure the __LINE__ macro is expanded

# define RSTUDIO_PROFILE(x) RSTUDIO_PROFILE_1(x, __LINE__)
# define RSTUDIO_PROFILE_1(x, l) RSTUDIO_PROFILE_2(x, l)
# define RSTUDIO_PROFILE_2(x, l)                                               \
   std::cout << "(profiling) " << x << std::endl;                              \
   ::boost::timer::auto_cpu_timer RS_TIMER__ ## l;                             \
   if (true ||                                                                 \
       return_true(RS_TIMER__ ## l = boost::timer::auto_cpu_timer(3)))

#endif /* Profiling macros */

#ifndef DEBUG
# define DEBUG RSTUDIO_DEBUG
#endif

#ifndef DEBUG_LINE
# define DEBUG_LINE RSTUDIO_DEBUG_LINE
#endif

#ifndef DEBUG_BLOCK
# define DEBUG_BLOCK RSTUDIO_DEBUG_BLOCK
#endif

#ifndef PROFILE
# define PROFILE RSTUDIO_PROFILE
#endif


#endif
