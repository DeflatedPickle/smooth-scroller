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

package com.weebly.opus1269.smoothscroller.property;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a float value that is common to all editors and
 * is persisted to the GUI
 */
public class Prop {
    public final SmoothScrollerProperties property;
    /**
     * The default value of this property
     */
    public final float def;
    /**
     * The maximum value of this property
     */
    private final float max;
    /**
     * The current value of this property
     */
    private float val;
    private int pos;

    public Prop(
            @NotNull SmoothScrollerProperties property,
            float def,
            float max
    ) {
        this.property = property;
        this.def = def;
        this.max = max;
        this.val = def;
        this.pos = Math.round(100.0F * this.val / this.max);
    }

    public float getVal() {
        return val;
    }

    public void setVal(float val) {
        this.val = val;
        this.pos = Math.round(100.0F * this.val / this.max);
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
        this.val = this.max * this.pos / 100.0F;
    }
}
