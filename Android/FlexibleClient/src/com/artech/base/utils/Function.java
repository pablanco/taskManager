package com.artech.base.utils;

/**
 * Determines an output value based on an input value (acts as a Parameterized Runnable).
 * Similar to http://code.google.com/p/guava-libraries/wiki/FunctionalExplained
 *
 * @param <Input> Type of the function input.
 * @param <Output> Type of the function output.
 */
public interface Function<Input, Output>
{
	Output run(Input input);
}
