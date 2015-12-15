package com.artech.extendedcontrols.matrixgrid;

import android.view.View;

interface ScrollViewListener
{
    void onScrollChanged(View scrollView, int x, int y, int oldX, int oldY);
}