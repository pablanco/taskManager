package com.artech.ui.navigation;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Possible Call Target for a CallOptions object.
 * @author matiash
 *
 */
public class CallTarget
{
	// Some predefined targets. Well, one. For now.
	public static final CallTarget BLANK = new CallTarget("Blank");

	private final String mName;
	private final Set<String> mAliases;

	public CallTarget(String... names)
	{
		mName = names[0];

		Set<String> aliases = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		Collections.addAll(aliases, names);

		mAliases = Collections.unmodifiableSet(aliases);
	}

	@Override
	public String toString()
	{
		return String.format("%s (%s)", mName, mAliases);
	}

	public String getName()
	{
		return mName;
	}

	public boolean isTarget(CallOptions options)
	{
		if (options != null)
		{
			String targetName = options.getTargetName();
			return (targetName != null && mAliases.contains(targetName));
		}
		else
			return false;
	}
}
