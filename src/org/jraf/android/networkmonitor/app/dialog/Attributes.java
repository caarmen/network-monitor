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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;

/**
 * Some helper methods to get style attributes
 */
public class Attributes {


    static int getDimensionStyleAttribute(Context context, int styleId, int attrId) {
        TypedArray values = context.getTheme().obtainStyledAttributes(styleId, new int[] { attrId });
        int value = values.getLayoutDimension(0, 0);
        values.recycle();
        return value;
    }

    static int getColorStyleAttribute(Context context, int styleId, int attrId) {
        TypedArray values = context.getTheme().obtainStyledAttributes(styleId, new int[] { attrId });
        int value = values.getColor(0, android.R.color.primary_text_dark);
        values.recycle();
        return value;
    }

    static int getResourceIdStyleAttribute(Context context, int styleId, int attrId) {
        TypedArray values = context.getTheme().obtainStyledAttributes(styleId, new int[] { attrId });
        int value = values.getResourceId(0, 0);
        values.recycle();
        return value;
    }

    static Drawable getDrawableStyleAttribute(Context context, int styleId, int attrId) {
        TypedArray values = context.getTheme().obtainStyledAttributes(styleId, new int[] { attrId });
        Drawable value = values.getDrawable(0);
        values.recycle();
        return value;
    }
}
