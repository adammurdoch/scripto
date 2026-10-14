package net.rubygrapefruit.scripto.jvm

import java.lang.constant.ClassDesc

internal class StaticField(val owner: ClassDesc, val name: String, val type: ClassDesc) : ReferenceExpression()
