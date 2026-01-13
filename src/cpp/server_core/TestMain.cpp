/*
 * TestMain.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#include <gtest/gtest.h>
#include <cstdlib>
#include <iostream>

int main(int argc, char* argv[])
{
   testing::InitGoogleTest(&argc, argv);
   
   // Check if Docker environment is enabled
   if (std::getenv("DOCKER_ENABLED") == nullptr) {
      std::cout << "Docker environment disabled. Set DOCKER_ENABLED=1 to run Docker-specific tests." << std::endl;
   } else {
      std::cout << "Docker environment enabled. Running all tests including Docker-specific tests." << std::endl;
   }
   
   return RUN_ALL_TESTS();
}
