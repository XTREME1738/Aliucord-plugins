/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

package io.github.juby210.acplugins

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.NestedScrollView
import com.aliucord.Constants
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.fragments.SettingsPage
import com.aliucord.patcher.Hook
import com.aliucord.utils.GsonUtils
import com.aliucord.utils.MDUtils
import com.aliucord.views.Divider
import com.discord.databinding.WidgetChatListActionsBinding
import com.discord.models.message.Message
import com.discord.models.user.CoreUser
import com.discord.utilities.color.ColorCompat
import com.discord.widgets.chat.list.actions.WidgetChatListActions
import com.lytefast.flexinput.R

@AliucordPlugin
@Suppress("unused")
@SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
class ViewRaw : Plugin() {
    init {
        needsResources = true
    }

    class Page(private val message: Message) : SettingsPage() {
        override fun onViewBound(view: View) {
            super.onViewBound(view)

            setActionBarTitle("Raw message by " + CoreUser(message.author).username)
            setActionBarSubtitle("View Raw")

            val context = view.context
            val layout = linearLayout

            val content = message.content
            if (!content.isNullOrEmpty()) {
                layout.addView(TextView(context).apply {
                    text = MDUtils.renderCodeBlock(context, SpannableStringBuilder(), null, content)
                    setTextIsSelectable(true)
                })
                layout.addView(Divider(context))
            }

            layout.addView(TextView(context, null, 0, R.i.UiKit_Settings_Item_Header).apply {
                text = "All Raw Data"
                typeface = ResourcesCompat.getFont(context, Constants.Fonts.whitney_semibold)
                setPadding(0, paddingTop, paddingRight, paddingBottom)
            })
            layout.addView(TextView(context).apply {
                text = MDUtils.renderCodeBlock(context, SpannableStringBuilder(), "js", GsonUtils.toJsonPretty(message))
                setTextIsSelectable(true)
            })
        }
    }

    override fun start(ctx: Context) {
        val icon = ResourcesCompat.getDrawable(
            resources,
            resources.getIdentifier("ic_viewraw", "drawable", "io.github.juby210.acplugins"), null
        ) ?: ctx.resources.getDrawable(R.e.design_password_eye, null).mutate()

        val viewId = View.generateViewId()
        val c = WidgetChatListActions::class.java
        val getBinding = c.getDeclaredMethod("getBinding").apply { isAccessible = true }

        patcher.patch(c.getDeclaredMethod("configureUI", WidgetChatListActions.Model::class.java), Hook {
            val binding = getBinding.invoke(it.thisObject) as WidgetChatListActionsBinding
            val viewRaw = binding.a.findViewById<TextView>(viewId)
            viewRaw.setOnClickListener { e ->
                Utils.openPageWithProxy(e.context, Page((it.args[0] as WidgetChatListActions.Model).message))
            }
        })

        patcher.patch(c, "onViewCreated", arrayOf(View::class.java, Bundle::class.java), Hook {
            val linearLayout = (it.args[0] as NestedScrollView).getChildAt(0) as LinearLayout
            val context = linearLayout.context
            icon.setTint(ColorCompat.getThemedColor(context, R.b.colorInteractiveNormal))
            linearLayout.addView(TextView(context, null, 0, R.i.UiKit_Settings_Item_Icon).apply {
                id = viewId
                text = "View Raw"
                setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null)
            })
        })
    }

    override fun stop(context: Context?) = patcher.unpatchAll()
}
