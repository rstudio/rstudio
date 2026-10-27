/*
 * SessionRUtilTests.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifdef _WIN32

#include <gtest/gtest.h>

#include <string>

#include <boost/scope_exit.hpp>

#include <core/system/RegistryKey.hpp>
#include <core/system/System.hpp>

#include <r/RExec.hpp>
#include <r/RSexp.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace tests {

// .rs.rig.registryRead() snapshots values such as the user PATH and writes
// them back after an install; under R < 4.2 the native encoding is a code
// page, so data that reaches R unmarked would be re-encoded on the way back
TEST(SessionRUtilTest, RegistryValuesReachRAsUtf8)
{
   // directly under Software, which always exists: create() would also make
   // any missing parent keys, and leave them behind
   std::string subKey = "Software\\RStudio-Tests-" + core::system::generateUuid(false);
   std::string keyPath = "HKCU\\" + subKey;
   std::string path = "%USERPROFILE%\\Caf\xC3\xA9" ";C:\\Tools";

   BOOST_SCOPE_EXIT(&subKey)
   {
      ::RegDeleteKeyA(HKEY_CURRENT_USER, subKey.c_str());
   }
   BOOST_SCOPE_EXIT_END

   {
      core::system::RegistryKey key;
      Error error = key.create(HKEY_CURRENT_USER, subKey, KEY_READ | KEY_WRITE);
      ASSERT_FALSE(error) << error.asString();
      error = key.setValue("Path", path, REG_EXPAND_SZ);
      ASSERT_FALSE(error) << error.asString();
   }

   r::sexp::Protect protect;
   SEXP valueSEXP = R_NilValue;
   Error error = r::exec::RFunction(".rs.registryGetValue")
         .addParam(keyPath)
         .addParam("Path")
         .call(&valueSEXP, &protect);
   ASSERT_FALSE(error) << error.asString();
   ASSERT_EQ(VECSXP, TYPEOF(valueSEXP));

   SEXP dataSEXP = R_NilValue;
   error = r::sexp::getNamedListSEXP(valueSEXP, "data", &dataSEXP);
   ASSERT_FALSE(error) << error.asString();
   ASSERT_EQ(STRSXP, TYPEOF(dataSEXP));
   ASSERT_EQ(1, Rf_length(dataSEXP));

   SEXP charSEXP = STRING_ELT(dataSEXP, 0);
   EXPECT_EQ(CE_UTF8, Rf_getCharCE(charSEXP));
   EXPECT_EQ(path, std::string(CHAR(charSEXP), LENGTH(charSEXP)));

   SEXP typeSEXP = R_NilValue;
   error = r::sexp::getNamedListSEXP(valueSEXP, "type", &typeSEXP);
   ASSERT_FALSE(error) << error.asString();

   // the value as read goes back unchanged, as the rig snapshot restore does
   error = r::exec::RFunction(".rs.registrySetValue")
         .addParam(keyPath)
         .addParam("Copy")
         .addParam(dataSEXP)
         .addParam(typeSEXP)
         .call();
   ASSERT_FALSE(error) << error.asString();

   core::system::RegistryKey key;
   error = key.open(HKEY_CURRENT_USER, subKey, KEY_QUERY_VALUE);
   ASSERT_FALSE(error) << error.asString();

   std::string copy;
   DWORD type = REG_NONE;
   error = key.getValue("Copy", &copy, &type);
   ASSERT_FALSE(error) << error.asString();
   EXPECT_EQ(path, copy);
   EXPECT_EQ(static_cast<DWORD>(REG_EXPAND_SZ), type);
}

} // namespace tests
} // namespace session
} // namespace rstudio

#endif // _WIN32
