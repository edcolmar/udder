package com.coillighting.udder.blend;

public class MaxBlendOp implements BlendOp {

    public float blend(float background, float foreground) {
        float val = background >= foreground ? background : foreground;
        if(val < 0.0f) {
            val = 0.0f;
        } else if(val > 1.0f) {
            val = 1.0f;
        }
        return val;
    }

}
