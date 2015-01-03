package com.coillighting.udder.infrastructure;

import java.util.Queue;
import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;

import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.coillighting.udder.mix.Frame;
import com.coillighting.udder.model.Pixel;

import static com.coillighting.udder.util.LogUtil.log;

/** First stab at an Image rendering client.
 *  This class is responsible for translating the pixels in mixed down frames
 *  into a png image.
 *
 *  Runs in its own thread, kicked off by ServicePipeline.
 *  Typically deployed as a singleton.
 *
 *  Note: If the OPC server says "OPC: Source -1 does not exist", check the
 *  very first error message. Its assigned port is probably already in use.
 *
 *  Note: WATCH OUT FOR JAVA'S EVIL SIGNED BYTES.
 */
public class ImageRenderTransmitter implements Transmitter {

    protected BlockingQueue<Frame> frameQueue;
    protected int maxDelayMillis;
    protected Socket socket;
    protected DataOutputStream dataOutputStream;
    //protected String serverHost;
    //protected int serverPort;
    protected int[] deviceAddressMap; // see PatchSheet.deviceAddressMap
    protected long previousFrameRealTimeMillis = 0;

    protected final boolean verbose = false;
    protected final boolean debug = true;
    
    private Boolean imageBufferWriteEnable = true;
    
    private BufferedImage imageBuffer = null;
    protected int imageBufferCurrentRow =1;
    protected int imageBufferCurrentColumn =1;
    protected int imageBufferMaxRowCount = 1000;
    protected int imageBufferSavedCount =0;
    
    private String filenamePrefix = "framebuffer-";
    private String filename = "framebuffer-0";
        
    private File imageFile = null;

    // Keep one black pixel to skip needless reallocations:
    private final Pixel black = new Pixel(0.0f, 0.0f, 0.0f);

    public ImageRenderTransmitter( //SocketAddress opcServerAddr,
                          BlockingQueue<Frame> frameQueue,
                          int[] deviceAddressMap)
    {
        this.setFrameQueue(frameQueue);
        this.maxDelayMillis = 15000; // FUTURE: allow the user to tune this

        //this.serverHost = opcServerAddr.getHost();
        //this.serverPort = opcServerAddr.getPort();
        this.deviceAddressMap = deviceAddressMap;
    }

    public void setFrameQueue(BlockingQueue<Frame> frameQueue) {
        this.frameQueue = frameQueue;
    }
    
    protected void createImageBuffer(int imageWidth, int imageHeight){
        BufferedImage imageBufferNew = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB );
        this.imageBuffer = imageBufferNew;
    }

    // Broken out into a separate method for easy profiling.
    // Some profilers otherwise have a hard time distinguishing between
    // time spent in run() and time spend waiting for the next frame.
    protected Frame pollFrameQueue() throws InterruptedException {
        return this.frameQueue.poll(this.maxDelayMillis,
                TimeUnit.MILLISECONDS);
    }

    protected void writeOPCPixels(byte[] message, Pixel[] pixels) {
        int i = OpcHeader.SUBPIXEL_START;
        for (int deviceIndex : deviceAddressMap) {
            Pixel pixel;
            if (deviceIndex < 0 || deviceIndex >= pixels.length) {
                pixel = black;
            } else {
                pixel = pixels[deviceIndex];
            }
            message[i] = (byte) (0xFF & (int) (255.99999f * pixel.r));
            message[i + 1] = (byte) (0xFF & (int) (255.99999f * pixel.g));
            message[i + 2] = (byte) (0xFF & (int) (255.99999f * pixel.b));
            i += 3;
        }
    }

    public void run() {
        try {
            log("Starting Image Render transmitter " + this);

            if(frameQueue==null) {
                throw new NullPointerException(
                        "ImageRenderTransmitter requires a queue that supplies frames.");
            }
            
            final int imageWidth = deviceAddressMap.length +1;
            System.out.format("ImageRenderTransmitter imageWidth: %d \n", imageWidth);
            
            if (imageBuffer == null) {
                System.out.println("ImageRenderTransmitter createImageBuffer()");
                createImageBuffer(imageWidth, 1001);
            }

            byte[] message = new byte[0];
            while(true) {

                    Frame frame = this.pollFrameQueue();
                    if(frame != null) {

                        if(verbose && debug) {
                            // Roughly clock frame timing.
                            long time = frame.getTimePoint().realTimeMillis();
                            long latency = time - previousFrameRealTimeMillis;
                            log("ImageRender frame latency: " + latency + " ms");
                            previousFrameRealTimeMillis = time;
                        }

                        // Pixels per Device, listed in device order, i.e. in
                        // the order of PatchSheet.modelSpaceDevices.
                        Pixel[] pixels = frame.getPixels();
                        
                        if (imageBufferCurrentRow < imageBufferMaxRowCount) {
                            // Add a row of pixel data to the output image
                            //System.out.println("ShowRunner imageBufferCurrentRow < imageBufferMaxRowCount");
                            
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


                        // this.writeOPCPixels(message, pixels);
                        // this.sendBytes(message);
                    } 

            }
        } catch(InterruptedException e) {
            log("Stopping ImageRender transmitter " + this);
        }
    }

    // We break this out into a separate method so that a profiler can easily
    // distinguish between a real hotspot and a quick nap.
    protected void delayReconnect() throws InterruptedException {
        int timeout = 10000;
        log("Waiting " + timeout + " milliseconds before attempting reconnection to OPC server...");
        Thread.currentThread().sleep(timeout);
    }

    public String toString() {
        return "ImageRenderTransmitter";
    }
}

