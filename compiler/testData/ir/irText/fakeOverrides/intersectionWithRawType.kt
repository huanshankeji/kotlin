// FIR_IDENTICAL
// ENABLE_IR_FAKE_OVERRIDE_GENERATION
// TARGET_BACKEND: JVM_IR
// ISSUE: KT-65493

// FILE: JavaBase.java
import java.util.*;

public class JavaBase {
    public List bar() { return null; };
}

// FILE: main.kt
interface KotlinInterface {
    fun bar(): List<Any?>
}

abstract class Derived: JavaBase(), KotlinInterface
