/*
 * WeakCallback.hpp
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

#ifndef CORE_WEAK_CALLBACK_HPP
#define CORE_WEAK_CALLBACK_HPP

// these macros currently only work with GCC and Clang
#if !defined(_WIN32)
// macros to make the declaration and definition of weak callbacks much easier
// usage: for a callback that would normally take a shared pointer that should only
// capture a weak pointer, add WEAK_CALLBACK_DECL(instance type, return type, method name, arg 1 type, arg 1, ...);
// to the class declaration - then, the callback method will be auto generated and only the instance method
// needs to be written, saving quite a bit of boilerplate
#define WEAK_CALLBACK_DECL_6(instanceType, methodName, arg1Type, arg1, arg2Type, arg2,             \
                              arg3Type, arg3, arg4Type, arg4, arg5Type, arg5, arg6Type, arg6)      \
   static void methodName##Callback(const boost::weak_ptr<instanceType>& weak, arg1Type arg1,      \
                                    arg2Type arg2, arg3Type arg3, arg4Type arg4,                   \
                                    arg5Type arg5, arg6Type arg6)                                  \
   {                                                                                               \
      if (boost::shared_ptr<instanceType> instance = weak.lock())                                  \
      {                                                                                            \
         instance->methodName(arg1, arg2, arg3, arg4, arg5, arg6);                                 \
      }                                                                                            \
   }                                                                                               \
   void methodName(arg1Type arg1, arg2Type arg2, arg3Type arg3, arg4Type arg4, arg5Type arg6,      \
                   arg6Type arg6)

#define WEAK_CALLBACK_DECL_5(instanceType, methodName, arg1Type, arg1, arg2Type, arg2,             \
                             arg3Type, arg3, arg4Type, arg4, arg5Type, arg5)                       \
   static void methodName##Callback(const boost::weak_ptr<instanceType>& weak, arg1Type arg1,      \
                                    arg2Type arg2, arg3Type arg3, arg4Type arg4, arg5Type arg5)    \
   {                                                                                               \
      if (boost::shared_ptr<instanceType> instance = weak.lock())                                  \
      {                                                                                            \
         instance->methodName(arg1, arg2, arg3, arg4, arg5);                                       \
      }                                                                                            \
   }                                                                                               \
   void methodName(arg1Type arg1, arg2Type arg2, arg3Type arg3, arg4Type arg4, arg5Type arg5)


#define WEAK_CALLBACK_DECL_4(instanceType, methodName, arg1Type, arg1, arg2Type, arg2,             \
                             arg3Type, arg3, arg4Type, arg4)                                       \
   static void methodName##Callback(const boost::weak_ptr<instanceType>& weak, arg1Type arg1,      \
                                    arg2Type arg2, arg3Type arg3, arg4Type arg4)                   \
   {                                                                                               \
      if (boost::shared_ptr<instanceType> instance = weak.lock())                                  \
      {                                                                                            \
         instance->methodName(arg1, arg2, arg3, arg4);                                             \
      }                                                                                            \
   }                                                                                               \
   void methodName(arg1Type arg1, arg2Type arg2, arg3Type arg3, arg4Type arg4)

#define WEAK_CALLBACK_DECL_3(instanceType, methodName, arg1Type, arg1, arg2Type, arg2,             \
                             arg3Type, arg3)                                                       \
   static void methodName##Callback(const boost::weak_ptr<instanceType>& weak, arg1Type arg1,      \
                                    arg2Type arg2, arg3Type arg3)                                  \
   {                                                                                               \
      if (boost::shared_ptr<instanceType> instance = weak.lock())                                  \
      {                                                                                            \
         instance->methodName(arg1, arg2, arg3);                                                   \
      }                                                                                            \
   }                                                                                               \
   void methodName(arg1Type arg1, arg2Type arg2, arg3Type arg3)


#define WEAK_CALLBACK_DECL_2(instanceType, methodName, arg1Type, arg1, arg2Type, arg2)             \
   static void methodName##Callback(const boost::weak_ptr<instanceType>& weak, arg1Type arg1,      \
                                    arg2Type arg2)                                                 \
   {                                                                                               \
      if (boost::shared_ptr<instanceType> instance = weak.lock())                                  \
      {                                                                                            \
         instance->methodName(arg1, arg2);                                                         \
      }                                                                                            \
   }                                                                                               \
   void methodName(arg1Type arg1, arg2Type arg2)

#define WEAK_CALLBACK_DECL_1(instanceType, methodName, arg1Type, arg1)                             \
   static void methodName##Callback(const boost::weak_ptr<instanceType>& weak, arg1Type arg1)      \
   {                                                                                               \
      if (boost::shared_ptr<instanceType> instance = weak.lock())                                  \
      {                                                                                            \
         instance->methodName(arg1);                                                               \
      }                                                                                            \
   }                                                                                               \
   void methodName(arg1Type arg1)

#define WEAK_CALLBACK_DECL_0(instanceType, methodName, dummy)                                      \
   static void methodName##Callback(const boost::weak_ptr<instanceType>& weak)                     \
   {                                                                                               \
      if (boost::shared_ptr<instanceType> instance = weak.lock())                                  \
      {                                                                                            \
         instance->methodName();                                                                   \
      }                                                                                            \
   }                                                                                               \
   void methodName()

#define WEAK_CALLBACK_DECL_GET_MACRO(_0, _1, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, NAME, ...) NAME
#define WEAK_CALLBACK_DECL(instanceType, methodName, ...) WEAK_CALLBACK_DECL_GET_MACRO             \
   (_0, ##__VA_ARGS__, WEAK_CALLBACK_DECL_6, WEAK_CALLBACK_DECL_6, WEAK_CALLBACK_DECL_5,           \
   WEAK_CALLBACK_DECL_5, WEAK_CALLBACK_DECL_4, WEAK_CALLBACK_DECL_4, WEAK_CALLBACK_DECL_3,         \
   WEAK_CALLBACK_DECL_3, WEAK_CALLBACK_DECL_2, WEAK_CALLBACK_DECL_2, WEAK_CALLBACK_DECL_1,         \
   WEAK_CALLBACK_DECL_1, WEAK_CALLBACK_DECL_0, WEAK_CALLBACK_DECL_0)(instanceType, methodName,     \
   __VA_ARGS__)
#else
#error WeakCallback.hpp is only supported on Linux
#endif

#endif // CORE_WEAK_CALLBACK_HPP
