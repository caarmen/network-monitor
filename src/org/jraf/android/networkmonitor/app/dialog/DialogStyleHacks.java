/**
 * Copyright 2013 Carmen Alvarez
 *
 * This file is part of Scrum Chatter.
 *
 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Scrum Chatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Scrum Chatter. If not, see <http://www.gnu.org/licenses/>.
 */
package org.jraf.android.networkmonitor.app.dialog;

import java.lang.reflect.Field;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.res.ColorStateList;
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
import android.widget.TextView;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.R;

/**
 * Boy is customizing alert dialogs a pain in the booty. Tried the android-styled-dialogs library but it didn't fit the needs of this app: no support for alert
 * dialogs with EditTexts, and not a clean way to manage clicks on the dialog buttons. Started out trying to copy the resources used for dialogs, one-by-one,
 * from the core android framework, but that was more pain than the approach I decided to take in this class.
 */
public class DialogStyleHacks {

    private static final String TAG = Constants.TAG + "/" + DialogStyleHacks.class.getSimpleName();
    private static final int DIALOG_STYLE = R.style.dialogStyle;
    private static int sHoloBlueLightColorId = -1;
    private static int sHoloBlueDarkColorId = -1;
    private static int sHoloPurpleColorId = -1;
    private static Field sNinePatchSourceField = null;
    private static Field sNinePatchField = null;

    /**
     * @param dialog apply our custom theme to the given dialog, once the dialog is visible.
     */
    public static void styleDialog(final Context context, final AlertDialog dialog) {
        dialog.getContext().setTheme(DIALOG_STYLE);
        if (dialog.isShowing()) styleDialogElements(context, dialog);
        else
            dialog.setOnShowListener(new OnShowListener() {

                @Override
                public void onShow(DialogInterface dialogInterface) {
                    styleDialogElements(context, dialog);
                }
            });
    }

    public static void styleDialogElements(Context context, AlertDialog dialog) {
        // Update the dialog elements which couldn't be updated cleanly with the theme:
        ListView listView = dialog.getListView();
        int listViewStyle = Attributes.getResourceIdStyleAttribute(context, DIALOG_STYLE, android.R.attr.listViewStyle);
        Drawable listSelector = Attributes.getDrawableStyleAttribute(context, listViewStyle, android.R.attr.listSelector);
        if (listView != null) listView.setSelector(listSelector);
        Button button = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        if (button != null) styleButton(context, button);//button.setBackgroundResource(R.drawable.netmon_btn_default_holo_light);
        button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) styleButton(context, button);//button.setBackgroundResource(R.drawable.netmon_btn_default_holo_light);
        button = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        if (button != null) styleButton(context, button);//button.setBackgroundResource(R.drawable.netmon_btn_default_holo_light);
        DialogStyleHacks.uglyHackReplaceBlueHoloBackground(context, (ViewGroup) dialog.getWindow().getDecorView(), dialog);
    }

    /**
     * Set the min height, min width, text color, and background drawable of this button based on our ButtonNetMon style.
     */
    private static void styleButton(Context context, Button button) {
        int buttonStyleId = Attributes.getResourceIdStyleAttribute(context, DIALOG_STYLE, android.R.attr.buttonStyle);
        int minWidth = Attributes.getDimensionStyleAttribute(context, buttonStyleId, android.R.attr.minWidth);
        int minHeight = Attributes.getDimensionStyleAttribute(context, buttonStyleId, android.R.attr.minHeight);
        int textColor = Attributes.getColorStyleAttribute(context, buttonStyleId, android.R.attr.textColor);
        int backgroundId = Attributes.getResourceIdStyleAttribute(context, buttonStyleId, android.R.attr.background);
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
    public static void uglyHackReplaceBlueHoloBackground(Context context, ViewGroup viewGroup, AlertDialog dialog) {
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof ViewGroup) {
                uglyHackReplaceDialogCorners(context, (ViewGroup) child);
                uglyHackReplaceBlueHoloBackground(context, (ViewGroup) child, dialog);
            }
            // 2.x and 3.x: replace the nine patch
            else if (child instanceof ImageView) {
                ImageView imageView = (ImageView) child;
                Drawable drawable = imageView.getDrawable();
                if (drawable instanceof NinePatchDrawable) {
                    if (isHoloBlueNinePatch((NinePatchDrawable) drawable)) {
                        imageView.setImageResource(R.drawable.divider_strong_netmon);
                        // On 2.x, in a dialog with a list, the divider is hidden.  Let's show it.
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB && !(dialog instanceof ProgressDialog))
                            imageView.setVisibility(View.VISIBLE);
                    }
                }
            }
            // 2.x: replace the radio button
            else if (child instanceof CheckedTextView) {
                Drawable radioButtonDrawable = Attributes.getDrawableStyleAttribute(context, DIALOG_STYLE, android.R.attr.listChoiceIndicatorSingle);
                ((CheckedTextView) child).setCheckMarkDrawable(radioButtonDrawable);
            } else if (child instanceof TextView) {
                TextView textView = (TextView) child;
                ColorStateList textColors = textView.getTextColors();
                int defaultColor = textColors.getDefaultColor();
                if (isHoloBlueColor(context, defaultColor)) textView.setTextColor(sHoloPurpleColorId);
            }
            // 4.x: replace the color
            else {
                Drawable drawable = child.getBackground();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && drawable instanceof ColorDrawable) {
                    if (isHoloBlueColor(context, (ColorDrawable) drawable)) child.setBackgroundColor(sHoloPurpleColorId);
                }
            }
        }
    }

    /**
     * Replace dark dialog corners with bright ones (2.x)
     */
    private static void uglyHackReplaceDialogCorners(Context context, ViewGroup viewGroup) {
        Drawable background = viewGroup.getBackground();
        if (background instanceof NinePatchDrawable) {
            String imageSource = getNinePatchImageSource((NinePatchDrawable) background);
            if (imageSource != null) {
                if (imageSource.contains("popup_top_dark") || imageSource.contains("popup_top_medium")) viewGroup
                        .setBackgroundResource(R.drawable.popup_top_bright);
                else if (imageSource.contains("popup_center_dark") || imageSource.contains("popup_center_medium")
                        || imageSource.contains("popup_center_bright")) viewGroup.setBackgroundResource(R.drawable.popup_center_bright);
                else if (imageSource.contains("popup_bottom_dark") || imageSource.contains("popup_bottom_medium"))
                    viewGroup.setBackgroundResource(R.drawable.popup_bottom_bright);
            }
        }
    }

    /**
     * @return true if the given nine patch is the divider_strong_holo nine patch.
     */
    private static boolean isHoloBlueNinePatch(NinePatchDrawable n) {
        String imageSource = getNinePatchImageSource(n);
        return imageSource != null && (imageSource.contains("divider_strong_holo") || imageSource.contains("divider_horizontal_dark"));
    }

    private static String getNinePatchImageSource(NinePatchDrawable n) {
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

    private static boolean isHoloBlueColor(Context context, int colorId) {
        lazyInitHoloColors(context);
        return colorId == sHoloBlueLightColorId || colorId == sHoloBlueDarkColorId;
    }

    /**
     * @return true if the given color is holo blue light or dark
     */
    @TargetApi(14)
    private static boolean isHoloBlueColor(Context context, ColorDrawable c) {
        return isHoloBlueColor(context, c.getColor());
    }

    @TargetApi(14)
    private static void lazyInitHoloColors(Context context) {
        if (sHoloBlueLightColorId == -1) {
            sHoloBlueLightColorId = context.getResources().getColor(android.R.color.holo_blue_light);
            sHoloBlueDarkColorId = context.getResources().getColor(android.R.color.holo_blue_dark);
            sHoloPurpleColorId = context.getResources().getColor(R.color.netmon_color);
        }
    }

    private static void lazyInitCrazyReflectionCrap() {
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
