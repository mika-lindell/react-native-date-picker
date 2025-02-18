package com.henninghall.date_picker.ui;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.accessibilityservice.AccessibilityServiceInfo;
import cn.carbswang.android.numberpickerview.library.NumberPickerView;

import com.henninghall.date_picker.DatePickerPackage;
import com.henninghall.date_picker.State;
import com.henninghall.date_picker.Utils;
import com.henninghall.date_picker.wheelFunctions.WheelFunction;
import com.henninghall.date_picker.wheels.Wheel;

import java.util.Locale;
import java.util.List;

public class Accessibility {

    private final static AccessibilityManager systemManager =
            (AccessibilityManager) DatePickerPackage.context
                    .getApplicationContext()
                    .getSystemService(Context.ACCESSIBILITY_SERVICE);

    /**
      When TalkBack is active, user can use one finger to explore the screen
      and set focus to elements. Then user can proceed to use second finger
      to scroll contents of focused element.
      When there's multiple pickers next to each other horizontally,
      it's easy to accidentally scroll more than one picker at a time.
      Following code aims to fix this issue.
    */
    public static boolean shouldAllowScroll(View view){
        // If TalkBack isn't active, always proceed without suppressing touch events
        if (!systemManager.isTouchExplorationEnabled()) {
            return true;
        }
        if (Build.VERSION.SDK_INT >= 21) {
            return view.isAccessibilityFocused();
        }
        return false;
    }

    public static class SetAccessibilityDelegate implements WheelFunction {

        private final Locale locale;

        public SetAccessibilityDelegate(Locale locale) {
            this.locale = locale;
        }

        @Override
        public void apply(Wheel wheel) {
            final View view = wheel.picker.getView();
            view.setAccessibilityDelegate(
                    new View.AccessibilityDelegate(){
                        @Override
                        public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
                            super.onPopulateAccessibilityEvent(host, event);
                            if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                                String resourceKey = view.getTag().toString()+"_description";
                                String localeTag =  Utils.getLocalisedStringFromResources(locale, resourceKey);
                                // Screen reader reads the content description when focused on each picker wheel
                                view.setContentDescription(localeTag);
                            }
                        }
                    }
            );
        }
    }

    private final State state;
    private final Wheels wheels;

    public Accessibility(State state, Wheels wheels){
        this.state = state;
        this.wheels = wheels;
    }

    public void update(Wheel picker){
        String tagName = picker.picker.getView().getTag().toString();
        String selectedDateString = getAccessibleTextForSelectedDate();
        String descriptionPrefix = Utils.getLocalisedStringFromResources(state.getLocale(), "selected_"+tagName+"_description");
        String descriptionPostFix = Utils.getLocalisedStringFromResources(state.getLocale(), "selected_value_description");

        picker.picker.getView().setContentDescription(descriptionPrefix + ", "+ descriptionPostFix + " "+ selectedDateString);
    }

    /**
     * Checks if the accessibility service responsible of spoken feedback is active
     */
    public static boolean isSpokenFeedbackEnabled() {
        return hasAccessibilityFeatureTypeEnabled(AccessibilityServiceInfo.FEEDBACK_SPOKEN);
    }

    /**
     * Get a list of accessibility services currently active
     */
    private static boolean hasAccessibilityFeatureTypeEnabled(int type) {

        List<AccessibilityServiceInfo> enabledServices =
                systemManager.getEnabledAccessibilityServiceList(type);

        return enabledServices != null && enabledServices.size() > 0;
    }

    /**
     * Read a message out loud when spoken feedback is active
     */
    public static void announce(String message) {
        if (systemManager == null || !isSpokenFeedbackEnabled()) {
            return;
          }

          AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT);
          event.getText().add(message);
          systemManager.sendAccessibilityEvent(event);
    }

    /**
     * Get NumberPickerView displayValue from value.
     */
    private static String numberPickerValueToDisplayedValue(NumberPickerView numberPicker, int value) {
        final String[] displayValues = numberPicker.getDisplayedValues();

        final String displayValue = displayValues[value];

        if (displayValue != null) {
            return displayValue;
        }

        return String.valueOf(value);
    }

    /**
     * Read NumberPickerView displayed value.
     * For TalkBack to read dates etc. correctly, make sure they are in localised format.
     */
    public static void announceNumberPickerValue(NumberPickerView numberPicker, int newValue) {
        announce(numberPickerValueToDisplayedValue(numberPicker, newValue));
    }

    private String getAccessibleTextForSelectedDate() {
        String accessibleText;
        switch(state.getMode()) {
            case date:
                accessibleText = wheels.getDateString();
                break;
            case time:
                accessibleText = wheels.getTimeString();
                break;
            default:
                // default is dateTime
                String timePrefix = Utils.getLocalisedStringFromResources(state.getLocale(), "time_tag");
                String hourPrefix = Utils.getLocalisedStringFromResources(state.getLocale(), "hour_tag");
                String minutesPrefix = Utils.getLocalisedStringFromResources(state.getLocale(), "minutes_tag");
                accessibleText = wheels.getAccessibleDateTimeString(timePrefix, hourPrefix, minutesPrefix);
                break;
        }
        return accessibleText;
    }

}
