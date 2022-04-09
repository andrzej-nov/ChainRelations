package com.andrzejn.chainrelations.logic

import aurelienribon.tweenengine.TweenAccessor

const val TW_EYE_HK: Int = 1
const val TW_ALPHA: Int = 2

/**
 * Used by the Tween Engine to access field properties
 */
class BallAccessor : TweenAccessor<Ball> {
    /**
     * Gets one or many values from the target object associated to the given tween type.
     * It is used by the Tween Engine to determine starting values.
     */
    override fun getValues(target: Ball?, tweenType: Int, returnValues: FloatArray?): Int {
        when (tweenType) {
            TW_EYE_HK -> returnValues!![0] = target!!.eyeHk
            TW_ALPHA -> returnValues!![0] = target!!.alpha
        }
        return 1
    }

    /**
     * This method is called by the Tween Engine each time
     * a running tween associated with the current target object has been updated.
     */
    override fun setValues(target: Ball?, tweenType: Int, newValues: FloatArray?) {
        when (tweenType) {
            TW_EYE_HK -> (target ?: return).eyeHk = (newValues ?: return)[0]
            TW_ALPHA -> (target ?: return).alpha = (newValues ?: return)[0]
        }
    }
}