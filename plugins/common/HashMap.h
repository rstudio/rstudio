#ifndef _H_HashMap
#define _H_HashMap
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

// Portability wrapper for hash maps, since they aren't part of the standard C++ library

#ifdef __GNUC__
#ifdef CXX_TR1
// future support
#include <unordered_map>
#define hash_map std::tr1::unordered_map

namespace HashFunctions = std;
#else
#include <ext/hash_map>
using __gnu_cxx::hash_map;

// TODO(jat): surely this exists somewhere already?
// TODO(jat): portability issues
namespace __gnu_cxx {
  using std::size_t;

  template<> struct hash<std::string> {
    size_t operator()(const std::string& str) const {
      return hash<const char*>()(str.c_str());
    }
  };
};
namespace HashFunctions = __gnu_cxx;
#endif
#elif sun
// TODO(jat): find a hash_map implementation for Solaris
#include <map>
namespace HashFunctions = std;
#define hash_map map
using std::map;
#else
// Try something reasonably standard, which works in Windows
#include <hash_map>
using stdext::hash_map;
namespace HashFunctions = stdext;
#endif

#endif
