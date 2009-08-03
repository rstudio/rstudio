/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

// This file exists to verify that the debugging code leaves nothing
// behind when disabled or at an appropriate level.
//
// Compile with, for example, g++ -O3 -D* -S DebugTest.cpp and inspect the DebugTest.s file.
//   where * is GWT_DEBUGDISABLE or GWT_DEBUGLEVEL=Spam (etc)
//
// testdebug is a shell script to automate this test

// #define GWT_DEBUGDISABLE
// #define GWT_DEBUGLEVEL Info

#include "../Debug.h"

void foo(int i) {
  Debug::log(Debug::Error) << "Error GarbalDeGook" << i << Debug::flush;
  Debug::log(Debug::Warning) << "Warning GarbalDeGook" << i << Debug::flush;
  Debug::log(Debug::Info) << "Info GarbalDeGook" << i << Debug::flush;
  Debug::log(Debug::Debugging) << "Debugging GarbalDeGook" << i << Debug::flush;
  Debug::log(Debug::Spam) << "Spam GarbalDeGook" << i << Debug::flush;
  if (Debug::level(Debug::Spam)) {
    extern int ExpensiveCall();
    Debug::log(Debug::Spam) << "Filtered spam GarbalDeGook" << ExpensiveCall() << Debug::flush;
  }
}
