package com.coillighting.udder.infrastructure;

import java.util.List;
import java.util.Queue;
import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;

import com.coillighting.udder.mix.Frame;
import com.coillighting.udder.mix.Mixer;
import com.coillighting.udder.mix.TimePoint;
import com.coillighting.udder.model.Pixel;
import com.coillighting.udder.util.TimingUtil;

import static com.coillighting.udder.util.LogUtil.log;

/** A ShowRunner owns all the infrastructure required to pump events through
 *  a Mixer which implements the current scenegraph. This object owns the scene
 *  itself, while its neighbors manage the infrastructure.
 *
 *  Runs in its own thread, started by ServicePipeline.
 *  Typically deployed as a singleton.
 */
public class ShowRunner implements Runnable {

    protected Queue<Command> commandQueue;
    protected Mixer mixer;
    protected Router router;
    protected List<Queue<Frame>> frameQueues;

    protected boolean verbose = false;

    // Timing measurements.
    // Normally (busyWait=false) fps roughly equals 1000/frameDelayMillis.
    public static int DEFAULT_FRAME_DELAY_MILLIS = 10;
    protected int frameDelayMillis = DEFAULT_FRAME_DELAY_MILLIS; // ignored if busywait
    protected boolean busyWait = false; // wait in a hot idle loop, not thread sleep
    protected long previousFrameRealTimeMillis = 0;
    protected long frameCounter = 0;
    
    private Boolean imageBufferWriteEnable = true;
    
    private BufferedImage imageBuffer = null;
    protected int imageBufferCurrentRow =1;
    protected int imageBufferCurrentColumn =1;
    protected int imageBufferMaxRowCount = 1000;
    protected int imageBufferSavedCount =0;
    
    private String filenamePrefix = "framebuffer-";
    private String filename = "framebuffer-0";
        
    
    private File imageFile = null;
    
    private Pixel[] pixels = null;

    public ShowRunner(Integer frameDelayMillis, Queue<Command> commandQueue, Mixer mixer,
        Router router, List<Queue<Frame>> frameQueues)
    {
        if(frameDelayMillis != null) {
            int delay = frameDelayMillis.intValue();
            if(delay < 1) {
                throw new IllegalArgumentException("Invalid frame delay (too short): "
                    + delay + " ms");
            } else {
                this.frameDelayMillis = delay;
            }
        }

        if(commandQueue==null) {
            throw new NullPointerException(
                "ShowRunner requires a queue that supplies commands.");
        } else if(mixer==null) {
            throw new NullPointerException(
                "ShowRunner requires a Mixer that defines the scene.");
        } else if(router==null) {
            throw new NullPointerException(
                "ShowRunner requires a Router to send commands to scene elements.");
        } else if(frameQueues==null) {
            throw new NullPointerException(
                "ShowRunner requires a list of queues for supplying frames to outputs.");
        } else if(frameQueues.size()==0) {
            throw new IllegalArgumentException(
                "ShowRunner requires at least one queue for supplying frames to outputs.");
        }
        this.commandQueue = commandQueue;
        this.mixer = mixer;
        this.router = router;
        this.frameQueues = frameQueues;
    }

    public void run() {
        try {
            // Immutable timepoint, passed down the chain to frames.
            TimePoint timePoint = new TimePoint();
            boolean sleepy=false;
            int droppedFrameCount = -1;
            final int droppedFrameLogInterval = 1001;

            log("Starting show.");

            while(true) {

                Command command = this.commandQueue.poll();

                // TODO Don't necessarily re-animate and re-render in response
                // to every command. Several densely spaced commands should be
                // able to effect the same rendered frame. Currently we respond
                // as fast as possible so that we can explore the response time
                // of the whole system. Eventually the max command queue polling
                // delay should be independent of the max frame rendering delay.

                if(command != null || !sleepy) {
                    sleepy = true;
                    if(command != null) {
                        String path = command.getPath();
                        Stateful dest = this.router.get(path);
                        try {
                            dest.setState(command.getValue());
                        } catch(Exception e) {
                            log("Failed to issue command to destination "
                                + dest + " at " + path + ": " + e); // TEMP?
                        }
                    }
                    timePoint = timePoint.next();

                    if(verbose) {
                        long time = timePoint.realTimeMillis();
                        long latency = time - previousFrameRealTimeMillis;

                        // The JVM system time only comes in millis, but the nano
                        // timers are a can of worms (and AFAIK system-dependent),
                        // so we count frames until the clock changes in order to
                        // estimate framerate.
                        if(latency > 0) {
                            log("Command latency <= " + latency + " ms (" + frameCounter + " frames / " + latency + " ms) = " + (1000 * frameCounter/latency) + " fps");
                            previousFrameRealTimeMillis = time;
                            frameCounter = 1;
                        } else {
                            ++frameCounter;
                        }
                    }

                    this.mixer.animate(timePoint);

                    // These pixels belong to mixer, and Effect's contract
                    // prohibits ShowRunner from sharing them beyond this
                    // point, so we'll make a copy per output frameQueue.
                    //
                    // FUTURE: Could use object pooling to recycle these Frames
                    // without reallocating when the downstream transmitter is
                    // done with them. This worked well in LD50.
                    Pixel[] mixerPixels = this.mixer.render();

                    int q=0;
                    for(Queue<Frame> frameQueue: frameQueues) {
                        Frame frame = Frame.createByCopy(timePoint, mixerPixels);
                        
                        if (imageBufferWriteEnable){
                        
                            // Write output frame to image buffer
                            // This probably does not belong here.
                            // It probably wants some kind of enable/disable toggle
                        
                            //System.out.format("ShowRunner imageBufferCurrentColumn: %d \n", this.imageBufferCurrentColumn);
                            // System.out.format("ShowRunner imageBufferCurrentRow: %d \n", this.imageBufferCurrentRow);
                        
                            if (imageBuffer == null) {
                                System.out.println("ShowRunner createImageBuffer()");
                                createImageBuffer();
                            }
                        
                            if (imageBufferCurrentRow < imageBufferMaxRowCount) {
                                // Add a row of pixel data to the output image
                                //System.out.println("ShowRunner imageBufferCurrentRow < imageBufferMaxRowCount");
                                pixels = frame.getPixels();
                                for(Pixel px: pixels) {
                                    this.imageBuffer.setRGB(this.imageBufferCurrentColumn, this.imageBufferCurrentRow, px.toRGB());
                                    this.imageBufferCurrentColumn++;
                                }
                                this.imageBufferCurrentColumn = 1;
                                this.imageBufferCurrentRow ++;
                            
                            } else if (imageBufferCurrentRow == imageBufferMaxRowCount) {
                                // Add a row of pixel data to the output image
                                // Save the image and reset.
                                //System.out.println("ShowRunner imageBufferCurrentRow == imageBufferMaxRowCount");
                                pixels = frame.getPixels();
                                for(Pixel px: pixels) {
                                    this.imageBuffer.setRGB(this.imageBufferCurrentColumn, this.imageBufferCurrentRow, px.toRGB());
                                    this.imageBufferCurrentColumn ++;
                                }
                            
                                try {
                                    filename = filenamePrefix + Integer.toString(this.imageBufferSavedCount);
                                    imageFile = File.createTempFile(filename, ".PNG");
                                    ImageIO.write(this.imageBuffer, "PNG", imageFile);
                                
                                    System.out.println(imageFile.getAbsolutePath() + " isFile: " + imageFile.isFile() + " isDir:" + imageFile.isDirectory());
                                
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                this.imageBufferCurrentColumn = 1;
                                this.imageBufferCurrentRow =1;
                                this.imageBufferSavedCount ++;
                            } else {
                                // Should not happen - reset
                            }
                        
                        }
                        

                        if(!frameQueue.offer(frame)) {
                            if(droppedFrameCount == -1) {
                                log("Frame queue #" + q + " (of " + frameQueues.size()
                                    + " queues) overflow. Dropped frame at " + timePoint);
                                droppedFrameCount = 1;
                            } else {
                                if(droppedFrameCount + 1 >= droppedFrameLogInterval) {
                                    log("Frame queue #" + q + " (of " + frameQueues.size()
                                        + " queues) overflow on frame at "
                                        + timePoint + ". Dropped " + droppedFrameCount
                                        + " frames since the previous message like this.");
                                    droppedFrameCount = 0;
                                } else {
                                    ++droppedFrameCount;
                                }
                            }
                        }
                        q++;
                    }
                } else if(busyWait) {
                    // EXPERIMENTAL: For load testing. Avoid busyWait in production.
                    // duration=10000 gave me 2000-5000 fps in a mix with
                    // more than 10 layers * 2 Kpixels, 1 of them animated.
                    // Performance degraded by roughly 20% when I animated 10 of
                    // them instead. Since then we've made several optimizations.
                    TimingUtil.waitBusy(10000);
                    sleepy = false;
                } else {
                    // Our crude timing mechanism currently does not account for
                    // the cost of processing each command.
                    this.waitSleepy(this.frameDelayMillis);
                    sleepy = false;
                }
            }
        } catch(InterruptedException e) {
            log("Stopping show.");
        }
    }

    // We break this out into a separate method so that a profiler can easily
    // distinguish between a real hotspot and a quick nap.
    protected void waitSleepy(long Duration) throws InterruptedException {
        Thread.sleep(this.frameDelayMillis);
    }
    
    protected void createImageBuffer(){
        BufferedImage imageBufferNew = new BufferedImage(500, imageBufferMaxRowCount +1,BufferedImage.TYPE_INT_RGB );
        this.imageBuffer = imageBufferNew;
    }

}
