package com.coillighting.udder.effect;

import javax.sound.midi.*;  

import com.coillighting.udder.mix.TimePoint;
import com.coillighting.udder.model.Pixel;

/** Show a single color on all pixels. */
public class MidiMonochromeEffect extends EffectBase {

    private boolean dirty = false;
    private ShortMessage message = null;
	private Pixel color = null;
	

    public MidiMonochromeEffect(ShortMessage message) {
        this.setState(message);
    }

    public Class getStateClass() {
        return ShortMessage.class;
    }

    public Object getState() {
        if(this.message == null) {
            return null;
        } else {
            return this.message.clone();
        }
    }
	
	

    public void setState(Object state) throws ClassCastException {
		
		//System.out.println("MidiMonochromeEffect setState ");
		
		ShortMessage message = (ShortMessage) state;
		
        //MidiNote command = (MidiNote) state;
		
		// Integer noteNumber = command.getNoteNumber();
		// Integer velocity = command.getVelocity();
		
		//System.out.println(" setState message.getData1(): " + message.getData1());
		//System.out.println(" setState message.getData2(): " + message.getData2());
		//System.out.println(" setState message.getChannel(): " + message.getChannel());
		//System.out.println(" setState message.getCommand(): " + message.getCommand());
		
		Integer noteNumber = message.getData1();
		Integer velocity = message.getData2();
		
		Pixel monochromeColor = Pixel.black();
		
		Integer noteNoOctave = noteNumber % 16;
			
		//System.out.println("MidiMonochromeEffect noteNoOctave: "+ noteNoOctave);
			
		float hue = noteNoOctave / 12.0f;
		
		//System.out.format("MidiMonochromeEffect hue: %f \n", hue);
		
		float saturation = 1.0f;
		float brightness = 1.0f;
		
		Boolean ignoreCommand = false;
		
		switch (message.getCommand())
		{
		case 0x80:
		//  NOTE OFF
			//System.out.println("MidiMonochromeEffect Note Off.");
			brightness = 0.0f;
			break;
		case 0x90:
		//  NOTE ON.  
			//System.out.println("MidiMonochromeEffect Note On.");
			brightness = velocity / 127.0f;
			break;
		case 0xa0:
		//  POLY KEY PRESSURE
			ignoreCommand = true;
			break;
		case 0xd0:
		//  KEY PRESSURE
			ignoreCommand = true;
			break;
			
		case 0xc0:
		//  PROGRAM CHANGE
			ignoreCommand = true;
			break;
			
		case 0xb0:
		//  CONTROL CHANGE
			ignoreCommand = true;
			break;		
			
		default:
			ignoreCommand = true;
			break;	
		}
		
		//System.out.format("MidiMonochromeEffect brightness: %f \n", brightness);
		
		if(ignoreCommand == false) {
			monochromeColor.setHSBColor(hue,saturation,brightness);
		
	        monochromeColor.clip();
	        this.setNote(message);
			this.setColor(monochromeColor);
		}

		
    }

    public void setNote(ShortMessage message) {
        // Do not dirty this effect if color hasn't actually changed.
        if(message == null) {
            throw new NullPointerException("MidiMonochromeEffect requires a note.");
        } else if(this.message == null || !this.message.equals(message)) {
            this.message = (ShortMessage) message.clone();
            this.dirty = true;
        }
    }
	
    public void setColor(Pixel color) {
		
		//System.out.println("MidiMonochromeEffect setColor.");
		
        // Do not dirty this effect if color hasn't actually changed.
        if(color == null) {
            throw new NullPointerException("MonochromeEffect requires a color.");
        } else if(this.color == null || !this.color.equals(color)) {
            this.color = new Pixel(color);
            this.dirty = true;
        }
    }

    /** Draw pictures only when needed. */
    public void animate(TimePoint timePoint) {
        if(this.dirty) {
            if(this.pixels != null) {
				
		        //System.out.println("MidiMonochromeEffect Drawing.");
				
                for(int i=0; i<this.pixels.length; i++) {
                    this.pixels[i].setColor(this.color);
                }
            }
            this.dirty = false;
        }
    }

}
