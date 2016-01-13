package com.coillighting.udder.scene;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.io.File.separator;

import javax.sound.midi.*;  

import com.coillighting.udder.blend.BlendOp;
import com.coillighting.udder.blend.MaxBlendOp;
import com.coillighting.udder.blend.MultiplyBlendOp;
import com.coillighting.udder.blend.MaskBlendOp;
import com.coillighting.udder.effect.MonochromeEffect;
import com.coillighting.udder.effect.MidiMonochromeEffect;
import com.coillighting.udder.effect.MidiImageRollEffect;
import com.coillighting.udder.effect.MidiSparkleEffect;
import com.coillighting.udder.effect.MidiImageRollState;
import com.coillighting.udder.effect.woven.WovenEffect;
import com.coillighting.udder.mix.Layer;
import com.coillighting.udder.mix.Mixable;
import com.coillighting.udder.mix.Mixer;
import com.coillighting.udder.model.Device;
import com.coillighting.udder.model.Pixel;

/** Define the scenegraph for the December, 2014 weavers' conference at
 *  Boulder's Dairy Center for the Arts (thedairy.org). An Udder scenegraph
 *  has as its root a Mixer object, and each layer in the scene is backed by
 *  a Layer child of that Mixer.
 */
public abstract class NoiseForestScene {

    /** Instantiate a new scene in the form of a Mixer. */
    public static Mixer create(Device[] devices) {
        BlendOp max = new MaxBlendOp();
        BlendOp mult = new MultiplyBlendOp();
		BlendOp mask = new MaskBlendOp();

        // Add layers from bottom (background) to top (foreground),
        // in order of composition.
        ArrayList<Mixable> layers = new ArrayList<Mixable>();

        // A basic three-layer look to get started.

        // The background is additive (unlike the gel layer
        // below), so add color globally using this level.
		
		MidiImageRollState shortmessage = new MidiImageRollState();
		
        Layer background = new Layer("Background",
            new MidiMonochromeEffect(shortmessage));
        background.setBlendOp(max);
        layers.add(background);
		
        Layer imageRoll1 = new Layer("imageRoll1-MAX",
            new MidiImageRollEffect(shortmessage));
        imageRoll1.setBlendOp(max);
        layers.add(imageRoll1);
		
        Layer imageRoll2 = new Layer("imageRoll2-MAX",
            new MidiImageRollEffect(shortmessage));
        imageRoll2.setBlendOp(max);
        layers.add(imageRoll2);
		
        Layer imageRoll3 = new Layer("imageRoll3-MAX",
            new MidiImageRollEffect(shortmessage));
        imageRoll3.setBlendOp(max);
        layers.add(imageRoll3);
		
        Layer imageRoll4 = new Layer("imageRoll4-MAX",
            new MidiImageRollEffect(shortmessage));
        imageRoll4.setBlendOp(max);
        layers.add(imageRoll4);
		
        Layer imageRoll5 = new Layer("imageRoll5-MAX",
            new MidiImageRollEffect(shortmessage));
        imageRoll5.setBlendOp(max);
        layers.add(imageRoll5);
		
        Layer imageRoll6 = new Layer("imageRoll6-MAX",
            new MidiImageRollEffect(shortmessage));
        imageRoll6.setBlendOp(max);
        layers.add(imageRoll6);
		
        
        Layer imageRoll7 = new Layer("imageRoll7-MASK",
            new MidiImageRollEffect(shortmessage));
        imageRoll7.setBlendOp(mask);
        layers.add(imageRoll7);

        
        Layer sparkle = new Layer("sparkle",
            new MidiSparkleEffect(shortmessage));
        sparkle.setBlendOp(max);
        layers.add(sparkle);
        
        

        // In the mult blendop, white=transparent. Tint
        // everything globally by adjusting this color.
        Layer gel = new Layer("Color correction gel", new MonochromeEffect(Pixel.white()));
        gel.setBlendOp(mult);
        layers.add(gel);

        Mixer mixer = new Mixer((Collection<Mixable>) layers);
        mixer.patchDevices(devices);
        System.out.println("Patched " + devices.length
            + " devices to the NoiseForest's Mixer.");

        for(Mixable layer: mixer) {
            layer.setLevel(1.0f);
        }
        mixer.setLevel(1.0f);

        System.out.println(mixer.getDescription());

        return mixer;
    }

}
