/*
 * Copyright (C) 2021 The Android Open Source Project
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

package io.github.dot166.focuslock.ui;

import android.Manifest;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import io.github.dot166.focuslock.R;
import kotlin.Pair;

/**
 * A Preference for the permission usage graphic.
 */
public class PermissionUsageGraphicPreference extends Preference {

    /** Permission group to count mapping. */
    private @NonNull Map<String, Pair<Integer, String>> mUsages = new HashMap<>();
    private boolean mIsNightMode;

    public PermissionUsageGraphicPreference(@NonNull Context context, @Nullable AttributeSet attrs,
                                            @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public PermissionUsageGraphicPreference(@NonNull Context context, @Nullable AttributeSet attrs,
                                            @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public PermissionUsageGraphicPreference(@NonNull Context context,
                                            @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PermissionUsageGraphicPreference(@NonNull Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        mIsNightMode = (configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        setLayoutResource(R.layout.permission_usage_graphic);
        setSelectable(false);
    }

    /** Sets permission group usages: map of group name to usage count. */
    public void setUsages(Map<String, Pair<Integer, String>> usages) {
        if (!Objects.equals(mUsages, usages)) {
            mUsages = usages;
            notifyChanged();
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        boolean isUsagesEmpty = isUsagesEmpty();

        CompositeCircleView ccv =
                (CompositeCircleView) holder.findViewById(R.id.composite_circle_view);
        CompositeCircleViewLabeler ccvl = (CompositeCircleViewLabeler) holder.findViewById(
                R.id.composite_circle_view_labeler);

        // Set center text.
        TextView centerLabel = new TextView(getContext());
        centerLabel.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);

        centerLabel.setText(getContext().getString(R.string.privdash_label_24h));
        centerLabel.setTextAppearance(R.style.PrivacyDashboardGraphicLabel);

        final int colorCamera = getContext().obtainStyledAttributes(new int[]{androidx.appcompat.R.attr.colorPrimary})
                .getColor(0, 0xFFFFC0CB);
        final int colorMicrophone = getContext().obtainStyledAttributes(new int[]{com.google.android.material.R.attr.colorOnPrimary})
                .getColor(0, 0xFF680000);
        final int colorLocation = getContext().obtainStyledAttributes(new int[]{com.google.android.material.R.attr.colorSecondary})
                .getColor(0, 0xFF006800);
        final int colorOther = getContext().obtainStyledAttributes(new int[]{com.google.android.material.R.attr.colorOnSecondary})
                .getColor(0, 0xFF000068);

        // Create labels, counts, and colors.
        TextView[] labels;
        Pair<Integer, String>[] counts;
        int[] colors;
        if (isUsagesEmpty) {
            // Special case if usages are empty.
            labels = new TextView[] { new TextView(getContext()) };
            labels[0] = null;
            counts = new Pair[] {new Pair(0, "")};
            colors = new int[] { 0 };
        } else {
            labels = new TextView[]{
                    new TextView(getContext()),
                    new TextView(getContext()),
                    new TextView(getContext()),
                    new TextView(getContext())
            };
            labels[0].setText(getContext().getString(R.string.privdash_label_camera));
            labels[1].setText(getContext().getString(R.string.privdash_label_microphone));
            labels[2].setText(getContext().getString(R.string.privdash_label_location));
            labels[3].setText(getContext().getString(R.string.privdash_label_other));
            counts = new Pair[]{
                    getUsageCount("App 1"),
                    getUsageCount("App 2"),
                    getUsageCount("App 3"),
                    getUsageCount("Others")
            };
            int total = 0;
            for (int i = 0; i < counts.length; i++) {
                total = total + counts[i].getFirst();
                labels[i].setText(counts[i].getSecond());
            }
            int hours = (int) (total / (1000 * 60 * 60));
            int minutes = (int) ((total % (1000 * 60 * 60)) / (1000 * 60));
            int seconds = (int) ((total % (1000 * 60)) / 1000);
            String timeString = String.format("Today\n%02dh%02dm%02ds", hours, minutes, seconds);
            centerLabel.setText(timeString);
            colors = new int[]{
                    colorCamera,
                    colorMicrophone,
                    colorLocation,
                    colorOther
            };

            // Set label styles.
            for (int i = 0; i < labels.length; i++) {
                if (labels[i] != null) {
                    labels[i].setTextAppearance(R.style.PrivacyDashboardGraphicLabel);
                }
            }
        }

        // Get circle-related dimensions.
        TypedValue outValue = new TypedValue();
        getContext().getResources().getValue(R.dimen.privhub_label_radius_scalar,
                outValue, true);
        float labelRadiusScalar = outValue.getFloat();
        int circleStrokeWidth = (int) getContext().getResources().getDimension(
                R.dimen.privhub_circle_stroke_width);

        // Configure circle and labeler.
        ccvl.configure(R.id.composite_circle_view, centerLabel, labels, labelRadiusScalar);
        // Start at angle 300 (top right) to allow for small segments.
        ccv.configure(300, counts, colors, circleStrokeWidth, labels);
    }

    private Pair<Integer, String> getUsageCount(String group) {
        Pair<Integer, String> count = mUsages.get(group);
        if (count == null) {
            return new Pair<>(0, "");
        }
        return count;
    }

    private int getUsageCountExcluding(String... excludeGroups) {
        int count = 0;
        List<String> exclude = Arrays.asList(excludeGroups);
        for (Map.Entry<String, Pair<Integer, String>> entry : mUsages.entrySet()) {
            if (exclude.indexOf(entry.getKey()) >= 0) {
                continue;
            }
            count += entry.getValue().getFirst();
        }
        return count;
    }

    private boolean isUsagesEmpty() {
        return getUsageCountExcluding() == 0;
    }
}
