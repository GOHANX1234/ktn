package com.android.support

/**
 * Sealed class hierarchy representing every widget type that the native
 * [Menu.GetFeatureList] / [Menu.SettingsList] string arrays can describe.
 *
 * Each item carries exactly the data its Compose composable needs.
 */
sealed class FeatureItem {
    abstract val featNum: Int

    /** Toggle_Name[_True] */
    data class Toggle(
        override val featNum: Int,
        val name: String,
        val defaultOn: Boolean,
    ) : FeatureItem()

    /** SeekBar_Name_Min_Max */
    data class SeekBarItem(
        override val featNum: Int,
        val name: String,
        val min: Int,
        val max: Int,
    ) : FeatureItem()

    /** Button_Name */
    data class ButtonItem(
        override val featNum: Int,
        val name: String,
    ) : FeatureItem()

    /** ButtonOnOff_Name[_True] */
    data class ButtonOnOff(
        override val featNum: Int,
        val name: String,
        val defaultOn: Boolean,
    ) : FeatureItem()

    /** Spinner_Name_Opt1,Opt2,… */
    data class SpinnerItem(
        override val featNum: Int,
        val name: String,
        val options: List<String>,
    ) : FeatureItem()

    /** InputText_Name */
    data class InputText(
        override val featNum: Int,
        val name: String,
    ) : FeatureItem()

    /** InputValue_Name  or  InputValue_MaxVal_Name */
    data class InputValue(
        override val featNum: Int,
        val name: String,
        val maxValue: Int,
    ) : FeatureItem()

    /** CheckBox_Name[_True] */
    data class CheckBoxItem(
        override val featNum: Int,
        val name: String,
        val defaultOn: Boolean,
    ) : FeatureItem()

    /** RadioButton_Name_Opt1,Opt2,… */
    data class RadioButtonGroup(
        override val featNum: Int,
        val name: String,
        val options: List<String>,
    ) : FeatureItem()

    /** Collapse_Label[_True] — collapsible section; children collected via CollapseAdd_ prefix */
    data class CollapseSection(
        val text: String,
        val startExpanded: Boolean,
        val children: List<FeatureItem>,
    ) : FeatureItem() {
        override val featNum: Int = -999
    }

    /** ButtonLink_Label_URL */
    data class ButtonLink(
        val name: String,
        val url: String,
    ) : FeatureItem() {
        override val featNum: Int = -998
    }

    /** Category_Label */
    data class CategoryLabel(val text: String) : FeatureItem() {
        override val featNum: Int = -997
    }

    /** RichTextView_Label */
    data class RichTextLabel(val text: String) : FeatureItem() {
        override val featNum: Int = -996
    }

    /** RichWebView_HtmlString */
    data class RichWebLabel(val text: String) : FeatureItem() {
        override val featNum: Int = -995
    }
}

/**
 * Parses the raw string array returned by GetFeatureList() / SettingsList()
 * into a typed list of [FeatureItem]s, replicating the exact same logic as
 * the original Java featureList() method in Menu.java.
 */
fun parseFeatureList(listFT: Array<String>): List<FeatureItem> {
    val result  = mutableListOf<FeatureItem>()
    var subFeat = 0
    var currentCollapseChildren: MutableList<FeatureItem>? = null

    for (i in listFT.indices) {
        var switchedOn = false
        var feature = listFT[i]

        // _True suffix means the feature defaults to ON
        if (feature.contains("_True")) {
            switchedOn = true
            feature = feature.replaceFirst("_True", "")
        }

        // CollapseAdd_ prefix routes this item into the active collapse section
        val isCollapseAdd = feature.contains("CollapseAdd_")
        if (isCollapseAdd) feature = feature.replaceFirst("CollapseAdd_", "")

        // Numeric first token = explicit feature number
        val firstToken = feature.split("_")[0]
        val featNum: Int
        if (firstToken.matches(Regex("-?[0-9]+"))) {
            featNum = firstToken.toInt()
            feature = feature.replaceFirst("${firstToken}_", "")
            subFeat++
        } else {
            featNum = i - subFeat
        }

        val parts  = feature.split("_")
        val target = if (isCollapseAdd) (currentCollapseChildren ?: result) else result

        when (parts[0]) {
            "Toggle"       -> target += FeatureItem.Toggle(featNum, parts[1], switchedOn)
            "SeekBar"      -> target += FeatureItem.SeekBarItem(featNum, parts[1], parts[2].toInt(), parts[3].toInt())
            "Button"       -> target += FeatureItem.ButtonItem(featNum, parts[1])
            "ButtonOnOff"  -> target += FeatureItem.ButtonOnOff(featNum, parts[1], switchedOn)
            "Spinner"      -> target += FeatureItem.SpinnerItem(featNum, parts[1], parts[2].split(","))
            "InputText"    -> target += FeatureItem.InputText(featNum, parts[1])
            "InputValue"   -> target += when (parts.size) {
                3    -> FeatureItem.InputValue(featNum, parts[2], parts[1].toInt())
                else -> FeatureItem.InputValue(featNum, parts[1], 0)
            }
            "CheckBox"     -> target += FeatureItem.CheckBoxItem(featNum, parts[1], switchedOn)
            "RadioButton"  -> target += FeatureItem.RadioButtonGroup(featNum, parts[1], parts[2].split(","))
            "Collapse"     -> {
                subFeat++
                val children = mutableListOf<FeatureItem>()
                currentCollapseChildren = children
                result += FeatureItem.CollapseSection(parts[1], switchedOn, children)
            }
            "ButtonLink"   -> { subFeat++; target += FeatureItem.ButtonLink(parts[1], parts[2]) }
            "Category"     -> { subFeat++; target += FeatureItem.CategoryLabel(parts[1]) }
            "RichTextView" -> { subFeat++; target += FeatureItem.RichTextLabel(parts[1]) }
            "RichWebView"  -> { subFeat++; target += FeatureItem.RichWebLabel(parts[1]) }
        }
    }
    return result
}
