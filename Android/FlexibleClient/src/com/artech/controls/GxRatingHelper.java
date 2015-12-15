package com.artech.controls;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

public class GxRatingHelper {

	private static Paint paintGreen = new Paint();
	private static Paint paintDarkGreen = new Paint();
	private static Paint paintDarkGrey = new Paint();
    private static Paint paintBlack = new Paint();
    private static Paint paintGrey = new Paint();

    /**
     * Coordinates of the Star

              A
             / \
            /   \
     C-----B     J-----I
       \             /
         D         H
        /     F     \
       /   /     \   \
      E               G

     * */
    //coord_POINT [coordOut_X, coordOut_Y, coordIn_X, coordIn_Y]
    private static float[] coord_A = new float[4];
    private static float[] coord_B = new float[4];
    private static float[] coord_C = new float[4];
    private static float[] coord_D = new float[4];
    private static float[] coord_E = new float[4];
    private static float[] coord_F = new float[4];
    private static float[] coord_G = new float[4];
    private static float[] coord_H = new float[4];
    private static float[] coord_I = new float[4];
    private static float[] coord_J = new float[4];

    private boolean useSmallStar = true;
    private boolean useSelectedStar = true;

	GxRatingHelper(float size, boolean selected, boolean enabled, boolean smallStar)
	{
        setEnabledStar(enabled);
        generateCoord(size);
        setSelectedStar(selected);
        setSmallStar(smallStar);
	}

	public void generateCoord(float size) {
		//generatePaint();
		coord_A[0] = coord_A[2] = coord_F[0] = coord_F[2] = size / 2;
		coord_A[1] = 0;
		coord_A[3] = (float)(size / 5.54);
		coord_B[0] = coord_D[2] = (float)(size / 2.77);
		coord_B[1] = coord_J[1] = coord_E[2] = (float)(size / 3.21);
		coord_B[2] = coord_C[3] = coord_I[3] = (float)(size / 2.44);
		coord_B[3] = coord_J[3] = (float)(size / 2.60);
		coord_C[0] = (float)(size / 40.67);
		coord_C[1] = coord_I[1] = (float)(size / 2.84);
		coord_C[2] = coord_E[0] = (float)(size / 5.08);
		coord_D[0] = (float)(size / 3.70);
		coord_D[1] = coord_H[1] = (float)(size / 1.74);
		coord_D[3] = coord_H[3] = (float)(size / 1.79);
		coord_E[1] = coord_G[1] = (float)(size / 1.11);
		coord_E[3] = coord_G[3] = (float)(size / 1.33);
		coord_F[1] = (float)(size / 1.36);
		coord_F[3] = (float)(size / 1.56);
		coord_G[0] = coord_I[2] = (float)(size / 1.24);
		coord_G[2] = (float)(size / 1.47);
		coord_H[0] = (float)(size / 1.39);
		coord_H[2] = coord_J[0] = (float)(size / 1.58);
		coord_I[0] = (float)(size / 1.03);
		coord_J[2] = (float)(size / 1.72);
	}

	public void setSmallStar(boolean smallStar) {
		useSmallStar = smallStar;
	}

	public void setSelectedStar(boolean selected) {
		useSelectedStar = selected;
	}

	public void setEnabledStar(boolean enabled) {
		if (enabled)
		{
			paintGreen.setColor(Color.argb( 200, 0, 255, 0));
	        paintGreen.setStrokeWidth(1);

	        paintDarkGreen.setColor(Color.argb( 230, 0, 255, 0));
	        paintDarkGreen.setStrokeWidth(1);

	        paintGrey.setColor(Color.argb( 210, 208, 208, 208));
	        paintGrey.setStrokeWidth(1);

	        paintDarkGrey.setColor(Color.argb( 240, 208, 208, 208));
	        paintDarkGrey.setStrokeWidth(1);
		} else {
			paintGreen.setColor(Color.argb( 150, 0, 255, 0));
	        paintGreen.setStrokeWidth(1);

	        paintDarkGreen.setColor(Color.argb( 180, 0, 255, 0));
	        paintDarkGreen.setStrokeWidth(1);

	        paintGrey.setColor(Color.argb( 80, 208, 208, 208));
	        paintGrey.setStrokeWidth(1);

	        paintDarkGrey.setColor(Color.argb( 80, 208, 208, 208));
	        paintDarkGrey.setStrokeWidth(1);
		}
        paintBlack.setColor(Color.argb( 80, 0, 0, 0));
        paintBlack.setStrokeWidth(2);
	}

	public void onDraw(Canvas canvas) {

		Path outline = drawOutLine();
		Path padding = drawPadding();
		Path paddingLeft = drawPaddingLeft();
		Path paddingRight = drawPaddingRight();

        if (useSmallStar) {
        	if (useSelectedStar) {
	        	canvas.drawPath(paddingLeft, paintDarkGreen);
	        	canvas.drawPath(paddingRight, paintGreen);
        	} else {
        		canvas.drawPath(paddingLeft, paintDarkGrey);
	        	canvas.drawPath(paddingRight, paintGrey);
        	}
        	drawLine(canvas, 0, 1);
        } else {
        	canvas.drawPath(outline, paintDarkGrey);
        	if (useSelectedStar)
        		canvas.drawPath(padding, paintGreen);
        	else
        		canvas.drawPath(padding, paintGrey);
        	drawLine(canvas, 2, 3);
        }
    }

	private static Path drawOutLine() {
		//Draw the outline
        Path outline = new Path();
        outline.moveTo(coord_A[0], coord_A[1]); /*A*/
        outline.lineTo(coord_B[0], coord_B[1]); /*B*/
        outline.lineTo(coord_C[0], coord_C[1]); /*C*/
        outline.lineTo(coord_D[0], coord_D[1]); /*D*/
        outline.lineTo(coord_E[0], coord_E[1]); /*E*/
        outline.lineTo(coord_F[0], coord_F[1]); /*F*/
        outline.lineTo(coord_G[0], coord_G[1]); /*G*/
        outline.lineTo(coord_H[0], coord_H[1]); /*H*/
        outline.lineTo(coord_I[0], coord_I[1]); /*I*/
        outline.lineTo(coord_J[0], coord_J[1]); /*J*/
        outline.moveTo(coord_A[2], coord_A[3]); /*A*/
        outline.lineTo(coord_B[2], coord_B[3]); /*J*/
        outline.lineTo(coord_C[2], coord_C[3]); /*I*/
        outline.lineTo(coord_D[2], coord_D[3]); /*H*/
        outline.lineTo(coord_E[2], coord_E[3]); /*G*/
        outline.lineTo(coord_F[2], coord_F[3]); /*F*/
        outline.lineTo(coord_G[2], coord_G[3]); /*E*/
        outline.lineTo(coord_H[2], coord_H[3]); /*D*/
        outline.lineTo(coord_I[2], coord_I[3]); /*C*/
        outline.lineTo(coord_J[2], coord_J[3]); /*B*/
        outline.close();

        return outline;
	}

	private static Path drawPadding() {
		//Draw the padding
    	Path padding = new Path();
        padding.moveTo(coord_A[2], coord_A[3]); /*A*/
        padding.lineTo(coord_B[2], coord_B[3]); /*B*/
        padding.lineTo(coord_C[2], coord_C[3]); /*C*/
        padding.lineTo(coord_D[2], coord_D[3]); /*D*/
        padding.lineTo(coord_E[2], coord_E[3]); /*E*/
        padding.lineTo(coord_F[2], coord_F[3]); /*F*/
        padding.lineTo(coord_G[2], coord_G[3]); /*G*/
        padding.lineTo(coord_H[2], coord_H[3]); /*H*/
        padding.lineTo(coord_I[2], coord_I[3]); /*I*/
        padding.lineTo(coord_J[2], coord_J[3]); /*J*/
        padding.close();

        return padding;
	}

	private static Path drawPaddingLeft() {
		//Draw the padding Left
    	Path paddingLeft = new Path();
    	paddingLeft.moveTo(coord_A[0], coord_A[1]); /*A*/
    	paddingLeft.lineTo(coord_B[0], coord_B[1]); /*B*/
    	paddingLeft.lineTo(coord_C[0], coord_C[1]); /*C*/
    	paddingLeft.lineTo(coord_D[0], coord_D[1]); /*D*/
    	paddingLeft.lineTo(coord_E[0], coord_E[1]); /*E*/
    	paddingLeft.lineTo(coord_F[0], coord_F[1]); /*F*/
    	paddingLeft.close();

    	return paddingLeft;
	}

	private static Path drawPaddingRight() {
    	//Draw the padding Right
        Path paddingRight = new Path();
        paddingRight.moveTo(coord_A[0], coord_A[1]); /*A*/
        paddingRight.lineTo(coord_F[0], coord_F[1]); /*F*/
        paddingRight.lineTo(coord_G[0], coord_G[1]); /*G*/
        paddingRight.lineTo(coord_H[0], coord_H[1]); /*H*/
        paddingRight.lineTo(coord_I[0], coord_I[1]); /*I*/
        paddingRight.lineTo(coord_J[0], coord_J[1]); /*J*/
        paddingRight.close();

        return paddingRight;
	}

	private static void drawLine(Canvas canvas, int coordX, int coordY) {
    	//Draw the line
        canvas.drawLine(coord_A[coordX], coord_A[coordY], coord_B[coordX], coord_B[coordY], paintBlack); /*A-B*/
        canvas.drawLine(coord_B[coordX], coord_B[coordY], coord_C[coordX], coord_C[coordY], paintBlack); /*B-C*/
        canvas.drawLine(coord_C[coordX], coord_C[coordY], coord_D[coordX], coord_D[coordY], paintBlack); /*C-D*/
        canvas.drawLine(coord_D[coordX], coord_D[coordY], coord_E[coordX], coord_E[coordY], paintBlack); /*D-E*/
        canvas.drawLine(coord_E[coordX], coord_E[coordY], coord_F[coordX], coord_F[coordY], paintBlack); /*E-F*/
        canvas.drawLine(coord_F[coordX], coord_F[coordY], coord_G[coordX], coord_G[coordY], paintBlack); /*F-G*/
        canvas.drawLine(coord_G[coordX], coord_G[coordY], coord_H[coordX], coord_H[coordY], paintBlack); /*G-H*/
        canvas.drawLine(coord_H[coordX], coord_H[coordY], coord_I[coordX], coord_I[coordY], paintBlack); /*H-I*/
        canvas.drawLine(coord_I[coordX], coord_I[coordY], coord_J[coordX], coord_J[coordY], paintBlack); /*I-J*/
        canvas.drawLine(coord_J[coordX], coord_J[coordY], coord_A[coordX], coord_A[coordY], paintBlack); /*J-A*/
	}
}
