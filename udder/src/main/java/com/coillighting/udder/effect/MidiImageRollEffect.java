package com.coillighting.udder.effect;

import javax.sound.midi.*;  
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import java.awt.Color;

import com.coillighting.udder.mix.TimePoint;
import com.coillighting.udder.model.Device;
import com.coillighting.udder.model.Pixel;
import com.coillighting.udder.effect.MidiImageRollState;

import static com.coillighting.udder.util.LogUtil.log;

/** Plays an image vertically when a midi note arrives. */
public class MidiImageRollEffect extends EffectBase {

    private boolean dirty = false;
    private ShortMessage message = null;
	protected boolean noteon = false;
	private Pixel color = null;
	
    protected String filename = null;
    protected BufferedImage image = null;
    protected int imageWidth = 0;
    protected int imageHeight = 0;
	
	protected int yLocation = 0;
	
	protected float attack = 0.05f;
	
	protected float attackEnv = 1.0f;
	
	protected float release = 0.01f;
	
	protected float releaseEnv = 1.0f;
	
    // Scratch variables that we shouldn't reallocate on every
    // trip through the animation loop:
    private Pixel p;
	

    public MidiImageRollEffect(MidiImageRollState message) {
		
		System.out.println("MidiImageRollEffect INIT ");
        
		this.filename = "images/dairy_collection_A_720p/coppertone_trigrams.png";
		this.setState(message);
		
		this.reloadImage();
		
        // Initialize temps
        p = Pixel.black();
    }

    public Class getStateClass() {
        return MidiImageRollState.class;
    }

    public Object getState() {
        if(this.message == null) {
            return null;
        } else {
            return this.message.clone();
        }
    }
	
	

    public void setState(Object state) throws ClassCastException {
		
		//System.out.println("MidiImageRollEffect setState ");
		
		MidiImageRollState message = (MidiImageRollState) state;
		
		//System.out.println(message.getFilename());
		
        // does the message payload contain a new file to load?
        // Or is it just midi data
		if (message.getFilename() != null) {
			System.out.println("MidiImageRollEffect set new filename");
			this.filename = message.getFilename();
			this.reloadImage();
		}

		//System.out.println(" setState message.getData1(): " + message.getData1());
		//System.out.println(" setState message.getData2(): " + message.getData2());
		//System.out.println(" setState message.getChannel(): " + message.getChannel());
		//System.out.println(" setState message.getCommand(): " + message.getCommand());
		
		Integer noteNumber = message.getData1();
		Integer velocity = message.getData2();
        
		switch (message.getCommand())
		{
		case 0x80:
		//  NOTE OFF
			//System.out.println(" Note Off.");
			this.setNoteOff(message);
			break;
		case 0x90:
		//  NOTE ON.  
			//System.out.println(" Note On.");
			// brightness = velocity / 127.0f;
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
		//  CONTROL CHANGE
			//System.out.println("MidiImageRollEffect Control Change.");
			
			if (message.getData1() == 1) {
				if (message.getData2() > 0) {
					this.attack = message.getData2() / 127.0f / 3.0f;
				} else {
					this.attack = 0.01f;
				}
				//System.out.format("MidiImageRollEffect Set Attack: %f", this.attack);
			} else if (message.getData1() == 2) {
				if (message.getData2() > 0) {
					this.release = message.getData2() / 127.0f / 3.0f;
				} else {
					this.release = 0.01f;
				}
				//System.out.format("MidiImageRollEffect Set Release: %f", this.release);
			}
			
			break;		
			
		default:
			break;	
		}
		
    }

    public void setNote(MidiImageRollState message) {
        // Do not dirty this effect if color hasn't actually changed.
        if(message == null) {
            throw new NullPointerException("MidiImageRollEffect requires a note.");
        } else if(this.message == null || !this.message.equals(message)) {
            this.message = (ShortMessage) message.clone();
            this.dirty = true;
			this.noteon = true;
        }
    }
	
	public void setNoteOff(ShortMessage message) {
		this.message = (ShortMessage) message.clone();
        this.noteon = false;
	}
	

    /** Draw pictures only when needed. */
    public void animate(TimePoint timePoint) {
		if(this.dirty) {
			// System.out.println("MidiImageRollEffect this.dirty ");
			
	        if(image == null) {
				// System.out.println("MidiImageRollEffect Image is null ");
				
	            for(Pixel px: pixels) {
	                px.setBlack();
	            }
				
			} else {
				// Pixel offset by note number
				
                // FADES
				if (releaseEnv > 0.05f) {
					if (attackEnv > 0.99f) {
						//System.out.println(this.attackEnv);
			            for(int i=0; i<devices.length; i++) {
			                Device dev = devices[i];
							p.setRGBColor(image.getRGB(i, yLocation));
							p.scale(releaseEnv);
							pixels[i].setColor(p);
						}
						if (this.noteon == true) {
							//System.out.println("MidiMonochromeEffect SUSTAIN");
						} else {
							releaseEnv = releaseEnv - release;
						}

					} else {
			            for(int i=0; i<devices.length; i++) {
			                Device dev = devices[i];
							p.setRGBColor(image.getRGB(i, yLocation));
							p.scale(attackEnv);
							pixels[i].setColor(p);
						}
					attackEnv = attackEnv + attack;
					}
					yLocation = yLocation + 1;
					if (yLocation >= imageHeight) {
						yLocation = 1;
					}
					
				} else {
					
                    // CLIP THE VERY BOTTOM to prevent flickering
					releaseEnv = 1.0f;
					attackEnv = 0.0f;
					this.dirty = false;
					yLocation = 0;
					
		            for(Pixel px: pixels) {
		                px.setBlack();
		            }
				}
			}
		}
    }
	
    private void clearImage() {
        image = null;
        imageWidth = 0;
        imageHeight = 0;
    }
	
    /** If we can't load the image, log an error and proceed.
     *  Don't crash the server.
     */
    public void reloadImage() {
		System.out.println("MidiImageRollEffect reloadImage ");
        this.clearImage();
        if(filename != null) {
            File imageFile = new File(filename);
            if(!imageFile.exists()) {
                log("File not found: " + filename);
                filename = null;
            } else if(!imageFile.isFile()) {
                log("Not a regular file: " + filename);
                filename = null;
            } else {
                try {
                    image = ImageIO.read(imageFile);
                } catch(IOException iox) {
                    log("Error loading image " + filename + "\n" + iox);
                    filename = null;
                    return;
                }
                imageWidth = image.getWidth();
                imageHeight = image.getHeight();
                if(imageWidth == 0 || imageHeight == 0) {
                    log("Error loading " + filename + ": empty image.");
                    this.clearImage();
                    filename = null;
                }
            }
        } else {
            log("MidiImageRollEffect: no image to load.");
        }
    }

}
