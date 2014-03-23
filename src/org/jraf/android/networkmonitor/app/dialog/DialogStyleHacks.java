/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014 Carmen Alvarez (c@rmen.ca)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jraf.android.networkmonitor.app.dialog;

import java.lang.reflect.Field;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.graphics.NinePatch;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListView;

import org.jraf.android.networkmonitor.Constants;

/**
 * Boy is customizing alert dialogs a pain in the booty. Tried the android-styled-dialogs library but it didn't fit the needs of this app: no support for alert
 * dialogs with EditTexts, and not a clean way to manage clicks on the dialog buttons. Started out trying to copy the resources used for dialogs, one-by-one,
 * from the core android framework, but that was more pain than the approach I decided to take in this class.
 */
class DialogStyleHacks {

    private final String TAG = Constants.TAG + "/" + DialogStyleHacks.class.getSimpleName();
    private static int sHoloBlueLightColorId = -1;
    private static int sHoloBlueDarkColorId = -1;
    private static Field sNinePatchSourceField = null;
    private static Field sNinePatchField = null;

    private final int mDialogStyleId;
    private final int mMyAppColorId;
    private final int mHorizontalDividerDrawableId;
    private final Context mContext;

    /**
     * @param dialogStyleId refers to a style which defines all the styling attributes for the dialog.
     * @param myAppColorId will be used for the dialog title text and, depending on the OS version (4.x), the horizontal divider.
     * @param horizontalDividerDrawableId the drawable to use for the horizontal divider for 2.x and 3.x.
     * 
     */
    DialogStyleHacks(Context context, int dialogStyleId, int myAppColorId, int horizontalDividerDrawableId) {
        mContext = context;
        mDialogStyleId = dialogStyleId;
        mMyAppColorId = mContext.getResources().getColor(myAppColorId);
        mHorizontalDividerDrawableId = horizontalDividerDrawableId;
    }

    /**
     * @param dialog apply our custom theme to the given dialog, once the dialog is visible.
     */
    public void styleDialog(final AlertDialog dialog) {
        dialog.getContext().setTheme(mDialogStyleId);
        if (dialog.isShowing()) styleDialogElements(dialog);
        else
            dialog.setOnShowListener(new OnShowListener() {

                @Override
                public void onShow(DialogInterface dialogInterface) {
                    styleDialogElements(dialog);
                }
            });
    }

    /**
     * Update colors and other attributes of the different views contained within this dialog.
     */
    private void styleDialogElements(AlertDialog dialog) {
        ListView listView = dialog.getListView();
        int listViewStyle = Attributes.getResourceIdStyleAttribute(mContext, mDialogStyleId, android.R.attr.listViewStyle);
        Drawable listSelector = Attributes.getDrawableStyleAttribute(mContext, listViewStyle, android.R.attr.listSelector);
        if (listView != null) listView.setSelector(listSelector);
        Button button = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        if (button != null) styleButton(button);
        button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) styleButton(button);
        button = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        if (button != null) styleButton(button);
        uglyHackReplaceBlueHoloBackground((ViewGroup) dialog.getWindow().getDecorView(), dialog);
    }

    /**
     * Set the min height, min width, text color, and background drawable of this button based on our ButtonNetMon style.
     */
    private void styleButton(Button button) {
        int buttonStyleId = Attributes.getResourceIdStyleAttribute(mContext, mDialogStyleId, android.R.attr.buttonStyle);
        int minWidth = Attributes.getDimensionStyleAttribute(mContext, buttonStyleId, android.R.attr.minWidth);
        int minHeight = Attributes.getDimensionStyleAttribute(mContext, buttonStyleId, android.R.attr.minHeight);
        int textColor = Attributes.getColorStyleAttribute(mContext, buttonStyleId, android.R.attr.textColor);
        int backgroundId = Attributes.getResourceIdStyleAttribute(mContext, buttonStyleId, android.R.attr.background);
        button.setMinHeight(minHeight);
        button.setMinWidth(minWidth);
        button.setBackgroundResource(backgroundId);
        button.setTextColor(textColor);
    }

    /**
     * Iterate through the whole view tree and replace the holo blue element(s) with our holo color.
     * For 2.x, the horizontal divider is a nine patch image "divider_horizontal_dark".
     * For 3.x, the horizontal divider is a nine patch image "divider_strong_holo".
     * For 4.x, the horizontal divider is a holo color.
     */
    private void uglyHackReplaceBlueHoloBackground(ViewGroup viewGroup, AlertDialog dialog) {
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof ViewGroup) {
                uglyHackReplaceDialogCorners((ViewGroup) child);
                uglyHackReplaceBlueHoloBackground((ViewGroup) child, dialog);
            }
            // 2.x and 3.x: replace the nine patch horizontal divider
            else if (child instanceof ImageView) {
                ImageView imageView = (ImageView) child;
                Drawable drawable = imageView.getDrawable();
                if (drawable instanceof NinePatchDrawable) {
                    if (isHoloBlueNinePatch((NinePatchDrawable) drawable)) {
                        imageView.setImageResource(mHorizontalDividerDrawableId);
                        // On 2.x, in a dialog with a list, the divider is hidden.  Let's show it.
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB && !(dialog instanceof ProgressDialog))
                            imageView.setVisibility(View.VISIBLE);
                    }
                }
            }
            // replace the radio button
            else if (child instanceof CheckedTextView) {
                Drawable radioButtonDrawable = Attributes.getDrawableStyleAttribute(mContext, mDialogStyleId, android.R.attr.listChoiceIndicatorSingle);
                ((CheckedTextView) child).setCheckMarkDrawable(radioButtonDrawable);
            }
            // 4.x: replace the color of the horizontal divider
            else {
                Drawable drawable = child.getBackground();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && drawable instanceof ColorDrawable) {
                    if (isHoloBlueColor((ColorDrawable) drawable)) child.setBackgroundColor(mMyAppColorId);
                }
            }
        }
    }

    /**
     * Replace dark dialog corners with bright ones (2.x)
     */
    private void uglyHackReplaceDialogCorners(ViewGroup viewGroup) {
        Drawable background = viewGroup.getBackground();
        if (background instanceof NinePatchDrawable) {
            String imageSource = getNinePatchImageSource((NinePatchDrawable) background);
            if (imageSource != null) {
                int alertDialogStyleId = Attributes.getResourceIdStyleAttribute(mContext, mDialogStyleId, android.R.attr.alertDialogStyle);
                if (imageSource.contains("popup_top_dark")) viewGroup.setBackgroundResource(Attributes.getResourceIdStyleAttribute(mContext,
                        alertDialogStyleId, android.R.attr.topDark));
                else if (imageSource.contains("popup_top_bright")) viewGroup.setBackgroundResource(Attributes.getResourceIdStyleAttribute(mContext,
                        alertDialogStyleId, android.R.attr.topBright));
                else if (imageSource.contains("popup_center_dark")) viewGroup.setBackgroundResource(Attributes.getResourceIdStyleAttribute(mContext,
                        alertDialogStyleId, android.R.attr.centerDark));
                else if (imageSource.contains("popup_center_medium")) viewGroup.setBackgroundResource(Attributes.getResourceIdStyleAttribute(mContext,
                        alertDialogStyleId, android.R.attr.centerMedium));
                else if (imageSource.contains("popup_center_bright")) viewGroup.setBackgroundResource(Attributes.getResourceIdStyleAttribute(mContext,
                        alertDialogStyleId, android.R.attr.centerBright));
                else if (imageSource.contains("popup_bottom_dark")) viewGroup.setBackgroundResource(Attributes.getResourceIdStyleAttribute(mContext,
                        alertDialogStyleId, android.R.attr.bottomDark));
                else if (imageSource.contains("popup_bottom_medium")) viewGroup.setBackgroundResource(Attributes.getResourceIdStyleAttribute(mContext,
                        alertDialogStyleId, android.R.attr.bottomMedium));
                else if (imageSource.contains("popup_bottom_bright"))
                    viewGroup.setBackgroundResource(Attributes.getResourceIdStyleAttribute(mContext, alertDialogStyleId, android.R.attr.bottomBright));
            }
        }
    }

    /**
     * @return true if the given nine patch is the divider_strong_holo nine patch.
     */
    private boolean isHoloBlueNinePatch(NinePatchDrawable n) {
        String imageSource = getNinePatchImageSource(n);
        return imageSource != null && (imageSource.contains("divider_strong_holo") || imageSource.contains("divider_horizontal_dark"));
    }

    /**
     * @return the filename for this nine patch.
     */
    private String getNinePatchImageSource(NinePatchDrawable n) {
        // horrible, horrible...
        String imageSource = null;
        lazyInitCrazyReflectionCrap();
        try {
            NinePatch ninePatch = (NinePatch) sNinePatchField.get(n);
            imageSource = (String) sNinePatchSourceField.get(ninePatch);
        } catch (IllegalAccessException e) {
            Log.v(TAG, "Oops: " + e.getMessage(), e);
        }
        return imageSource;
    }

    private boolean isHoloBlueColor(int colorId) {
        lazyInitHoloColors();
        return colorId == sHoloBlueLightColorId || colorId == sHoloBlueDarkColorId;
    }

    /**
     * @return true if the given color is holo blue light or dark
     */
    @TargetApi(14)
    private boolean isHoloBlueColor(ColorDrawable c) {
        return isHoloBlueColor(c.getColor());
    }

    @TargetApi(14)
    private void lazyInitHoloColors() {
        if (sHoloBlueLightColorId == -1) {
            sHoloBlueLightColorId = mContext.getResources().getColor(android.R.color.holo_blue_light);
            sHoloBlueDarkColorId = mContext.getResources().getColor(android.R.color.holo_blue_dark);
        }
    }

    private void lazyInitCrazyReflectionCrap() {
        try {
            if (sNinePatchSourceField == null) {
                sNinePatchField = NinePatchDrawable.class.getDeclaredField("mNinePatch");
                sNinePatchField.setAccessible(true);
                sNinePatchSourceField = NinePatch.class.getDeclaredField("mSrcName");
                sNinePatchSourceField.setAccessible(true);
            }
        } catch (NoSuchFieldException e) {
            Log.v(TAG, "An exception is what we deserve doing code like this: " + e.getMessage(), e);
        }
    }

}
