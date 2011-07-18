// 
// Boost.Process 
// ~~~~~~~~~~~~~ 
// 
// Copyright (c) 2006, 2007 Julio M. Merino Vidal 
// Copyright (c) 2008 Ilya Sokolov, Boris Schaeling 
// Copyright (c) 2009 Boris Schaeling 
// Copyright (c) 2010 Felipe Tanus, Boris Schaeling 
// 
// Distributed under the Boost Software License, Version 1.0. (See accompanying 
// file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt) 
// 

#include <boost/process/all.hpp> 

#if defined(BOOST_WINDOWS_API) 

#include <string> 
#include <vector> 

//[startupinfo_setup_function 
void setup(STARTUPINFOA &sainfo) 
{ 
    sainfo.dwFlags |= STARTF_USEPOSITION | STARTF_USESIZE; 
    sainfo.dwX = 0; 
    sainfo.dwY = 0; 
    sainfo.dwXSize = 640; 
    sainfo.dwYSize = 480; 
} 
//] 

//[startupinfo_setup_main 
int main() 
{ 
    std::string exe = boost::process::find_executable_in_path("notepad"); 
    std::vector<std::string> args; 
    boost::process::context ctx; 
    ctx.setup = &setup; 
    boost::process::create_child(exe, args, ctx); 
} 
//] 

#endif 
