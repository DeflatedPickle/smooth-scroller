/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 DeflatedPickle
 * Copyright (c) 2016 Michael A Updike
 * Copyright (c) 2013 Hugo Campos
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.weebly.opus1269.smoothscroller;

import com.intellij.ide.util.PropertiesComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Represents all the properties that are common to all editors
 */
public class Props {
    private static final ArrayList<Prop> sProps = new ArrayList<>();

    private Props() {
    }

    /**
     * Create the properties and load their current values from the IDE
     */
    public static void initialize() {
        // Create the Prop objects
        sProps.add(new Prop(SmoothScrollerProperties.THRESHOLD, 0.0005F, 0.001F));
        sProps.add(new Prop(SmoothScrollerProperties.SPEED_LIMIT, 25.0F, 100.0F));
        sProps.add(new Prop(SmoothScrollerProperties.ACCELERATION_LIMIT, 5.0F, 10.0F));
        sProps.add(new Prop(SmoothScrollerProperties.FRICTION, 0.005F, .015F));
        sProps.add(new Prop(SmoothScrollerProperties.MULTIPLIER, 1.0F, 100.0F));

        // Load the current values from the IDE
        PropertiesComponent propsComp = PropertiesComponent.getInstance();
        for (Prop prop : sProps) {
            prop.setVal(propsComp.getFloat(prop.property.toString(), prop.def));
        }
    }

    /**
     * Reset all values to their defaults
     */
    public static void resetDefaults() {
        for (Prop prop : sProps) {
            prop.setVal(prop.def);
        }

        storeProperties();
    }

    /**
     * Save properties to the IDE
     */
    public static void storeProperties() {
        PropertiesComponent propsComp = PropertiesComponent.getInstance();

        for (Prop prop : sProps) {
            propsComp.setValue(prop.property.toString(), String.valueOf(prop.getVal()));
        }
    }

    @NotNull
    public static Prop get(int id) {
        return Props.get(SmoothScrollerProperties.values()[id]);
    }

    @NotNull
    public static Prop get(SmoothScrollerProperties property) {
        return sProps.get(property.ordinal());
    }
}
