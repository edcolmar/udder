package com.coillighting.udder.effect;

import javax.sound.midi.*; 

public class MidiImageRollState extends ShortMessage {
	
	protected String filename = null;
	protected Integer command = 0;
	protected Integer data1 = 0;
	protected Integer data2 = 0;
	protected Integer channel = 0;
	
    public MidiImageRollState()
    {
		
    }

    public MidiImageRollState(ShortMessage shortMessage) throws InvalidMidiDataException
    {
		this.command = shortMessage.getCommand();
		this.data1 = shortMessage.getData1();
		this.data2 = shortMessage.getData2();
		this.channel = shortMessage.getChannel();
		//System.out.println("MidiImageRollState INIT ");

    }

	
    public String getFilename() {
        return filename;
    }
	
    public int getData1() {
        return data1;
    }
	
    public int getData2() {
        return data2;
    }
	
    public int getChannel() {
        return channel;
    }
	
    public int getCommand() {
        return command;
    }
	

}