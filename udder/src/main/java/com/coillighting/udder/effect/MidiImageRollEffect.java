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

import static com.coillighting.udder.util.LogUtil.log;

/** Show a single color on all pixels. */
public class MidiImageRollEffect extends EffectBase {

    private boolean dirty = false;
    private ShortMessage message = null;
	private Pixel color = null;
	
    protected String filename = null;
    protected BufferedImage image = null;
    protected int imageWidth = 0;
    protected int imageHeight = 0;
	
	protected int yLocation = 0;
	
	protected float release = 0.01f;
	
	protected float releaseEnv = 1.0f;
	
    // Scratch variables that we shouldn't reallocate on every
    // trip through the animation loop:
    private Pixel p, pEnv;
	

    public MidiImageRollEffect(ShortMessage message) {
		
		System.out.println("MidiImageRollEffect INIT ");
        
		this.filename = "images/dairy_collection_A_720p/coppertone_trigrams.png";
		this.setState(message);
		
		this.reloadImage();
		
        // Initialize temps
        p = Pixel.black();
		pEnv = Pixel.black();
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
		
		// System.out.println("MidiImageRollEffect setState ");
		
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
		
		//System.out.format("MidiMonochromeEffect hue: %f \n", hue);
		
		// float brightness = 1.0f;
		
		Boolean ignoreCommand = false;
		
		switch (message.getCommand())
		{
		case 0x80:
		//  NOTE OFF
			//System.out.println("MidiMonochromeEffect Note Off.");
			ignoreCommand = true;
			break;
		case 0x90:
		//  NOTE ON.  
			//System.out.println("MidiMonochromeEffect Note On.");
			// brightness = velocity / 127.0f;
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
			ignoreCommand = true;
			break;		
			
		default:
			ignoreCommand = true;
			break;	
		}
		
    }

    public void setNote(ShortMessage message) {
        // Do not dirty this effect if color hasn't actually changed.
        if(message == null) {
            throw new NullPointerException("MidiImageRollEffect requires a note.");
        } else if(this.message == null || !this.message.equals(message)) {
            this.message = (ShortMessage) message.clone();
            this.dirty = true;
        }
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
				
				if (releaseEnv > 0.05f) {
					
					// System.out.format("releaseEnv: %f ", releaseEnv);
					
		            for(int i=0; i<devices.length; i++) {
		                Device dev = devices[i];
						p.setRGBColor(image.getRGB(i, yLocation));
				
						// pEnv.setColor(p.r * releaseEnv,p.g * releaseEnv ,p.b * releaseEnv );
						
						p.scale(releaseEnv);
				
		        		pixels[i].setColor(p);
						
					}
					
					releaseEnv = releaseEnv - release;
					yLocation = yLocation + 1;
					
				} else {
					
					releaseEnv = 1.0f;
					this.dirty = false;
					yLocation = 0;
					
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
