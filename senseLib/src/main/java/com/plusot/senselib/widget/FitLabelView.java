/*******************************************************************************
 * Copyright (c) 2012 Plusot Biketech
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Peter Bruinink - initial API and implementation and/or initial documentation
 *******************************************************************************/

package com.plusot.senselib.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.plusot.senselib.R;


public class FitLabelView extends View {
	private Paint mTextPaint;
	private String mText;
	private float heightPercentage = 1.0f;
	private float widthPercentage = 1.0f;
	private int orgAlpha = 0xFF;
	private Rect rect = new Rect();
	private Paint.Align align = Paint.Align.CENTER;
	

	public FitLabelView(Context context) {
		super(context);
		initView();
	}

	public FitLabelView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();

		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.LabelView);

		CharSequence s = a.getString(R.styleable.LabelView_text);
		if (s != null) {
			setText(s.toString());
		} 

		setTextColor(a.getColor(R.styleable.LabelView_textColor, 0xFF000000));
		orgAlpha = a.getInt(R.styleable.LabelView_textAlpha, 0xFF);
		setAlpha(orgAlpha);

		heightPercentage = a.getFloat(R.styleable.LabelView_heightPercentage, 1.0f);
		widthPercentage = a.getFloat(R.styleable.LabelView_widthPercentage, 1.0f);
		
		switch(a.getInt(R.styleable.LabelView_textAlign, 0)) {
		case 1: align = Paint.Align.LEFT; break;
		case 2: align = Paint.Align.RIGHT; break;
		default: align = Paint.Align.CENTER; break;
		}

		a.recycle();
	}

	private final void initView() {
		mTextPaint = new Paint();
		mTextPaint.setAntiAlias(true);
		mTextPaint.setTextSize(16);
		setBackgroundColor(0x00FFFFFF);
		mTextPaint.setColor(0xFF000000);
		mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
	}

	public void setText(String text) {
		if (text.equals(mText)) return;
		mText = text.replaceAll("[\r\t\n]", " ");
		requestLayout();
		invalidate();
	}

	public void setWidthPercentage(float widthPercentage) {
		this.widthPercentage = widthPercentage;
		requestLayout();
		invalidate();
	}

	public void setHeightPercentage(float heightPercentage) {
		this.heightPercentage = heightPercentage;
		requestLayout();
		invalidate();
	}

	public void setTextColor(int color) {
		mTextPaint.setColor(color);
		invalidate();
	}

	public void setAlpha(int alpha) {
		if (alpha < 0 || alpha > 255) {
			restoreAlpha();
			return;
		}
		if (mTextPaint.getAlpha() != alpha) mTextPaint.setAlpha(alpha);
	}

	public void restoreAlpha() {
		if (mTextPaint.getAlpha() != orgAlpha) mTextPaint.setAlpha(orgAlpha);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
		int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
		if (parentHeight == 0) {
			WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
			if (wm != null) {
				Display display = wm.getDefaultDisplay();
				parentHeight = display.getHeight();
			} else
				parentHeight = 640; //this.getRootView().getHeight();
		}
		if (parentWidth == 0) {
			WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
			if (wm != null) {
				Display display = wm.getDefaultDisplay();
				parentWidth = display.getWidth();
			} else
				parentWidth = 480;
		}

		setMeasuredDimension((int) (widthPercentage * parentWidth), (int)(heightPercentage * parentHeight));
		//setMeasuredDimension((int) (widthPercentage * parentWidth + this.getPaddingLeft() + this.getPaddingRight()), (int)(heightPercentage * parentHeight + this.getPaddingBottom() + this.getPaddingTop()));
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mText == null || mText.length() == 0) return;
		float height = getHeight();
		float width = getWidth();
		float effectiveHeight = height - getPaddingTop() - getPaddingBottom();
		float effectiveWidth = width - getPaddingLeft() - getPaddingRight();

		mTextPaint.setTextSize(360);
		Paint.FontMetrics metrics = mTextPaint.getFontMetrics();
		float textHeight = metrics.descent - metrics.ascent; //rect.height();

		//mTextPaint.setFakeBoldText(true);
		mTextPaint.getTextBounds(mText, 0, mText.length(), rect);
		float textWidth = rect.width(); 
		float textSize = mTextPaint.getTextSize();

		float widthRatio = textWidth / effectiveWidth;
		float heightRatio = textHeight / effectiveHeight;
		String[] texts = null;
		int iLines = 1;
		String[] lines = null;
		if (widthRatio > 1f || heightRatio > 1f) {
			if (widthRatio > 1.5f * heightRatio && (mText.contains(" "))) {
				texts = mText.split(" ");
				lines = new String[texts.length];
				float maxWidth = 0;
				for (String text: texts) {
					mTextPaint.getTextBounds(text, 0, text.length(), rect);
					if (rect.width() > maxWidth) {
						maxWidth = rect.width(); 
					}
				}
				//maxWidth *= 2f;
				int iStep = 0;
				do {
					String temp = null;
					if (iLines > 1) maxWidth *= 1.5f;
					iLines = 0;
					for (String text: texts) {
						mTextPaint.getTextBounds(text, 0, text.length(), rect);
						if (rect.width() < maxWidth) {
							if (temp != null) {
								String test = temp + " " + text;						
								mTextPaint.getTextBounds(test, 0, test.length(), rect);
								if (rect.width() > maxWidth) {
									lines[iLines++] = temp;
									temp = text;
								} else {
									temp = test;
								}
							} else
								temp = text;
						} else {
							if (temp != null) {
								lines[iLines++] = temp;
								temp = null;
							}
							lines[iLines++] = text;
						}
					}
					if (temp != null) lines[iLines++] = temp;

				} while ((heightRatio * iLines > maxWidth / effectiveWidth) && (iStep++ < 5)) ;
				widthRatio = maxWidth / effectiveWidth;
				heightRatio *= iLines;
			}
			if (widthRatio > heightRatio) {				
				mTextPaint.setTextSize(0.95f * textSize / widthRatio);
			} else {
				mTextPaint.setTextSize(0.95f * textSize / heightRatio);
			}
			textSize = mTextPaint.getTextSize();
			//mTextPaint.getTextBounds(mText, 0, mText.length(), rect);
			metrics = mTextPaint.getFontMetrics();
			textHeight = metrics.descent - metrics.ascent; 
			//textWidth = rect.width(); 
		}

		mTextPaint.setTextAlign(align);
		if (iLines == 1 || lines == null) {
			switch(align) {
			case CENTER: canvas.drawText(mText.replace("~", " ").trim(), getPaddingLeft() + effectiveWidth / 2, getPaddingTop() + effectiveHeight / 2 - metrics.ascent / 2 - metrics.descent / 2, mTextPaint); break;
			case LEFT: canvas.drawText(mText.replace("~", " ").trim(), getPaddingLeft(), getPaddingTop() + effectiveHeight / 2 - metrics.ascent / 2 - metrics.descent / 2, mTextPaint); break;
			case RIGHT: canvas.drawText(mText.replace("~", " ").trim(), getPaddingLeft() + effectiveWidth, getPaddingTop() + effectiveHeight / 2 - metrics.ascent / 2 - metrics.descent / 2, mTextPaint); break;
			}
		} else {
			float heightPerLine = effectiveHeight / iLines;
			float yStart = 0;
			if (textHeight * iLines < effectiveHeight) {
				heightPerLine = textHeight;
				yStart = getPaddingTop() + effectiveHeight / 2 - heightPerLine * iLines / 2;
			}
			for (int iLine = 0; iLine < iLines; iLine++) {
				canvas.drawText(lines[iLine].replace("~", " "), getPaddingLeft() + effectiveWidth / 2, yStart + (0.5f + iLine) * heightPerLine - metrics.ascent / 2 - metrics.descent / 2, mTextPaint);
			}		

		}

	}
}
