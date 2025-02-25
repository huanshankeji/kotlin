// TARGET_BACKEND: JVM
// !LANGUAGE: +MultiPlatformProjects
// IGNORE_DIAGNOSTIC_API
// IGNORE_REVERSED_RESOLVE
//  Reason: MPP diagnostics are reported differentely in the compiler and AA

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect interface S1
expect interface S2

open <!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED{JVM}!>class A<!> : S1, S2

class B : A()

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt

actual interface S1 {
    fun f() {}
}

actual interface S2 {
    fun f() {}
}
