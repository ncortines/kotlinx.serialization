/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlin.reflect.*

/**
 * A builder which registers all its content for polymorphic serialization in the scope of [baseClass].
 * If [baseSerializer] is present, registers it as a serializer for [baseClass] (which will be used if base class is serializable).
 * Subclasses with its serializers can be added via [subclass] or [with].
 *
 * To obtain an instance of this builder, use [SerializersModuleBuilder.polymorphic] DSL function.
 */
public class PolymorphicModuleBuilder<Base : Any> internal constructor(
    private val baseClass: KClass<Base>,
    private val baseSerializer: KSerializer<Base>? = null
) {
    private val subclasses: MutableList<Pair<KClass<out Base>, KSerializer<out Base>>> = mutableListOf()

    /**
     * Adds a [subclass] [serializer] to the resulting module under the initial [baseClass].
     */
    public fun <T : Base> addSubclass(subclass: KClass<T>, serializer: KSerializer<T>) {
        subclasses.add(subclass to serializer)
    }

    /**
     * @see addSubclass
     */
    @Deprecated(
        message = "Use 'subclass(serializer)' instead",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("subclass(serializer)")
    )
    public inline fun <reified T : Base> addSubclass(serializer: KSerializer<T>): Unit =
        addSubclass(T::class, serializer)

    public inline fun <reified T : Base> subclass(serializer: KSerializer<T>): Unit =
        addSubclass(T::class, serializer)

    /**
     * @see addSubclass
     */
    @Deprecated(
        message = "Use 'subclass' instead",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("subclass<T>()")
    )
    @ImplicitReflectionSerializer
    public inline fun <reified T : Base> addSubclass(): Unit = addSubclass(T::class, T::class.serializer())

    /**
     * @see addSubclass
     */
    @ImplicitReflectionSerializer
    public inline fun <reified T : Base> subclass(): Unit = addSubclass(T::class, serializer())

    /**
     * @see addSubclass
     */
    public infix fun <T : Base> KClass<T>.with(serializer: KSerializer<T>): Unit = addSubclass(this, serializer)

    @Suppress("UNCHECKED_CAST")
    internal fun buildTo(builder: SerializersModuleBuilder) {
        if (baseSerializer != null) builder.registerPolymorphicSerializer(baseClass, baseClass, baseSerializer)
        subclasses.forEach { (kclass, serializer) ->
            builder.registerPolymorphicSerializer(
                baseClass,
                kclass as KClass<Base>,
                serializer.cast()
            )
        }
    }

    /**
     * Adds all subtypes of this builder to a new builder with a scope of [newBaseClass].
     *
     * If base type of this module had a serializer, adds it, too.
     *
     * @param newBaseClass A new base polymorphic type. Should be supertype of current [baseClass].
     * @param newBaseClassSerializer Serializer for the new base type, if needed.
     * @return A new builder with subclasses from this and [newBaseClass] as baseClass.
     */
    @Suppress("UNCHECKED_CAST")
    internal fun <NewBase : Any> changeBase(
        newBaseClass: KClass<NewBase>,
        newBaseClassSerializer: KSerializer<NewBase>? = null
    ): PolymorphicModuleBuilder<NewBase> {
        val newModule = PolymorphicModuleBuilder(newBaseClass, newBaseClassSerializer)
        baseSerializer?.let { newModule.addSubclass(baseClass as KClass<NewBase>, baseSerializer.cast()) }
        subclasses.forEach { (k, v) ->
            newModule.addSubclass(k as KClass<NewBase>, v as KSerializer<NewBase>)
        }
        return newModule
    }
}
