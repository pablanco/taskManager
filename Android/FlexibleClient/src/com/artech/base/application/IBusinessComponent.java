package com.artech.base.application;

import java.util.List;

import com.artech.base.model.Entity;

public interface IBusinessComponent
{
	void initialize(Entity entity);
	OutputResult load(Entity entity, List<String> key);
	OutputResult save(Entity entity);
	OutputResult delete();
}
