package net.rubygrapefruit.scripto.jvm

internal class StaticField(val owner: TypeDescriptor.ObjectType, val name: String, val type: TypeDescriptor) : ReferenceExpression()
