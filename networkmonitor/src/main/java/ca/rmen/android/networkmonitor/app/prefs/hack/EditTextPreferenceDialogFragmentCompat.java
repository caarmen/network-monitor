package ca.rmen.android.networkmonitor.app.prefs.hack;

import android.os.Bundle;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.support.v7.widget.AppCompatEditText;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;

import ca.rmen.android.networkmonitor.R;

/**
 * Hack to use an AppCompatEditText (not a normal EditText), so that the theming will work.
 * This code was partially obtained from Android Studio's decompilation of
 * EditTextPreferenceDialogFragmentCompat.
 */
public class EditTextPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {
    private EditText mEditText;

    public EditTextPreferenceDialogFragmentCompat() {
    }

    public static EditTextPreferenceDialogFragmentCompat newInstance(String key) {
        EditTextPreferenceDialogFragmentCompat fragment = new EditTextPreferenceDialogFragmentCompat();
        Bundle b = new Bundle(1);
        b.putString("key", key);
        fragment.setArguments(b);
        return fragment;
    }

    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        // This is the fix to make sure EditText is themed: use AppCompatEditText instead of
        // normal EditText:
        mEditText = new AppCompatEditText(view.getContext());
        mEditText.setId(R.id.pref_edit_text);
        mEditText.setText(getEditTextPreference().getText());
        ViewParent oldParent = mEditText.getParent();
        if (oldParent != view) {
            if (oldParent != null) {
                ((ViewGroup) oldParent).removeView(mEditText);
            }

            onAddEditTextToDialogView(view, mEditText);
        }
    }

    private EditTextPreference getEditTextPreference() {
        return (EditTextPreference) getPreference();
    }

    protected boolean needInputMethod() {
        return true;
    }

    protected void onAddEditTextToDialogView(View dialogView, EditText editText) {
        ViewGroup container = (ViewGroup) dialogView.findViewById(android.support.v7.preference.R.id.edittext_container);
        if (container != null) {
            container.addView(editText, -1, -2);
        }
    }

    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String value = mEditText.getText().toString();
            if (getEditTextPreference().callChangeListener(value)) {
                getEditTextPreference().setText(value);
            }
        }
    }
}

