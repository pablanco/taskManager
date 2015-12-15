package com.artech.controls;

public class GxTouchEvents
{
	public static final String TAP = "Tap";
	public static final String LONG_TAP = "LongTap";
	public static final String DOUBLE_TAP = "DoubleTap";
	public static final String SWIPE = "Swipe";
	public static final String SWIPE_LEFT = "SwipeLeft";
	public static final String SWIPE_RIGHT = "SwipeRight";
	public static final String SWIPE_UP = "SwipeUp";
	public static final String SWIPE_DOWN = "SwipeDown";

	public static final String START_DRAG = "Drag";
	public static final String DROP = "Drop"; // This is not strictly a "touch" event.

	public static final String[] ALL_EVENTS = { TAP, LONG_TAP, DOUBLE_TAP, SWIPE, SWIPE_LEFT, SWIPE_RIGHT, SWIPE_UP, SWIPE_DOWN, START_DRAG };
	public static final String[] TAP_EVENTS = { TAP, LONG_TAP, DOUBLE_TAP };
}
