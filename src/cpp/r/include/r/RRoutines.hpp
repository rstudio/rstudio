/*
 * RRoutines.hpp
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

#ifndef R_ROUTINES_HPP
#define R_ROUTINES_HPP

#include <vector>

#include <r/RSexp.hpp>

#include <R_ext/Rdynload.h>

#include <core/type_traits/TypeTraits.hpp>

namespace rstudio {
namespace r {
namespace routines {

namespace internal {

template <typename ReturnType, typename... ArgumentTypes>
int n_arguments(ReturnType(ArgumentTypes...)) {
    return sizeof...(ArgumentTypes);
}

template <typename... ArgumentTypes>
struct validate_args
{
   typedef std::true_type type;
};

template <typename T, typename... ArgumentTypes>
struct validate_args<T, ArgumentTypes...>
{
   static_assert(
         std::is_same<T, SEXP>::value,
         "Registered .Call methods should only accept SEXP parameters");

   typedef typename validate_args<ArgumentTypes...>::type type;
};

template <typename ReturnType, typename... ArgumentTypes>
void validate(ReturnType(ArgumentTypes...))
{
   static_assert(
            std::is_same<ReturnType, SEXP>::value,
            "Registered .Call methods should have SEXP return type");

   (void) typename validate_args<ArgumentTypes...>::type{};
}

} // end namespace internal

void addCallMethod(const R_CallMethodDef method);
void registerCallMethod(const char* name, DL_FUNC fun, int numArgs);
void registerAll();

// NOTE: This macro accepts multiple arguments but only uses the first one.
// This is just for backwards compatibility, so that existing calls that
// attempt to declare the number of arguments can continue to do so, while new
// usages can omit it and still do the right thing. (This is done primarily to
// avoid noisy diffs across the codebase where this macro is used)
#define RS_REGISTER_CALL_METHOD(__NAME__, ...)                         \
   do                                                                  \
   {                                                                   \
      using namespace core::type_traits;                               \
      ::rstudio::r::routines::internal::validate(__NAME__);            \
      int n = ::rstudio::r::routines::internal::n_arguments(__NAME__); \
      R_CallMethodDef callMethodDef;                                   \
      callMethodDef.name = #__NAME__;                                  \
      callMethodDef.fun = reinterpret_cast<DL_FUNC>(__NAME__);         \
      callMethodDef.numArgs = n;                                       \
      ::rstudio::r::routines::addCallMethod(callMethodDef);            \
   }                                                                   \
   while (false)

} // namespace routines   
} // namespace r
} // namespace rstudio


#endif // R_ROUTINES_HPP 

