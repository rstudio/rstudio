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

// re-define this in implementation files for labelled debugging
#define RSTUDIO_DEBUG_LABEL "rstudio"

/**
 * Basic infrastructure for macro dispatch
 */

// Concatenate two tokens.
#define RSTUDIO_PP_PASTE(A, B) A ## B

// Concatenate a name and a number (to form the 
// name of an implementation macro)
#define RSTUDIO_PP_SELECT(NAME, NUMBER) \
   RSTUDIO_PP_PASTE(NAME ## _, NUMBER)

// Count the number of arguments in a variadic macro.
// This uses the GCC extension ##__VA_ARGS__, but should
// be supported in all the platforms we target.
#define RSTUDIO_PP_VA_NUM_ARGS(...) \
   RSTUDIO_PP_VA_NUM_ARGS_IMPL(, ##__VA_ARGS__, 5, 4, 3, 2, 1, 0)

#define RSTUDIO_PP_VA_NUM_ARGS_IMPL(_0, _1, _2, _3, _4, _5, N, ...) N

/**
 * End macro dispatch infrastructure
 */

// A selector for debugging blocks of code
#define RSTUDIO_DEBUG_BLOCK(...)                                               \
   RSTUDIO_PP_SELECT(                                                          \
      RSTUDIO_DEBUG_BLOCK__IMPL,                                               \
      RSTUDIO_PP_VA_NUM_ARGS(__VA_ARGS__)                                      \
   )(__VA_ARGS__)

#define RSTUDIO_DEBUG_BLOCK__IMPL_0    if (false)
#define RSTUDIO_DEBUG_BLOCK__IMPL_1(x) if (false)

// More vanilla debugging
#define RSTUDIO_DEBUG(x)

// When RSTUDIO_DEBUG_MODE is set, we provide implementations for
// the above.
#ifdef RSTUDIO_DEBUG_MODE

#undef RSTUDIO_DEBUG
#define RSTUDIO_DEBUG(x)                                                       \
   do                                                                          \
   {                                                                           \
      std::cerr << "[" << RSTUDIO_DEBUG_LABEL << "] "                          \
                << "(" << __FILE__ << ":" << __LINE__ << "):" << std::endl     \
                << x << std::endl << std::endl;                                \
   } while (0)

#undef RSTUDIO_DEBUG_BLOCK__IMPL_0
#define RSTUDIO_DEBUG_BLOCK__IMPL_0 if (true)

#undef RSTUDIO_DEBUG_BLOCK__IMPL_1
#define RSTUDIO_DEBUG_BLOCK__IMPL_1(x)                                         \
   RSTUDIO_DEBUG(x); \
   if (true)

#endif

#endif
