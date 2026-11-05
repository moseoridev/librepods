package me.kavishdevar.librepods.ui.components

import android.graphics.Typeface
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.text.TextWatcher
import android.util.TypedValue
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt

/** Platform editor owns IME/selection; the supplied TextFieldState owns text and selection values. */
@Composable
internal fun SettingsInputEditor(
    state: TextFieldState, modifier: Modifier, placeholder: String, singleLine: Boolean,
    enabled: Boolean, focusRequester: FocusRequester?, keyboardOptions: KeyboardOptions,
    foreground: Color, hint: Color, cursor: Color, selection: Color, rule: Color, errorMessage: String?,
) {
    val density = LocalDensity.current
    val locales = LocalConfiguration.current.locales
    val fontPixels = with(density) { 17.sp.toPx().roundToInt().toFloat() }
    // InsetDrawable truncates its insets; the focused nine-patch's 4dp padding rounds.
    val horizontal = with(density) { 4.dp.toPx().toInt() }
    val top = with(density) { 10.dp.toPx().toInt() }
    val bottom = with(density) { 7.dp.toPx().toInt() + 4.dp.roundToPx() }
    val cursorWidth = with(density) { 2.dp.roundToPx() }
    key(state) {
        var editor by remember { mutableStateOf<StateEditor?>(null) }
        // Read snapshot values outside AndroidView.update so caller edits cause an update.
        val text = state.text.toString()
        val selectionRange = state.selection
        AndroidView(modifier = modifier
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .onFocusChanged { if (it.isFocused) editor?.requestFocus() }
            .focusTarget(),
            // The reference's application-context editor resolves Widget.Material.Light.EditText.
            factory = { context -> StateEditor(android.view.ContextThemeWrapper(context,
                android.R.style.Theme_Material_Light_NoActionBar), state).apply {
                minimumHeight = 0
                minimumWidth = 0
                typeface = Typeface.SANS_SERIF
                includeFontPadding = true
                if (Build.VERSION.SDK_INT >= 35) setLocalePreferredLineHeightForMinimumUsed(true)
                editor = this
            } },
            update = { view -> view.updateFromCaller {
                view.isEnabled = enabled
                view.validationMessage = errorMessage
                view.applyKeyboardOptions(keyboardOptions, singleLine)
                view.typeface = Typeface.SANS_SERIF
                view.textLocales = locales
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontPixels)
                view.setPadding(horizontal, top, horizontal, bottom)
                view.setTextColor(foreground.toArgb())
                view.backgroundTintList = ColorStateList.valueOf(rule.toArgb())
                view.hint = placeholder
                view.setHintTextColor(hint.toArgb())
                view.highlightColor = selection.toArgb()
                view.textCursorDrawable = GradientDrawable().apply {
                    setColor(cursor.toArgb())
                    setSize(cursorWidth, 0)
                }
                view.sync(text, selectionRange)
            } })
    }
}

private class StateEditor(context: android.content.Context, private val state: TextFieldState) : EditText(context) {
    var validationMessage: String? = null
    private var synchronizing = false
    private var selectionChanged: ((Int, Int) -> Unit)? = null

    init {
        selectionChanged = { start, end ->
            if (!synchronizing && start >= 0 && end >= 0 && text?.toString() == state.text.toString()) {
                state.edit { selection = TextRange(start.coerceIn(0, length), end.coerceIn(0, length)) }
            }
        }
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(text: Editable?) {
                if (!synchronizing) state.edit {
                    replace(0, length, text?.toString().orEmpty())
                    selection = TextRange(selectionStart.coerceIn(0, length), selectionEnd.coerceIn(0, length))
                }
            }
        })
    }

    override fun onSelectionChanged(start: Int, end: Int) {
        super.onSelectionChanged(start, end)
        // TextView also invokes this before our constructor has installed the callback.
        selectionChanged?.invoke(start, end)
    }

    override fun onInitializeAccessibilityNodeInfo(info: android.view.accessibility.AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.isContentInvalid = validationMessage != null
        info.error = validationMessage
    }

    fun sync(value: String, selection: TextRange) {
        if (text?.toString() != value) setText(value)
        val start = selection.start.coerceIn(0, value.length)
        val end = selection.end.coerceIn(0, value.length)
        if (selectionStart != start || selectionEnd != end) setSelection(start, end)
    }

    fun updateFromCaller(block: StateEditor.() -> Unit) {
        synchronizing = true
        try { block() } finally { synchronizing = false }
    }

    fun applyKeyboardOptions(options: KeyboardOptions, singleLine: Boolean) {
        var type = when (options.keyboardType) {
            KeyboardType.Number -> InputType.TYPE_CLASS_NUMBER
            KeyboardType.Decimal -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            KeyboardType.NumberPassword -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            KeyboardType.Phone -> InputType.TYPE_CLASS_PHONE
            KeyboardType.Uri -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            KeyboardType.Email -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            KeyboardType.Password -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            else -> InputType.TYPE_CLASS_TEXT
        }
        if (type and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT) {
            type = type or when (options.capitalization) {
                KeyboardCapitalization.Characters -> InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                KeyboardCapitalization.Words -> InputType.TYPE_TEXT_FLAG_CAP_WORDS
                KeyboardCapitalization.Sentences -> InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                else -> 0
            }
            if (options.autoCorrectEnabled != false) type = type or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
            if (!singleLine) type = type or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        if (inputType != type) inputType = type
        setSingleLine(singleLine)
        // setSingleLine resets TextView's transformation even when inputType did
        // not change. Reapply masking on every caller update for password inputs.
        if (options.keyboardType == KeyboardType.Password || options.keyboardType == KeyboardType.NumberPassword) {
            transformationMethod = PasswordTransformationMethod.getInstance()
        }
        imeOptions = when (options.imeAction) {
            ImeAction.Done -> EditorInfo.IME_ACTION_DONE
            ImeAction.Go -> EditorInfo.IME_ACTION_GO
            ImeAction.Next -> EditorInfo.IME_ACTION_NEXT
            ImeAction.Previous -> EditorInfo.IME_ACTION_PREVIOUS
            ImeAction.Search -> EditorInfo.IME_ACTION_SEARCH
            ImeAction.Send -> EditorInfo.IME_ACTION_SEND
            ImeAction.None -> EditorInfo.IME_ACTION_NONE
            else -> EditorInfo.IME_ACTION_UNSPECIFIED
        } or if (options.keyboardType == KeyboardType.Ascii) EditorInfo.IME_FLAG_FORCE_ASCII else 0
        showSoftInputOnFocus = options.showKeyboardOnFocus != false
        imeHintLocales = options.hintLocales?.let {
            android.os.LocaleList.forLanguageTags(it.joinToString(",") { locale -> locale.toLanguageTag() })
        }
        privateImeOptions = options.platformImeOptions?.privateImeOptions
    }
}
