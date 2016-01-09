package com.coillighting.udder.effect;

import javax.sound.midi.*;  

import com.coillighting.udder.mix.TimePoint;
import com.coillighting.udder.model.Pixel;

/** Show a single color on all pixels. */
public class MidiMonochromeEffect extends EffectBase {

    private boolean dirty = false;
    private ShortMessage message = null;
	protected boolean noteon = false;
	private Pixel color = null;
	
	protected float attack = 0.05f;
	
	protected float attackEnv = 1.0f;
	
	protected float release = 0.01f;
	
	protected float releaseEnv = 1.0f;
    
    protected float saturation = 1.0f;
    
    protected float brightness = 1.0f;
    
    protected float coverage = 1.0f;
	

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
		//boolean noteon = false;
		
        //MidiNote command = (MidiNote) state;
		
		// Integer noteNumber = command.getNoteNumber();
		// Integer velocity = command.getVelocity();
		
		//System.out.println(" setState message.getData1(): " + message.getData1());
		//System.out.println(" setState message.getData2(): " + message.getData2());
		//System.out.println(" setState message.getChannel(): " + message.getChannel());
		//System.out.println(" setState message.getCommand(): " + message.getCommand());
		
		Integer noteNumber = message.getData1();
		Integer velocity = message.getData2();
		
		//float saturation = 1.0f;
		//float brightness = 1.0f;
        
        float coverage = 0.1f;
		
		Pixel monochromeColor = Pixel.black();
		
		Integer noteNoOctave = noteNumber % 16;
			
		//System.out.println("MidiMonochromeEffect noteNoOctave: "+ noteNoOctave);
			
		float hue = noteNoOctave / 12.0f;
		
		//System.out.format("MidiMonochromeEffect hue: %f \n", hue);
		
		
		Boolean ignoreCommand = false;
		
		switch (message.getCommand())
		{
		case 0x80:
		//  NOTE OFF
			//System.out.println("MidiMonochromeEffect Note Off.");
			// brightness = 0.0f;
			this.setNoteOff(message);
			break;
		case 0x90:
		//  NOTE ON.  
			//System.out.println("MidiMonochromeEffect Note On.");
			this.setNote(message);
			
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
		//System.out.println("MidiMonochromeEffect Control Change.");
		
		if (message.getData1() == 1) {
			if (message.getData2() > 0) {
				this.attack = message.getData2() / 127.0f / 3.0f;
			} else {
				this.attack = 0.01f;
			}
			System.out.format("MidiMonochromeEffect Set Attack: %f", this.attack);
		} else if (message.getData1() == 2) {
			if (message.getData2() > 0) {
				this.release = message.getData2() / 127.0f / 3.0f;
			} else {
				this.release = 0.01f;
			}
			//System.out.format("MidiMonochromeEffect Set Release: %f", this.release);
		} else if (message.getData1() == 3) {
			if (message.getData2() > 0) {
				this.saturation = message.getData2() / 127.0f ;
			} else {
				this.saturation = 0.01f;
			}
			//System.out.format("MidiMonochromeEffect Set Saturation: %f", this.saturation);
            
		} 
			ignoreCommand = true;
			break;		
			
		default:
			ignoreCommand = true;
			break;	
		}
		
		//System.out.format("MidiMonochromeEffect brightness: %f \n", brightness);
		
		if(ignoreCommand == false) {
			monochromeColor.setHSBColor(hue,this.saturation,this.brightness);
		
	        monochromeColor.clip();
	        //this.setNote(message);
			this.setColor(monochromeColor);
		}

		
    }

    public void setNote(ShortMessage message) {
        // Do not dirty this effect if color hasn't actually changed.
        if(message == null) {
            throw new NullPointerException("MidiMonochromeEffect requires a note.");
        } else if(this.message == null || !this.message.equals(message)) {
            this.message = (ShortMessage) message.clone();
			this.brightness =  127.0f;
            this.dirty = true;
			this.noteon = true;
        }
    }
	public void setNoteOff(ShortMessage message) {
		this.message = (ShortMessage) message.clone();
        this.noteon = false;
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
				
				//fades
				if (this.releaseEnv > 0.05f) {
					//System.out.println("MidiMonochromeEffect ReleaseEnv.");
					//System.out.println(this.releaseEnv);
					if (this.attackEnv > 0.99f) {
						//System.out.println("MidiMonochromeEffect AttackEnv.");
						//System.out.println(this.attackEnv);
						if (this.noteon == true) {
							//nothing here...
							// Sustain if the note still held down
							//System.out.println("MidiMonochromeEffect SUSTAIN");
						} else {
							
							//System.out.println("MidiMonochromeEffect Note is not on.");
							// RELEASE ENVELOPE
			                for(int i=0; i<this.pixels.length; i++) {
			                    this.pixels[i].setColor(this.color);
								this.pixels[i].scale(this.releaseEnv);
			                }
							this.releaseEnv = this.releaseEnv - this.release;
							
						}
					} else {
						//System.out.println("MidiMonochromeEffect Attack.");
						
						// ATTACK ENVELOPE
		                for(int i=0; i<this.pixels.length; i++) {
		                    this.pixels[i].setColor(this.color);
							this.pixels[i].scale(this.attackEnv);
		                }
						this.attackEnv = this.attackEnv + this.attack;
					}
				
				} else {
					// CLIP THE BOTTOM to prevent flickering
					//System.out.println("MidiMonochromeEffect Clip bottom.");
					this.releaseEnv = 1.0f;
					this.attackEnv = 0.0f;
					this.dirty = false;
		            for(Pixel px: pixels) {
		                px.setBlack();
		            }
				}
            }
        }
    }

}
