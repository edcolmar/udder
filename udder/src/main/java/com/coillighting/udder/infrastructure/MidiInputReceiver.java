package com.coillighting.udder.infrastructure;

import java.io.*;
import java.net.URLDecoder;
import java.util.Queue;
import java.util.Map;

import javax.sound.midi.*;  

import static com.coillighting.udder.util.LogUtil.log;

import com.coillighting.udder.util.StringUtil;
import com.coillighting.udder.effect.MidiImageRollState;

public class MidiInputReceiver implements Receiver {
	
	protected int requestIndex = 0; // Count requests to assist debugging (for now)
	
    public String name;
	
	protected Queue<Command> queue; // feed requests to this queue
    protected boolean verbose = true; // log errors
    protected boolean debug = false; // log successful POSTs and our response
	
	protected Map<String, Class> commandMap; // translate JSON to command object
	
	public static long seByteCount = 0;
	public static long smByteCount = 0;
	public static long seCount = 0;
	public static long smCount = 0;
	
    public MidiInputReceiver(Queue<Command> queue, Map<String, Class> commandMap, String name) {
        if(queue == null) {
            throw new NullPointerException(
                    "MidiInputReceiver requires a Queue for consuming commands.");
        }
        this.name = name;
		this.queue = queue;
		this.commandMap = commandMap;
    }
	
	
    public void send(MidiMessage msg, long timeStamp) {
		
		// String hardcodedCommand = "{\"r\":0.0,\"g\":0.0,\"b\":0.0}";

        //System.out.println("midi received");
		//System.out.println("  msg: " + msg);
		//System.out.println("  msg.getMessage(): " + msg.getMessage());

		ShortMessage message = (ShortMessage) msg;
		
		try {
			MidiImageRollState mMessage = new MidiImageRollState(message);
			
			//System.out.println("  message.getData1(): " + mMessage.getData1());
			//System.out.println("  message.getData2(): " + mMessage.getData2());
			//System.out.println("  message.getChannel(): " + mMessage.getChannel());
			//System.out.println("  message.getCommand(): " + mMessage.getCommand());
		
		
			String hardcodedRoute = "/mixer0/layer" + mMessage.getChannel() + "/effect";
			//String hardcodedRoute = "/mixer0/layer0/effect";
		
			Command command = new Command(hardcodedRoute, mMessage);
		
			this.queue.offer(command);
			
		} catch (InvalidMidiDataException e) {
			
		}
		

		
		
    }
    public void close() {}
    

}