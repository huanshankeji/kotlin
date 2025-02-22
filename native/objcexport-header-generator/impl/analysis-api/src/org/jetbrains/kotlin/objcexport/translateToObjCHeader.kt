/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.objcexport.KtObjCExportHeaderGenerator.QueueElement
import org.jetbrains.kotlin.objcexport.analysisApiUtils.*
import org.jetbrains.kotlin.objcexport.extras.originClassId
import org.jetbrains.kotlin.objcexport.extras.requiresForwardDeclaration
import org.jetbrains.kotlin.psi.KtFile


context(KtAnalysisSession, KtObjCExportSession)
fun translateToObjCHeader(files: List<KtFile>): ObjCHeader {
    val generator = KtObjCExportHeaderGenerator()
    generator.translateAll(files.sortedWith(StableFileOrder).map { QueueElement.File(it) })
    return generator.buildObjCHeader()
}

/**
 * Encapsulates the 'dynamic' nature of the ObjCExport where only during the export phase decisions about
 * 1) Which symbols are to be exported
 * 2) In which order symbols are to be exported
 *
 * can be made.
 *
 * Functions inside this class will have side effects such as mutating the [symbolDeque] or adding results to the [objCStubs]
 */
private class KtObjCExportHeaderGenerator {
    /**
     * Represents all elements that still have to be processed and translated.
     * So far this only includes references to top level entities (such as files or classes):
     * Note: Top level functions and properties will be translated as part of the file.
     * See [translateToObjCTopLevelInterfaceFileFacades]
     */
    private val symbolDeque = ArrayDeque<QueueElement>()

    /**
     * The mutable aggregate of the already translated elements
     */
    private val objCStubs = mutableListOf<ObjCTopLevel>()

    /**
     * An index of all already translated classes. All classes here are also present in [objCStubs]
     */
    private val objCStubsByClassId = hashMapOf<ClassId, ObjCClass?>()

    /**
     * An index of already translated classes (by ObjC name)
     */
    private val objCStubsByClassName = hashMapOf<String, ObjCClass>()

    /**
     * The mutable aggregate of all protocol names that shall later be rendered as forward declarations
     */
    private val objCProtocolForwardDeclarations = mutableSetOf<String>()

    /**
     * The mutable aggregate of all class names that shall later be rendered as forward declarations
     */
    private val objCClassForwardDeclarations = mutableSetOf<String>()

    /**
     * See [symbolDeque]:
     * All top level 'to do' elements will be represented as [QueueElement] and later handled by the [translateAll] function.
     */
    sealed class QueueElement {
        class File(val psi: KtFile) : QueueElement()
        class Class(val classId: ClassId) : QueueElement()
    }

    context(KtAnalysisSession, KtObjCExportSession)
    fun translateAll(symbolProviders: List<QueueElement>) {
        symbolDeque.addAll(symbolProviders)

        while (true) {
            val symbolProvider = symbolDeque.removeFirstOrNull() ?: break
            translateElement(symbolProvider)
        }
    }

    context(KtAnalysisSession, KtObjCExportSession)
    private fun translateElement(element: QueueElement) = when (element) {
        is QueueElement.Class -> translateClassElement(element)
        is QueueElement.File -> translateFileElement(element)
    }

    context(KtAnalysisSession, KtObjCExportSession)
    private fun translateClassElement(element: QueueElement.Class) {
        val classOrObjectSymbol = getClassOrObjectSymbolByClassId(element.classId) ?: return
        translateClassOrObjectSymbol(classOrObjectSymbol)
    }

    context(KtAnalysisSession, KtObjCExportSession)
    private fun translateFileElement(element: QueueElement.File) {
        val fileSymbol = element.psi.getFileSymbol()
        fileSymbol.getAllClassOrObjectSymbols().sortedWith(StableClassifierOrder).forEach { classOrObjectSymbol ->
            translateClassOrObjectSymbol(classOrObjectSymbol)
        }
        translateFileSymbol(fileSymbol)
    }

    context(KtAnalysisSession, KtObjCExportSession)
    private fun translateFileSymbol(symbol: KtFileSymbol) {
        symbol.getExtensionsFacade()?.let { extensionFacade ->
            objCStubs += extensionFacade
            enqueueDependencyClasses(extensionFacade)
            objCClassForwardDeclarations += extensionFacade.name
        }

        symbol.getTopLevelFacade()?.let { topLevelFacade ->
            objCStubs += topLevelFacade
            enqueueDependencyClasses(topLevelFacade)
        }
    }

    context(KtAnalysisSession, KtObjCExportSession)
    private fun translateClassOrObjectSymbol(symbol: KtClassOrObjectSymbol): ObjCClass? {
        /* No classId, no stubs ¯\_(ツ)_/¯ */
        val classId = symbol.classIdIfNonLocal ?: return null

        /* Already processed this class, therefore nothing to do! */
        if (classId in objCStubsByClassId) return objCStubsByClassId[classId]

        /**
         * Translate: Note: Even if the result was 'null', the classId will still be marked as 'handled' by adding it
         * to the [objCStubsByClassId] index.
         */
        val objCClass = symbol.translateToObjCExportStub()
        objCStubsByClassId[classId] = objCClass
        objCClass ?: return null

        /*
        To replicate the translation (and result stub order) of the K1 implementation:
        1) Super interface / superclass symbols have to be translated right away
        2) Super interface / superclass symbol export stubs (result of translation) have to be present in the stubs list before the
        original stub
         */
        symbol.getDeclaredSuperInterfaceSymbols().forEach { superInterfaceSymbol ->
            translateClassOrObjectSymbol(superInterfaceSymbol)?.let {
                objCProtocolForwardDeclarations += it.name
            }
        }

        symbol.getSuperClassSymbolNotAny()?.let { superClassSymbol ->
            translateClassOrObjectSymbol(superClassSymbol)?.let {
                objCClassForwardDeclarations += it.name
            }
        }


        /* Note: It is important to add *this* stub to the result list only after translating/processing the superclass symbols */
        objCStubs += objCClass
        objCStubsByClassName[objCClass.name] = objCClass
        enqueueDependencyClasses(objCClass)
        return objCClass
    }

    /**
     * Will introspect the given [stub] to collect all used 'dependency' types/classes.
     * Example: Usage of Kotlin Stdlib Type (Array):
     *
     * ```
     * class Foo {
     *      fun createArray(): Array<String> = error("stub")
     * }
     * ```
     *
     * The given symbol "Foo" will reference `Array`. Therefore, the `Array` class has to be translated as well (later)
     * and `Array` has to be registered as forward declaration.
     */
    private fun enqueueDependencyClasses(stub: ObjCExportStub) {
        symbolDeque += stub.closureSequence()
            .mapNotNull { child ->
                when (child) {
                    is ObjCMethod -> child.returnType
                    is ObjCParameter -> child.type
                    is ObjCProperty -> child.type
                    is ObjCTopLevel -> null
                }
            }
            .flatMap { type ->
                if (type is ObjCClassType) type.typeArguments + type
                else listOf(type)
            }
            .filterIsInstance<ObjCReferenceType>()
            .onEach { type ->
                if (!type.requiresForwardDeclaration) return@onEach
                if (type is ObjCClassType) objCClassForwardDeclarations += type.className
                if (type is ObjCProtocolType) objCProtocolForwardDeclarations += type.protocolName
            }
            .mapNotNull { it.originClassId }
            .map(QueueElement::Class).toList()
    }

    /**
     * [objCClassForwardDeclarations] are recorded by their respective class name:
     * This method will resolve the objc interface that was translated, which is associated with the [className] and
     * build the respective [ObjCClassForwardDeclaration] from it.
     *
     * If no such class was explicitly translated a simple [ObjCClassForwardDeclaration] will be emitted that does not
     * carry any generics.
     */
    private fun resolveObjCClassForwardDeclaration(className: String): ObjCClassForwardDeclaration {
        objCStubsByClassName[className]
            .let { it as? ObjCInterface }
            ?.let { objCClass -> return ObjCClassForwardDeclaration(objCClass.name, objCClass.generics) }

        return ObjCClassForwardDeclaration(className)
    }


    context(KtAnalysisSession, KtObjCExportSession)
    fun buildObjCHeader(): ObjCHeader {
        val hasErrorTypes = objCStubs.hasErrorTypes()

        val protocolForwardDeclarations = objCProtocolForwardDeclarations.toSet()

        val classForwardDeclarations = objCClassForwardDeclarations
            .map { className -> resolveObjCClassForwardDeclaration(className) }
            .toSet()

        val stubs = (if (configuration.generateBaseDeclarationStubs) objCBaseDeclarations() else emptyList()).plus(objCStubs)
            .plus(listOfNotNull(errorInterface.takeIf { hasErrorTypes }))

        return ObjCHeader(
            stubs = stubs,
            classForwardDeclarations = classForwardDeclarations,
            protocolForwardDeclarations = protocolForwardDeclarations,
            additionalImports = emptyList()
        )
    }
}
