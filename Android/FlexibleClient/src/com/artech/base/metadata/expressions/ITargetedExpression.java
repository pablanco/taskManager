package com.artech.base.metadata.expressions;

/**
 * Interface for expression classes that have a target.something structure (such as methods or properties).
 * Created by matiash on 01/06/2015.
 */
public interface ITargetedExpression
{
	Expression getTarget();
}
