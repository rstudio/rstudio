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

/* Compatibility Macros */
#if __cplusplus < 201103L
# define MOVE_THREAD(t) t.move()
#else
# define MOVE_THREAD(t) std::move(t)
#endif

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
# define RSTUDIO_DEBUG_BLOCK(x) if (false)
# define RSTUDIO_LOG_OBJECT(x)

#else

#include <core/Debug.hpp>

#define RSTUDIO_DEBUG(x)                                                       \
   do                                                                          \
   {                                                                           \
         std::cerr << "(" << RSTUDIO_DEBUG_LABEL << ":" << std::setw(4)        \
                   << std::setfill('0') << __LINE__ << "): " << x              \
                   << std::endl;                                               \
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

# define RSTUDIO_DEBUG_BLOCK(x)                                                \
   if (strlen(x))                                                              \
      std::cerr << "(" << RSTUDIO_DEBUG_LABEL << ":" << std::setw(4)           \
                << std::setfill('0') << __LINE__ << "): " << x << std::endl;   \
   if (true)

# define RSTUDIO_LOG_OBJECT(x)                                                 \
   do                                                                          \
   {                                                                           \
      ::rstudio::core::debug::print(x);                                        \
   } while (0)

#endif /* Debug logging macros */

#ifndef DEBUG
# define DEBUG RSTUDIO_DEBUG
#endif

#ifndef DEBUG_LINE
# define DEBUG_LINE RSTUDIO_DEBUG_LINE
#endif

#ifndef DEBUG_BLOCK
# define DEBUG_BLOCK RSTUDIO_DEBUG_BLOCK
#endif

#ifndef LOG_OBJECT
# define LOG_OBJECT(x) RSTUDIO_LOG_OBJECT(x)
#endif

#endif
