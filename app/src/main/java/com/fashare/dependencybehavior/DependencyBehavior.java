package com.fashare.dependencybehavior;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.IdRes;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * User: fashare(153614131@qq.com)
 * Date: 2016-12-16
 * Time: 21:16
 * <br/><br/>
 *
 * 一个 "建立 children 之间依赖" 的 Behavior
 * child 紧跟在 dependencyView 的右边
 */
public class DependencyBehavior extends CoordinatorLayout.Behavior<View> {
    public static final String TAG = "DependencyBehavior";

    @IdRes int mDependencyId = View.NO_ID;
    boolean mIsTopOfCenter = false;
    float mMarginCenter = 150;

    private Context mContext;

    public DependencyBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initAttrs(context, attrs);
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.DependencyBehavior);
        mDependencyId = ta.getResourceId(R.styleable.DependencyBehavior_mDependencyId, mDependencyId);
        mIsTopOfCenter = ta.getBoolean(R.styleable.DependencyBehavior_mIsTopOfCenter, mIsTopOfCenter);
        mMarginCenter = ta.getDimension(R.styleable.DependencyBehavior_mMarginCenter, mMarginCenter);
        ta.recycle();
    }

    // 根据 mDependencyId, 指定依赖的对象
    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return mDependencyId == dependency.getId();
    }

    // child 紧跟在 dependencyView 的右边
    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
        int marginLeft = (int)mContext.getResources().getDimension(R.dimen.activity_vertical_margin) * 2;

        float center = dependency.getY() + dependency.getHeight() / 2,
                sign = mIsTopOfCenter ? -1: 1;

//        child.setX(dependency.getX() + dependency.getWidth() + marginLeft);
//        child.setY(center - child.getHeight()/2 + (int)mMarginCenter * sign);

        child.animate()
                .x(dependency.getX() + dependency.getWidth() + marginLeft)
                .y(center - child.getHeight()/2 + (int)mMarginCenter * sign)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        return true;
    }
}
