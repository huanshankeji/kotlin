
// FILE: test.kt

fun box() {
    var x = 2
    while (--x > 0) {
        "OK"
    }

    x = 2
    do {
        "OK"
    } while (--x > 0)
}

// EXPECTATIONS JVM_IR
// test.kt:5 box
// test.kt:6 box
// test.kt:7 box
// test.kt:6 box
// test.kt:10 box
// test.kt:12 box
// test.kt:13 box
// test.kt:12 box
// test.kt:13 box
// test.kt:14 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:6 box
// test.kt:6 box
// test.kt:6 box
// test.kt:6 box
// test.kt:10 box
// test.kt:13 box
// test.kt:13 box
// test.kt:13 box
// test.kt:13 box
// test.kt:14 box

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:5 $box (12, 4)
// test.kt:6 $box (13, 11, 13, 17, 11, 11, 11, 13, 11, 13, 17, 11, 11, 11)
// Primitives.kt:1159 $kotlin.Int__dec-impl (15, 8, 16, 15, 8, 16, 15, 8, 16, 15, 8, 16)
// test.kt:7 $box (8, 8, 8, 8)
// String.kt:141 $kotlin.stringLiteral (17, 28, 17, 17, 28, 17, 17, 28, 17)
// Array.kt:59 $kotlin.Array.get (19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8)
// ThrowHelpers.kt:29 $kotlin.wasm.internal.rangeCheck (6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19)
// ThrowHelpers.kt:30 $kotlin.wasm.internal.rangeCheck (1, 1, 1, 1)
// Array.kt:60 $kotlin.Array.get (15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8)
// String.kt:142 $kotlin.stringLiteral (8, 8, 8)
// String.kt:146 $kotlin.stringLiteral (47, 61, 16, 4)
// String.kt:147 $kotlin.stringLiteral (20, 20, 20, 20, 27, 33, 41, 20, 4)
// String.kt:148 $kotlin.stringLiteral (4, 15, 25, 4)
// Array.kt:74 $kotlin.Array.set (19, 26, 34, 8)
// Array.kt:75 $kotlin.Array.set (8, 20, 27, 16)
// Array.kt:76 $kotlin.Array.set
// String.kt:149 $kotlin.stringLiteral (11, 4)
// test.kt:10 $box (8, 4)
// test.kt:12 $box (8, 8, 8, 8, 8, 8, 8, 8)
// String.kt:143 $kotlin.stringLiteral (15, 8, 15, 8)
// test.kt:13 $box (15, 13, 15, 19, 13, 13, 15, 13, 15, 19, 13, 13)
// test.kt:14 $box
