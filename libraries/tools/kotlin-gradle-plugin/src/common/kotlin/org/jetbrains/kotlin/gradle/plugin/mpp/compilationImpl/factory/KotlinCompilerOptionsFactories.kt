/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory

import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.baseModuleName
import org.jetbrains.kotlin.gradle.plugin.mpp.moduleNameForCompilation
import org.jetbrains.kotlin.gradle.targets.native.NativeCompilerOptions
import org.jetbrains.kotlin.gradle.utils.configureExperimentalTryNext
import org.jetbrains.kotlin.gradle.utils.klibModuleName

internal object KotlinMultiplatformCommonCompilerOptionsFactory : KotlinCompilationImplFactory.KotlinCompilerOptionsFactory {
    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options {
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
        val compilerOptions = object : DeprecatedHasCompilerOptions<KotlinMultiplatformCommonCompilerOptions> {
            override val options: KotlinMultiplatformCommonCompilerOptions = target.project.objects
                .newInstance(KotlinMultiplatformCommonCompilerOptionsDefault::class.java)
                .configureExperimentalTryNext(target.project)
        }

        val kotlinOptions = object : KotlinCommonOptions {
            override val options: KotlinCommonCompilerOptions
                get() = compilerOptions.options
        }

        return KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options(compilerOptions, kotlinOptions)
    }
}

internal object KotlinNativeCompilerOptionsFactory : KotlinCompilationImplFactory.KotlinCompilerOptionsFactory {

    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options {
        val compilerOptions = NativeCompilerOptions(target.project)
        compilerOptions.options.moduleName.convention(
            target.project.klibModuleName(
                moduleNameForCompilation(
                    compilationName,
                    target.project.baseModuleName()
                )
            )
        )

        val kotlinOptions = object : KotlinCommonOptions {
            override val options get() = compilerOptions.options
        }

        return KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options(compilerOptions, kotlinOptions)
    }
}


internal object KotlinJsCompilerOptionsFactory : KotlinCompilationImplFactory.KotlinCompilerOptionsFactory {
    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options {
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
        val compilerOptions = object : DeprecatedHasCompilerOptions<KotlinJsCompilerOptions> {
            override val options: KotlinJsCompilerOptions = target.project.objects
                .newInstance(KotlinJsCompilerOptionsDefault::class.java)
                .configureExperimentalTryNext(target.project)
        }

        val kotlinOptions = object : KotlinJsOptions {
            override val options: KotlinJsCompilerOptions
                get() = compilerOptions.options
        }

        return KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options(compilerOptions, kotlinOptions)
    }
}

internal object KotlinJvmCompilerOptionsFactory : KotlinCompilationImplFactory.KotlinCompilerOptionsFactory {
    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options {
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
        val compilerOptions = object : DeprecatedHasCompilerOptions<KotlinJvmCompilerOptions> {
            override val options: KotlinJvmCompilerOptions = target.project.objects
                .newInstance(KotlinJvmCompilerOptionsDefault::class.java)
                .configureExperimentalTryNext(target.project)
        }

        val kotlinOptions = object : KotlinJvmOptions {
            override val options: KotlinJvmCompilerOptions
                get() = compilerOptions.options
        }

        return KotlinCompilationImplFactory.KotlinCompilerOptionsFactory.Options(compilerOptions, kotlinOptions)
    }
}


