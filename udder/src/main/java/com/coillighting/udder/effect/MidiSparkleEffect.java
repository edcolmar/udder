package com.coillighting.udder.effect;

import javax.sound.midi.*;  

import java.util.Random;

import com.coillighting.udder.mix.TimePoint;
import com.coillighting.udder.model.Pixel;
import com.coillighting.udder.model.Device;

/** Show a single color on all pixels. */
public class MidiSparkleEffect extends EffectBase {

    private boolean dirty = false;
    private ShortMessage message = null;
    
	protected float attack = 0.05f;
	
	protected float attackEnv = 1.0f;
	
	protected float release = 0.01f;
	
	protected float releaseEnv = 1.0f;
    
    protected float saturation = 0.0f;
    
    protected float brightness = 1.0f;
    
    protected float coverage = 0.0001f;
    
    // Scratch variables that we shouldn't reallocate on every
    // trip through the animation loop:
    private Pixel p;
	
    private Random random = new Random();

    public MidiSparkleEffect(ShortMessage message) {
        // Initialize temps
        p = Pixel.black();
        
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
		
		//System.out.println("MidiSparkleEffect setState ");
		
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
		
		float saturation = 0.0f;
		float brightness = 1.0f;
        
        float coverage = 0.001f;
        
        
		
		
		switch (message.getCommand())
		{
		case 0x80:
		//  NOTE OFF
			//System.out.println("MidiSparkleEffect Note Off.");
			this.brightness = 0.0f;
            
			break;
		case 0x90:
		//  NOTE ON.  
			//System.out.println("MidiSparkleEffect Note On.");
			this.brightness = velocity / 127.0f;
            this.setNote(message);
			break;
		case 0xa0:
		//  POLY KEY PRESSURE
			break;
		case 0xd0:
		//  KEY PRESSURE
			break;
			
		case 0xc0:
		//  PROGRAM CHANGE
			break;
			
		case 0xb0:
        // CONTROL CHANGE
		//System.out.println("MidiSparkleEffect Control Change.");
		
		if (message.getData1() == 1) {
			if (message.getData2() > 0) {
				this.attack = message.getData2() / 127.0f / 3.0f;
			} else {
				this.attack = 0.01f;
			}
			//System.out.format("MidiSparkleEffect Set Attack: %f", this.attack);
		} else if (message.getData1() == 2) {
			if (message.getData2() > 0) {
				this.release = message.getData2() / 127.0f / 3.0f;
			} else {
				this.release = 0.01f;
			}
			//System.out.format("MidiSparkleEffect Set Release: %f", this.release);
		} else if (message.getData1() == 3) {
			if (message.getData2() > 0) {
				this.saturation = message.getData2() / 127.0f ;
			} else {
				this.saturation = 0.01f;
			}
			//System.out.format("MidiSparkleEffect Set Saturation: %f", this.saturation);
            
		} else if (message.getData1() == 4) {
			if (message.getData2() > 0) {
				this.coverage = message.getData2() / 10000.0f ;
			} else {
				this.coverage = 0.0001f;
			}
			//System.out.format("MidiSparkleEffect Set Saturation: %f", this.saturation);
            
        }
			
		break;		
		
	default:
		break;	
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
	

    /** Draw pictures only when needed. */
    public void animate(TimePoint timePoint) {
		if(this.dirty) {
			// System.out.println("MidiSparkleEffect this.dirty ");
			
			            for(int i=0; i<devices.length; i++) {
                            
                            // double hue = Math.random();
			                Device dev = devices[i];
                            if (this.random.nextFloat() < this.coverage){
    							p.setHSBColor(this.random.nextFloat(), this.saturation,  this.brightness);
    							p.scale(releaseEnv);
                            } else  {
                                p.setBlack();
                            }

							pixels[i].setColor(p);
						}
					
			
		}
    }

}
