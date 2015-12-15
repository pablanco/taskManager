package com.artech.controls;

import java.util.List;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.artech.R;
import com.artech.base.controls.IGxControlNotifyEvents;
import com.artech.base.controls.MappedValue;
import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.enums.DataTypes;
import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.compatibility.CompatibilityHelper;
import com.artech.controls.common.EditInput;
import com.artech.controls.common.EditInputDescriptions;
import com.artech.controls.common.HistoryHelper;
import com.artech.controls.common.SuggestionsAdapter;
import com.artech.ui.Anchor;
import com.artech.ui.Coordinator;
import com.artech.utils.KeyboardUtils;

public class GxEditText extends AppCompatAutoCompleteTextView implements IGxEdit, IGxControlNotifyEvents, IGxLocalizable
{
	private Coordinator mCoordinator;
	private LayoutItemDefinition mDefinition;
	private HistoryHelper mHistoryHelper;

	private EditInput mEditInput;

	private boolean mIsSettingText;
	private boolean mIsTextEdited;
	private String mLastText;
	private boolean mUpdateValueAfterTextChangedPending;

	private EnterKeyType mEnterKeyType;
	private String mEnterKeyEvent;

	private static final String ENTER_EVENT_DEFAULT = "<Platform Default>"; //$NON-NLS-1$
	private static final String ENTER_EVENT_NEXT_FIELD = "<Go To Next Field>"; //$NON-NLS-1$
	private static final String ENTER_EVENT_NONE = "<None>"; //$NON-NLS-1$

	private static final String ENTER_CAPTION_NEXT = "Next"; //$NON-NLS-1$
	private static final String ENTER_CAPTION_DONE = "Done"; //$NON-NLS-1$
	private static final String ENTER_CAPTION_GO = "Go"; //$NON-NLS-1$
	private static final String ENTER_CAPTION_SEARCH = "Search"; //$NON-NLS-1$
	private static final String ENTER_CAPTION_SEND = "Send"; //$NON-NLS-1$

	private enum EnterKeyType { Default, NextField, Done, Custom }

	private static final Set<String> SUGGEST_ENABLED = Strings.newSet("OnRequest", "Incremental"); //$NON-NLS-1$ //$NON-NLS-2$

	public GxEditText(Context context, Coordinator coordinator, LayoutItemDefinition definition)
	{
		super(context);
		mCoordinator = coordinator;
		mDefinition = definition;

		mEditInput = EditInput.newEditInput(coordinator, definition);

		setInputType(InputType.TYPE_CLASS_TEXT);
		setSelectAllOnFocus(true);
		setHint(definition.getInviteMessage());
		DataItem di = definition.getDataItem();
		setMaxEms(10);

		mLastText = Strings.EMPTY;

		Integer maximumLength = mEditInput.getEditLength();
		if (maximumLength != null)
			setMaximumLength(maximumLength);

		if (di.getLength() > 0 && DataTypes.isLongCharacter(definition.getDataTypeName().GetDataType()))
		{
			// Longvarchar is assumed to be a text, i.e. with multiline and capitalization.
			setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
			setGravity(Gravity.TOP);
			setHorizontallyScrolling(false);
			setVerticalScrollBarEnabled(true);
		}

		setUpPasswordInput(getInputType());
		setupInputConfiguration();
		setupAutocomplete();
		setupEnterKey();
	}

	public GxEditText(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		mEditInput = EditInput.newEditInput(null, null);
	}

	public GxEditText(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		mEditInput = EditInput.newEditInput(null, null);
	}

	protected LayoutItemDefinition getDefinition()
	{
		return mDefinition;
	}

	@SuppressLint("InlinedApi")
	protected void setUpPasswordInput(int baseInputType)
	{
		if (isPassword())
		{
			if ((baseInputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_NUMBER)
			{
				// Note: TYPE_NUMBER_VARIATION_PASSWORD is only supported on Honeycomb or above,
				// and TYPE_TEXT_VARIATION_PASSWORD does not work in combination with TYPE_CLASS_NUMBER.
				// According to http://stackoverflow.com/questions/2420181/android-numeric-password-field the
				// only way for this to work in Android 2.3 is to use setTransformationMethod(), but beware,
				// it DOES NOT work when the edit field is in landscape and full screen, and it doesn't work
				// in combination with other methods (such as setSingleLine()) either.
				if (CompatibilityHelper.isHoneycomb())
					setInputType(baseInputType | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
				else
					setTransformationMethod(PasswordTransformationMethod.getInstance());
			}
			else
			{
				// For characters (and others) just use the PASSWORD variation.
				setInputType(baseInputType | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			}
		}
	}

	private void setupInputConfiguration()
	{
		if (mDefinition.getControlInfo() != null && !isPassword())
		{
			int flags = 0;

			if (mEditInput.getSupportsAutocorrection())
			{
				// Consider special pictures
				String inputPicture = mDefinition.getDataItem().getInputPicture();
				if (DataTypes.isCharacter(mDefinition.getDataTypeName().GetDataType()))
				{
					// If the field is character type, but the accepted input is only numeric, show the numeric keyboard.
					if (inputPicture != null && inputPicture.matches("9+")) //$NON-NLS-1$
						setInputType(InputType.TYPE_CLASS_NUMBER);

					// If "@!" means Case = True (this should be a value of AutoCapitalization).
					if (inputPicture != null && inputPicture.equals("@!")) //$NON-NLS-1$
						flags |= InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS;
					else
					{
						int autoCapitalization = Services.Strings.parseEnum(mDefinition.getControlInfo().optStringProperty("@AutoCapitalization"), "None", "FirstWord", "EachWord"); // 0, 1, 2. //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						if (autoCapitalization == 1)
							flags |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
						else if (autoCapitalization == 2)
							flags |= InputType.TYPE_TEXT_FLAG_CAP_WORDS;
					}
				}

				boolean autoCorrection = mDefinition.getControlInfo().getBooleanProperty("@AutoCorrection", true); // Default true for compatibility. //$NON-NLS-1$
				flags |= (autoCorrection ? InputType.TYPE_TEXT_FLAG_AUTO_CORRECT : InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			}
			else
			{
				 // Autocorrection or autocapitalization makes no sense for input type = descriptions.
				flags = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
			}

			setInputType(getInputType() | flags);
		}
	}

	private void setupAutocomplete()
	{
		ControlInfo controlInfo = mDefinition.getControlInfo();
		if (controlInfo != null)
		{
			// There are two (exclusive) possibilities for auto-completion:
			if (SUGGEST_ENABLED.contains(controlInfo.optStringProperty("@EditAutocomplete"))) //$NON-NLS-1$
			{
				// 1) Suggestions (based on provider data).
				SuggestionsAdapter adapter = new SuggestionsAdapter(mCoordinator, mDefinition);
				setThreshold(1);
				setAdapter(adapter);
			}
			else if (controlInfo.optBooleanProperty("@Autocomplete")) //$NON-NLS-1$
			{
				// 2) Enable history (based on previously entered values).
				mHistoryHelper = new HistoryHelper(mDefinition);
				updateHistorySuggestions();
			}
		}
	}

	private void setupEnterKey()
	{
		mEnterKeyType = EnterKeyType.Default;

		String enterEvent = mDefinition.optStringProperty("@EnterEvent"); //$NON-NLS-1$
		if (!Strings.hasValue(enterEvent) || ENTER_EVENT_DEFAULT.equals(enterEvent))
			return; // Do nothing, use default behavior (next/done).

		setOnEditorActionListener(mEditorActionListener);

		if (ENTER_EVENT_NEXT_FIELD.equalsIgnoreCase(enterEvent))
		{
			mEnterKeyType = EnterKeyType.NextField;
			setImeOptions(EditorInfo.IME_ACTION_NEXT);
		}
		else if (ENTER_EVENT_NONE.equalsIgnoreCase(enterEvent))
		{
			mEnterKeyType = EnterKeyType.Done;
			setImeOptions(EditorInfo.IME_ACTION_DONE);
		}
		else
		{
			// Run a custom event (remove user event quotes).
			mEnterKeyType = EnterKeyType.Custom;
			mEnterKeyEvent = enterEvent.replace("'", Strings.EMPTY); //$NON-NLS-1$

			// ... and use custom caption.
			String enterCaption = mDefinition.optStringProperty("@EnterKeyCaption"); //$NON-NLS-1$
			Integer imeAction = imeActionFromEnterCaption(enterCaption);
			if (imeAction == null)
				imeAction = EditorInfo.IME_ACTION_DONE; // Unknown caption, but if we don't set an IME action the event will not fire.

			setImeOptions(imeAction);
		}
	}

	private static Integer imeActionFromEnterCaption(String enterCaption)
	{
		if (!Strings.hasValue(enterCaption))
			return null;

		if (ENTER_CAPTION_NEXT.equalsIgnoreCase(enterCaption))
			return EditorInfo.IME_ACTION_NEXT;
		else if (ENTER_CAPTION_DONE.equalsIgnoreCase(enterCaption))
			return EditorInfo.IME_ACTION_DONE;
		else if (ENTER_CAPTION_GO.equalsIgnoreCase(enterCaption))
			return EditorInfo.IME_ACTION_GO;
		else if (ENTER_CAPTION_SEARCH.equalsIgnoreCase(enterCaption))
			return EditorInfo.IME_ACTION_SEARCH;
		else if (ENTER_CAPTION_SEND.equalsIgnoreCase(enterCaption))
			return EditorInfo.IME_ACTION_SEND;
		else
			return null;
	}

	private final OnEditorActionListener mEditorActionListener = new OnEditorActionListener()
	{
		@Override
		@SuppressLint("InlinedApi")
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
		{
			actionId = actionId & EditorInfo.IME_MASK_ACTION;

			if (actionId >= EditorInfo.IME_ACTION_GO && actionId <= EditorInfo.IME_ACTION_PREVIOUS)
				dismissDropDown();

			// The user may be executing the standard action by long-pressing on the enter key.
			boolean isStandardAction = (actionId == EditorInfo.IME_ACTION_NEXT);
			if (CompatibilityHelper.isApiLevel(Build.VERSION_CODES.HONEYCOMB))
				isStandardAction |= (actionId == EditorInfo.IME_ACTION_PREVIOUS);

			if (mEnterKeyType == EnterKeyType.Custom && !isStandardAction)
			{
				// Hide the keyboard.
				KeyboardUtils.hideKeyboard(GxEditText.this);

				mEditInput.setText(getText().toString(), new EditInput.OnMappedAvailable()
				{
					@Override
					public void run(MappedValue mapped)
					{
						// Run the custom event.
						mCoordinator.runAction(mEnterKeyEvent, new Anchor(GxEditText.this));
					}
				});

				return true;
			}

			return false;
		}
	};

	@Override
	public void onEditorAction(int actionCode)
	{
		// Workaround for Android issue: OnEditorActionListener.onEditorAction() is NOT called for IME_ACTION_DONE.
		if ((actionCode & EditorInfo.IME_MASK_ACTION) == EditorInfo.IME_ACTION_DONE)
			dismissDropDown();

		super.onEditorAction(actionCode);
	}

	private void updateHistorySuggestions()
	{
		List<String> values = mHistoryHelper.getValues();
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), R.layout.support_simple_spinner_dropdown_item, values);

		setThreshold(1);
		setAdapter(adapter);
	}

	@Override
	protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect)
	{
		super.onFocusChanged(focused, direction, previouslyFocusedRect);

		if (focused)
		{
			if (mEnterKeyType == EnterKeyType.Default)
			{
				// AutoCompleteTextView doesn't handle the next/done buttons as the TextView does.
				// Try to calculate appropriate IME options in the same way.
				View nextView = focusSearch(FOCUS_DOWN);
				setImeOptions(nextView != null ? EditorInfo.IME_ACTION_NEXT : EditorInfo.IME_ACTION_DONE);
			}
		}
		else
		{
			// Losing focus. Fire ControlValueChanged if value was changed by the user.
			String currentText = getText().toString();
			if (currentText != null && !currentText.equals(mLastText))
			{
				mLastText = currentText;
				mEditInput.setText(currentText, new EditInput.OnMappedAvailable()
				{
					@Override
					public void run(MappedValue mapped)
					{
						if (mCoordinator != null)
							mCoordinator.onValueChanged(GxEditText.this, true);
					}
				});
			}
		}
	}

	@Override
	public void setText(CharSequence text, BufferType type)
	{
		mIsSettingText = true;
		super.setText(text, type);
		mIsSettingText = false;
	}

	public void setTextAsEdited(CharSequence text)
	{
		super.setText(text);
		mIsTextEdited = true;
	}

	@Override
	protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter)
	{
		if (!mIsSettingText)
			mIsTextEdited = true;

		super.onTextChanged(text, start, lengthBefore, lengthAfter);

		if (!mIsSettingText)
		{
			// If 1s passes without further typing, assume the user has finished input.
			final int DELAY_MEANS_STOPPED_TYPING = 1000;
			removeCallbacks(mUpdateValueAfterTextChanged);
			postDelayed(mUpdateValueAfterTextChanged, DELAY_MEANS_STOPPED_TYPING);
			mUpdateValueAfterTextChangedPending = true;
		}
	}

    @Override
	protected void replaceText(CharSequence text)
    {
    	// This runs when the user selects one of the suggestions.
    	super.replaceText(text);
    	mUpdateValueAfterTextChanged.run();
    }

	private final Runnable mUpdateValueAfterTextChanged = new Runnable()
	{
		@Override
		public void run()
		{
			mUpdateValueAfterTextChangedPending = false;
			mEditInput.setText(getText().toString(), new EditInput.OnMappedAvailable()
			{
				@Override
				public void run(MappedValue mapped)
				{
					// When the EditInput reports a new value, call onValueChanged() so dependencies can update
					// e.g. a Dynamic Combo that depends on an Edit with Input Type = Descriptions.
					// However, DO NOT fire ControlValueChanged, as editing isn't finished until focus is lost.
					if (mCoordinator != null && mEditInput instanceof EditInputDescriptions && Strings.hasValue(mapped.value))
						mCoordinator.onValueChanged(GxEditText.this, false);
				}
			});
		}
	};

	@Override
	protected void onDetachedFromWindow()
	{
		removeCallbacks(mUpdateValueAfterTextChanged);
		super.onDetachedFromWindow();
	}

	@Override
	public void notifyEvent(EventType type)
	{
		switch (type)
		{
			case ACTION_CALLED :
			{
				if (mHistoryHelper != null && mIsTextEdited)
				{
					mHistoryHelper.store(mEditInput.getText());
					updateHistorySuggestions(); // Refresh the adapter to include the new value.
				}
			}
		}
	}

	protected void setMaximumLength(int maxLength)
	{
		if (maxLength > 0)
		{
			InputFilter[] FilterArray = new InputFilter[1];
			FilterArray[0] = new InputFilter.LengthFilter(maxLength);
			setFilters(FilterArray);
		}
	}

	protected boolean isPassword()
	{
		if (mDefinition.getControlInfo() != null)
			return mDefinition.getControlInfo().getBooleanProperty("@IsPassword", false);
		else
			return false;
	}

	@Override
	public String getGx_Value()
	{
		if (mUpdateValueAfterTextChangedPending)
		{
			// Force it to run immediately! Otherwise we would be returning stale data.
			removeCallbacks(mUpdateValueAfterTextChanged);
			mUpdateValueAfterTextChanged.run();
		}

		return mEditInput.getValue();
	}

	@Override
	public void setGx_Value(final String value)
	{
		mEditInput.setValue(value, new EditInput.OnMappedAvailable()
		{
			@Override
			public void run(MappedValue input)
			{
				setText(input.value);
				mLastText = input.value;
				mIsTextEdited = false;
			}
		});
	}

	@Override
	public String getGx_Tag()
	{
		return (String)this.getTag();
	}

	@Override
	public void setGx_Tag(String data)
	{
		this.setTag(data);
	}

	@Override
	public void setValueFromIntent(Intent data) { }

	@Override
	public IGxEdit getViewControl()
	{
		return new GxTextView(getContext(), mCoordinator, mDefinition, mEditInput.getValuesFormatter());
	}

	@Override
	public IGxEdit getEditControl()
	{
		return this;
	}

	@Override
	public boolean isEditable()
	{
		return isEnabled(); // Editable when enabled.
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		setFocusable(enabled);
		setFocusableInTouchMode(enabled);
	}

	@Override
	public void onTranslationChanged() {
		setHint(getDefinition().getInviteMessage());
	}
}
