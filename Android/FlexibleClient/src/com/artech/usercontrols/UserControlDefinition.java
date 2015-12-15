package com.artech.usercontrols;

public class UserControlDefinition {
	public UserControlDefinition(String name, String cls) {
		Name = name;
		ClassName = cls;
	}

	public UserControlDefinition() {
	}

	public String Name;
	public String ClassName;
	public boolean IsScrollable;
}
