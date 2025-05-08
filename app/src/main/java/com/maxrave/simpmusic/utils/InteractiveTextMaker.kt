package com.maxrave.simpmusic.utils

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.FontRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils

class InteractiveTextMaker private constructor(
    private val textView: TextView,
    private val context: Context,
) {
    private var onTextClicked: ((text: String) -> Unit) = {}
    private var specialTextColor: Int = textView.currentTextColor
    private var specialTextHighLightColor: Int = ColorUtils.setAlphaComponent(specialTextColor, 50)
    private var specialTextFontFamily = textView.typeface
    private var specialTextSize = textView.textSize
    private var underlinedSpecialText: Boolean = false

    fun setSpecialTextSize(size: Float): InteractiveTextMaker {
        specialTextSize = size
        return this
    }

    fun setSpecialTextFontFamilyRes(
        @FontRes fontId: Int,
    ): InteractiveTextMaker {
        specialTextFontFamily = ResourcesCompat.getFont(context, fontId)!!
        return this
    }

    fun setSpecialTextFontFamily(font: Typeface): InteractiveTextMaker {
        specialTextFontFamily = font
        return this
    }

    fun setSpecialTextUnderLined(boolean: Boolean): InteractiveTextMaker {
        underlinedSpecialText = boolean
        return this
    }

    fun setSpecialTextColorRes(
        @ColorRes colorId: Int,
    ): InteractiveTextMaker {
        val color = ContextCompat.getColor(context, colorId)
        specialTextColor = color
        specialTextHighLightColor = ColorUtils.setAlphaComponent(color, 50)
        return this
    }

    fun setSpecialTextColor(colorInt: Int): InteractiveTextMaker {
        specialTextColor = colorInt
        specialTextHighLightColor = ColorUtils.setAlphaComponent(colorInt, 50)
        return this
    }

    fun setSpecialTextHighlightColor(color: Int): InteractiveTextMaker {
        specialTextHighLightColor = color
        return this
    }

    fun setSpecialTextHighlightColorRes(
        @ColorRes color: Int,
    ): InteractiveTextMaker {
        specialTextHighLightColor = ContextCompat.getColor(context, color)
        return this
    }

    fun setOnTextClickListener(func: (text: String) -> Unit): InteractiveTextMaker {
        onTextClicked = func
        return this
    }

    fun initialize() {
        val regex = Regex("""(\d+):(\d+)(?::(\d+))?""")
        val words = regex.findAll(textView.text)
        val span = SpannableString(textView.text)
        if (words.toList().isEmpty()) {
            Log.w(
                TAG,
                "initialize: WARNING can't found in ${textView.text} to make it interactive.",
            )
            return
        }
        words.forEachIndexed { index: Int, wordResult: MatchResult ->
            val startIndex = wordResult.range.first
            val endIndex = wordResult.range.last + 1
            Log.i(
                TAG,
                "initialize: text:'${textView.text}' startIndex:$startIndex endIndex:$endIndex",
            )
            span.setSpan(
                object : ClickableSpan() {
                    override fun onClick(p0: View) {
                        onTextClicked(wordResult.value)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = underlinedSpecialText
                        ds.textSize = specialTextSize
                        ds.typeface = specialTextFontFamily
                        ds.color = specialTextColor
                    }
                },
                startIndex,
                endIndex,
                0,
            )
            textView.linksClickable = true
            textView.isClickable = true
            textView.movementMethod = LinkMovementMethod.getInstance()
            textView.highlightColor = specialTextHighLightColor
            textView.text = span
        }
    }

    companion object {
        fun of(textView: TextView): InteractiveTextMaker = InteractiveTextMaker(textView, textView.context)

        // Will be used at debugging.
        private const val TAG = "InteractiveTextMaker"
    }
}