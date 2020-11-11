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

import com.intellij.openapi.editor.ScrollingModel;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;

class SmoothScrollerMouseWheelListener implements MouseWheelListener, ActionListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmoothScrollerMouseWheelListener.class);

    // The frame rate of the animation
    // TODO: Investigate if we can get an AnimationFrame
    private static final int FRAMES_PER_SECOND;

    static {
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screenDevices = graphicsEnvironment.getScreenDevices();

        int highest = 0;
        for (GraphicsDevice graphicsDevice : screenDevices) {
            DisplayMode displayMode = graphicsDevice.getDisplayMode();
            int refreshRate = displayMode.getRefreshRate();

            if (refreshRate > highest) {
                highest = refreshRate;
            }
        }

        FRAMES_PER_SECOND = highest;
        LOGGER.debug(String.format("Set scrolling to %d FPS", FRAMES_PER_SECOND));
    }

    private static final int MILLIS_PER_FRAME = 1000 / FRAMES_PER_SECOND;

    // Scrolling model of the window
    private final ScrollingModel mScrollingModel;

    // Timer to handle the animation
    private final Timer mTimer = new Timer(MILLIS_PER_FRAME, this);

    // The last input from the mouse wheel event
    private double mLastWheelDelta = 0.0D;

    // 'true' when mouse wheel events are being processed
    private boolean mScrolling = false;

    // 'true' when mouse wheel events are horizontal
    private boolean mScrollingHorizontal = false;

    // The current velocity of the window, usually in rows / mSec
    private double mVelocity = 0.0D;

    // A history of the last several scroll velocities
    private final ArrayList<Double> mVelocities = new ArrayList<>();
    private static final int MAX_VELOCITIES = 10;

    /**
     * Constructor for our MouseWheelListener.
     *
     * @param editor The file editor to which smooth scrolling is to be added
     */
    public SmoothScrollerMouseWheelListener(FileEditor editor) {
        this.mScrollingModel = ((TextEditor) editor).getEditor().getScrollingModel();
        // We will do the animation
        this.mScrollingModel.disableAnimation();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        final double speedThreshold = Props.get(SmoothScrollerProperties.THRESHOLD).getVal();
        final double speedLimit = Props.get(SmoothScrollerProperties.SPEED_LIMIT).getVal();
        final double accelerationLimit = Props.get(SmoothScrollerProperties.ACCELERATION_LIMIT).getVal();
        final double scrollMultiplier = Props.get(SmoothScrollerProperties.MULTIPLIER).getVal();

        // don't want to apply any easing to velocity while scrolling
        this.mScrolling = true;
        this.mScrollingHorizontal = e.getModifiersEx() != 0;

        this.mScrollingModel.runActionOnScrollingFinished(() -> {
            mScrolling = false;
            mVelocities.clear();
        });

        // Track wheel motion delta
        // TODO: Could use a debounce probably
        final double wheelDelta = e.getPreciseWheelRotation();
        final boolean sameDirection = this.mLastWheelDelta * wheelDelta > 0.0D;
        this.mLastWheelDelta = wheelDelta;

        if (!sameDirection) {
            // changed direction
            zeroVelocity();
            return;
        }

        // calculate new velocity increment
        final double scrollDelta = e.getScrollAmount() * wheelDelta * scrollMultiplier;
        final double deltaV = scrollDelta / MILLIS_PER_FRAME;

        if (Math.abs(deltaV) < speedThreshold) {
            // skip small movements
            return;
        }

        final double oldVelocity = this.mVelocity;
        final double newVelocity = this.mVelocity + deltaV;

        // calculate average velocity over last several mouse wheel events
        if (this.mVelocities.size() == MAX_VELOCITIES) {
            this.mVelocities.remove(0);
        }
        this.mVelocities.add(newVelocity);

        this.mVelocity = getAverage(this.mVelocities);

        // limit acceleration
        final double acc = (this.mVelocity - oldVelocity) / MILLIS_PER_FRAME;
        if (Math.abs(acc) > accelerationLimit) {
            this.mVelocity = oldVelocity + accelerationLimit * MILLIS_PER_FRAME * Math.signum(acc);
        }

        // limit speed
        if (Math.abs(mVelocity) > speedLimit) {
            this.mVelocity = speedLimit * Math.signum(mVelocity);
        }
        if (Math.abs(mVelocity) < speedThreshold) {
            this.zeroVelocity();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.update();
    }

    /**
     * Starts animating the scroll offset.
     */
    public void startAnimating() {
        this.mTimer.start();
    }

    /**
     * Stops animating the scroll offset.
     */
    public void stopAnimating() {
        this.mTimer.stop();
    }

    /**
     * Updates the velocity acting on the scroll offset and then updates
     * the scroll offset.
     */
    private void update() {
        final double speedThreshold = Props.get(SmoothScrollerProperties.THRESHOLD).getVal();
        final double friction = Props.get(SmoothScrollerProperties.FRICTION).getVal();

        if (!mScrolling) {
            // Basic kinetic scrolling, exponential decay vel_new = vel * e^-friction*deltaT
            this.mVelocity = mVelocity * Math.exp(-friction * MILLIS_PER_FRAME);
        }

        if (Math.abs(mVelocity) >= speedThreshold) {
            // Reposition cursor offset based on current velocity
            if (this.mScrollingHorizontal) {
                final int currentOffset = mScrollingModel.getHorizontalScrollOffset();
                final long offset = Math.round(
                        (currentOffset + this.mVelocity * MILLIS_PER_FRAME)
                );
                this.mScrollingModel.scrollHorizontally(Math.max(0, (int) offset));
            } else {
                final int currentOffset = mScrollingModel.getVerticalScrollOffset();
                final long offset = Math.round(
                        (currentOffset + this.mVelocity * MILLIS_PER_FRAME)
                );
                this.mScrollingModel.scrollVertically(Math.max(0, (int) offset));
            }
        } else {
            // Bring to stop below threshold
            this.zeroVelocity();
        }
    }

    /**
     * Sets the velocity to 0 and clears the list of previous velocities
     */
    private void zeroVelocity() {
        this.mVelocity = 0.0D;
        this.mVelocities.clear();
    }

    /**
     * Finds the average of all values in a list
     *
     * @param array An array of doubles
     * @return The average of the given array
     */
    private double getAverage(@NotNull ArrayList<Double> array) {
        double sum = 0.0D;
        if (!array.isEmpty()) {
            for (Double item : array) {
                sum = sum + item;
            }
            return sum / array.size();
        }
        return sum;
    }
}
