package com.example.jura12x12x002.ui

import android.content.Context
import androidx.annotation.StringRes

sealed interface UiText {
    data class DynamicString(val value: String) : UiText

    data class StringResource(
        @StringRes val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText {
        constructor(@StringRes resId: Int, vararg args: Any) : this(resId, args.toList())
    }
}

fun UiText.asString(context: Context): String = when (this) {
    is UiText.DynamicString -> value
    is UiText.StringResource -> {
        val resolvedArgs = args.map { argument ->
            if (argument is UiText) {
                argument.asString(context)
            } else {
                argument
            }
        }.toTypedArray()

        context.getString(resId, *resolvedArgs)
    }
}
