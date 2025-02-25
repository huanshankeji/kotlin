/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.enums

@SinceKotlin("1.9")
@ExperimentalStdlibApi
@PublishedApi
@kotlin.internal.InlineOnly
internal actual inline fun <T : Enum<T>> enumEntriesIntrinsic(): EnumEntries<T> {
    throw NotImplementedError() // implemented as intrinsic
}