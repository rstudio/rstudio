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
#include <boost/assign/list_of.hpp> 
#include <string> 
#include <vector> 

void create_child() 
{ 
//[create_child_overloads_exe 
    boost::process::create_child("hostname"); 
//] 
} 

void create_child_find_exe() 
{ 
//[create_child_overloads_exe_in_path 
    std::string exe = boost::process::find_executable_in_path("hostname"); 
    boost::process::create_child(exe); 
//] 
} 

void create_child_args() 
{ 
//[create_child_overloads_args 
    std::string exe = boost::process::find_executable_in_path("hostname"); 
    std::vector<std::string> args = boost::assign::list_of("-?"); 
    boost::process::create_child(exe, args); 
//] 
} 

int main() 
{ 
    create_child(); 
    create_child_find_exe(); 
    create_child_args(); 
} 
