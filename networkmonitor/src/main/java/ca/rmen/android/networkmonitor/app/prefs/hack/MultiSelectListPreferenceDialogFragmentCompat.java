package ca.rmen.android.networkmonitor.app.prefs.hack;

import android.os.Bundle;
import androidx.preference.MultiSelectListPreference;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.DialogPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;

import java.util.HashSet;
import java.util.Set;

/**
 * This class is a hack to allow us to use a MultiSelectListPreference with the v7
 * PreferenceCompat library.
 * This class will not actually function on older devices (< API 11 for sure, not sure if
 * it will work on API 12 and 13).
 * <p/>
 * It may be possible to avoid this hack, and to use only v14 preference classes (ie
 * PreferenceFragment instead of PreferenceFragmentCompat), but they may be more difficult
 * to theme.  I noticed that the "ok" and "cancel" buttons of the preference dialogs
 * were not in my app's theme.  The theme works with PreferenceFragmentCompat (v7).
 * <p/>
 * This code was partially obtained from Android Studio's decompiling of
 * MultiSelectListPreferenceDialogFragment.
 */
public class MultiSelectListPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat
        implements DialogPreference.TargetFragment {
    private final Set<String> mNewValues = new HashSet<>();
    private boolean mPreferenceChanged;

    public MultiSelectListPreferenceDialogFragmentCompat() {
    }

    public static MultiSelectListPreferenceDialogFragmentCompat newInstance(String key) {
        MultiSelectListPreferenceDialogFragmentCompat fragment = new MultiSelectListPreferenceDialogFragmentCompat();
        Bundle b = new Bundle(1);
        b.putString("key", key);
        fragment.setArguments(b);
        return fragment;
    }

    /**
     * We set the AlertDialog to display a multi-choice list.  We register a listener: when the user
     * checks or un-checks values, we store the list of all selected values.
     */
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        final MultiSelectListPreference preference = getListPreference();
        if (preference.getEntries() != null && preference.getEntryValues() != null) {
            boolean[] checkedItems = getSelectedItems();
            builder.setMultiChoiceItems(preference.getEntries(), checkedItems, (dialog, which, isChecked) -> {
                mPreferenceChanged = true;
                if (isChecked) {
                    mNewValues.add(preference.getEntryValues()[which].toString());
                } else {
                    mNewValues.remove(preference.getEntryValues()[which].toString());
                }
            });
            mNewValues.clear();
            mNewValues.addAll(preference.getValues());
        } else {
            throw new IllegalStateException("MultiSelectListPreference requires an entries array and an entryValues array.");
        }
    }

    /**
     * If the user tapped on "ok", we update our Preference's selected values, based on the values
     * our alert dialog listener saved.
     * @param positiveResult true if the user tapped on "ok".
     */
    public void onDialogClosed(boolean positiveResult) {
        MultiSelectListPreference preference = getListPreference();
        if (positiveResult && mPreferenceChanged) {
            Set<String> values = mNewValues;
            if (preference.callChangeListener(values)) {
                preference.setValues(values);
            }
        }
        mPreferenceChanged = false;
    }

    /**
     * @return a list of the checked/unchecked state of all the list entries.
     */
    private boolean[] getSelectedItems() {
        MultiSelectListPreference preference = getListPreference();
        CharSequence[] entries = preference.getEntryValues();
        Set<String> values = preference.getValues();
        boolean[] result = new boolean[entries.length];
        for (int i = 0; i < entries.length; i++) {
            result[i] = values.contains(entries[i].toString());
        }
        return result;
    }

    private MultiSelectListPreference getListPreference() {
        return (MultiSelectListPreference) getPreference();
    }

    @Override
    public Preference findPreference(CharSequence charSequence) {
        return getPreference();
    }
}

