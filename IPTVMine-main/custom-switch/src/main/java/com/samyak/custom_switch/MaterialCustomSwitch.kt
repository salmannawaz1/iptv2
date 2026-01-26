/*
 * Created by Samyak Kamble on 11/23/25, 09:20 PM
 *  Copyright (c) 2024. All rights reserved.
 *  Last modified 12/12/25, 10:00 PM
 */
package com.samyak.custom_switch

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import com.google.android.material.materialswitch.MaterialSwitch

class MaterialCustomSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    private var textHead: String = ""
    private var textOn: String = ""
    private var textOff: String = ""
    private var checked: Boolean = false
    
    // Text colors
    @ColorInt private var textHeadColor: Int = Color.WHITE
    @ColorInt private var textDescColor: Int = Color.GRAY
    @ColorInt private var textOnColor: Int = Color.GREEN
    @ColorInt private var textOffColor: Int = Color.GRAY

    private val textHeadView: TextView
    private val textDescView: TextView
    private val materialSwitch: MaterialSwitch

    private var onCheckChangedListener: OnCheckChangeListener? = null

    init {
        inflate(context, R.layout.material_custom_switch, this)

        textHeadView = findViewById(R.id.text_head)
        textDescView = findViewById(R.id.text_desc)
        materialSwitch = findViewById(R.id.materialSwitch)

        findViewById<LinearLayout>(R.id.root).setOnClickListener {
            materialSwitch.toggle()
        }

        materialSwitch.setOnCheckedChangeListener { _, isChecked ->
            textDescView.text = if (isChecked) textOn else textOff
            updateDescriptionColor(isChecked)
            onCheckChangedListener?.onCheckChanged(isChecked)
        }

        if (attrs != null) {
            val a = context.obtainStyledAttributes(
                attrs,
                R.styleable.MaterialCustomSwitch,
                defStyle,
                0
            )

            textHead = a.getString(R.styleable.MaterialCustomSwitch_textHead) ?: ""
            textOn = a.getString(R.styleable.MaterialCustomSwitch_textOn) ?: ""
            textOff = a.getString(R.styleable.MaterialCustomSwitch_textOff) ?: ""
            checked = a.getBoolean(R.styleable.MaterialCustomSwitch_checked, false)
            
            // Get text colors
            textHeadColor = a.getColor(R.styleable.MaterialCustomSwitch_textHeadColor, Color.WHITE)
            textDescColor = a.getColor(R.styleable.MaterialCustomSwitch_textDescColor, Color.GRAY)
            textOnColor = a.getColor(R.styleable.MaterialCustomSwitch_textOnColor, textDescColor)
            textOffColor = a.getColor(R.styleable.MaterialCustomSwitch_textOffColor, textDescColor)

            textHeadView.text = textHead
            textDescView.text = if (checked) textOn else textOff
            materialSwitch.isChecked = checked
            
            // Apply text colors
            textHeadView.setTextColor(textHeadColor)
            updateDescriptionColor(checked)

            a.recycle()
        }
    }
    
    private fun updateDescriptionColor(isChecked: Boolean) {
        textDescView.setTextColor(if (isChecked) textOnColor else textOffColor)
    }

    fun setOnCheckChangeListener(listener: OnCheckChangeListener) {
        this.onCheckChangedListener = listener
    }

    fun setChecked(value: Boolean) {
        materialSwitch.isChecked = value
    }
    
    fun isChecked(): Boolean {
        return materialSwitch.isChecked
    }
    
    // Programmatic setters for text colors
    fun setTextHeadColor(@ColorInt color: Int) {
        textHeadColor = color
        textHeadView.setTextColor(color)
    }
    
    fun setTextDescColor(@ColorInt color: Int) {
        textDescColor = color
        textOnColor = color
        textOffColor = color
        updateDescriptionColor(materialSwitch.isChecked)
    }
    
    fun setTextOnColor(@ColorInt color: Int) {
        textOnColor = color
        if (materialSwitch.isChecked) {
            textDescView.setTextColor(color)
        }
    }
    
    fun setTextOffColor(@ColorInt color: Int) {
        textOffColor = color
        if (!materialSwitch.isChecked) {
            textDescView.setTextColor(color)
        }
    }
    
    // Setters for text content
    fun setTextHead(text: String) {
        textHead = text
        textHeadView.text = text
    }
    
    fun setTextOn(text: String) {
        textOn = text
        if (materialSwitch.isChecked) {
            textDescView.text = text
        }
    }
    
    fun setTextOff(text: String) {
        textOff = text
        if (!materialSwitch.isChecked) {
            textDescView.text = text
        }
    }

    interface OnCheckChangeListener {
        fun onCheckChanged(isChecked: Boolean)
    }
}
