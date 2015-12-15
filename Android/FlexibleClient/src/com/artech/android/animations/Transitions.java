package com.artech.android.animations;

import com.artech.base.services.Services;
import com.artech.base.utils.NameMap;

import static com.artech.R.anim.gx_fade_in;
import static com.artech.R.anim.gx_fade_out;
import static com.artech.R.anim.gx_none;
import static com.artech.R.anim.gx_push_down_in;
import static com.artech.R.anim.gx_push_down_out;
import static com.artech.R.anim.gx_push_left_in;
import static com.artech.R.anim.gx_push_left_out;
import static com.artech.R.anim.gx_push_right_in;
import static com.artech.R.anim.gx_push_right_out;
import static com.artech.R.anim.gx_push_up_in;
import static com.artech.R.anim.gx_push_up_out;
import static com.artech.R.anim.gx_slide_caller_activity_in;
import static com.artech.R.anim.gx_slide_caller_activity_out;
import static com.artech.R.anim.gx_slide_caller_fragment_out;
import static com.artech.R.anim.gx_slide_down_in;
import static com.artech.R.anim.gx_slide_down_out;
import static com.artech.R.anim.gx_slide_left_in;
import static com.artech.R.anim.gx_slide_left_out;
import static com.artech.R.anim.gx_slide_right_in;
import static com.artech.R.anim.gx_slide_right_out;
import static com.artech.R.anim.gx_slide_up_in;
import static com.artech.R.anim.gx_slide_up_out;

public class Transitions
{
	private static NameMap<Transition> sTransitions;

	static
	{
		sTransitions = new NameMap<Transition>();

		// These transitions are symmetrical: the "enter" and "close" parts are the same.
		add(new Transition("gx_none", gx_none, gx_none)); //$NON-NLS-1$
		add(new Transition("gx_push_up", gx_push_up_out, gx_push_up_in)); //$NON-NLS-1$
		add(new Transition("gx_push_down", gx_push_down_out, gx_push_down_in)); //$NON-NLS-1$
		add(new Transition("gx_push_left", gx_push_left_out, gx_push_left_in)); //$NON-NLS-1$
		add(new Transition("gx_push_right", gx_push_right_out, gx_push_right_in)); //$NON-NLS-1$
		add(new Transition("gx_fade", gx_fade_out, gx_fade_in)); //$NON-NLS-1$

		// These transitions are asymmetrical. The animation used when restoring the previous activity
		// is not exactly the same as the animation used to call up another activity.
		// Also, they are tweaked for fragments, because fragment animations are not respecting z-order.
		add(new Transition("gx_slide_up", gx_slide_caller_activity_out, gx_slide_caller_fragment_out, gx_slide_up_in, gx_slide_up_out, gx_slide_caller_activity_in)); //$NON-NLS-1$
		add(new Transition("gx_slide_down", gx_slide_caller_activity_out, gx_slide_caller_fragment_out, gx_slide_down_in, gx_slide_down_out, gx_slide_caller_activity_in)); //$NON-NLS-1$
		add(new Transition("gx_slide_left", gx_slide_caller_activity_out, gx_slide_caller_fragment_out, gx_slide_left_in, gx_slide_left_out, gx_slide_caller_activity_in)); //$NON-NLS-1$
		add(new Transition("gx_slide_right", gx_slide_caller_activity_out, gx_slide_caller_fragment_out, gx_slide_right_in, gx_slide_right_out, gx_slide_caller_activity_in)); //$NON-NLS-1$
	}

	public static Transition get(String name)
	{
		if (!Services.Strings.hasValue(name))
			return null;

		return sTransitions.get(name);
	}

	@SuppressWarnings("WeakerAccess")
	public static void add(Transition transition)
	{
		sTransitions.put(transition.getName(), transition);
	}
}
