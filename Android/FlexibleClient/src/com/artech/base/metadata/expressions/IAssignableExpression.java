package com.artech.base.metadata.expressions;

/**
 * Interface for expression classes that can also be l-values in an assignment.
 * Assignable: variables, sdt fields, read-write properties (such as &SDTCollection.CurrentItem).
 * Non-assignable: constants, arithmetic/boolean expressions, methods, read-only properties (such as &SDTCollection.Count)
 * @author matiash
 *
 */
public interface IAssignableExpression
{
	boolean setValue(IExpressionContext context, Object value);
	String getRootName();
}
