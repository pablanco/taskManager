package com.artech.base.controls;

public interface IGxControlNotifyEvents
{
	enum EventType { ACTION_CALLED, REFRESH, ACTIVITY_PAUSED, ACTIVITY_RESUMED, ACTIVITY_DESTROYED }

	void notifyEvent(EventType type);
}
