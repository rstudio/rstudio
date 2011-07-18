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
#include <string> 
#include <iostream> 

#if defined(BOOST_POSIX_API) 
#   include <sys/wait.h> 
#endif 

//[sync_wait 
int main() 
{ 
    std::string exe = boost::process::find_executable_in_path("hostname"); 
    boost::process::child c = boost::process::create_child(exe); 
    int exit_code = c.wait(); 
#if defined(BOOST_POSIX_API) 
    if (WIFEXITED(exit_code)) 
        exit_code = WEXITSTATUS(exit_code); 
#endif 
    std::cout << exit_code << std::endl; 
} 
//] 
