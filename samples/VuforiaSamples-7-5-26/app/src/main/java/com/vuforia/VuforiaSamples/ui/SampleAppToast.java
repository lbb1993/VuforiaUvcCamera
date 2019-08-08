/*===============================================================================
Copyright (c) 2018 PTC Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other
countries.
===============================================================================*/

package com.vuforia.VuforiaSamples.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vuforia.VuforiaSamples.R;


/**
 * This class configures and creates a custom Toast that can be used throughout the sample
 *
 * To change the length of time the toast appears on screen, modify FADE_IN_OUT_DURATION (ms)
 * For additional configuration, modify the AnimationListener
 */
public class SampleAppToast
{
    private final View mToastView;
    private final TextView mTextMessageView;
    private final Animation mFadeIn;
    private final Animation mFadeOut;

    private static final int FADE_IN_OUT_DURATION = 2000;

    public SampleAppToast(Context context, ViewGroup parentView, View placementReferenceView, boolean placeAboveView)
    {
        mToastView = View.inflate(context, R.layout.sample_app_toast_view, null);
        mTextMessageView = (TextView)mToastView.findViewById(R.id.toast_text_view);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

        params.addRule(placeAboveView ? RelativeLayout.ABOVE : RelativeLayout.BELOW, placementReferenceView.getId());
        params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);

        mToastView.setLayoutParams(params);
        mToastView.setVisibility(View.GONE);

        parentView.addView(mToastView);

        mFadeIn = new AlphaAnimation(0, 1);
        mFadeIn.setInterpolator(new DecelerateInterpolator());
        mFadeIn.setDuration(FADE_IN_OUT_DURATION);

        mFadeOut = new AlphaAnimation(1, 0);
        mFadeOut.setInterpolator(new DecelerateInterpolator());
        mFadeOut.setDuration(FADE_IN_OUT_DURATION);
        mFadeOut.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation arg0) {
            }
            @Override
            public void onAnimationRepeat(Animation arg0) {
            }
            @Override
            public void onAnimationEnd(Animation arg0) {
                mToastView.setVisibility(View.GONE);
            }
        });

    }

    public void hideToast()
    {
        if(mToastView.getVisibility() == View.VISIBLE)
        {
            mToastView.startAnimation(mFadeOut);
        }
    }

    public void showToast(String message)
    {
        mToastView.startAnimation(mFadeIn);
        mTextMessageView.setText(message);
        mToastView.setVisibility(View.VISIBLE);
    }
}
