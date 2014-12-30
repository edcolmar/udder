package com.coillighting.udder.blend;

public class MaskBlendOp implements BlendOp {

    public float blend(float background, float foreground) {
		float val = 0.0f;
		if (foreground < 0.5f){
			val = background
		}
        return val;
    }

    public String toString() {
        return "mask";
    }

}
