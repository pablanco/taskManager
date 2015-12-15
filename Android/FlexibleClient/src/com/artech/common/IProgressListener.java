package com.artech.common;

public interface IProgressListener {

	void setCount(long length) ;
	
	void step() ;

}
