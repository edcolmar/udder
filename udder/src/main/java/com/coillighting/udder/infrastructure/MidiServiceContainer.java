package com.coillighting.udder.infrastructure;

import java.io.*;
import java.net.URLDecoder;
import java.util.Map;
import java.util.Queue;
import java.util.List;

import javax.sound.midi.MidiDevice; 
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.MidiSystem;
//import javax.sound.midi.*;  

import org.boon.json.JsonFactory;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.Path;
import org.simpleframework.http.Query;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;

import static org.boon.Exceptions.SoftenedException;

import com.coillighting.udder.util.CollectionUtil;
import com.coillighting.udder.util.StringUtil;

import static com.coillighting.udder.util.LogUtil.log;

import com.coillighting.udder.infrastructure.MidiInputReceiver;

/** The HTTP controller for Udder's Simple-brand webserver.
 *  Receives requests, translates them into commands, and responds as needed.
 */
public class MidiServiceContainer {

    protected boolean verbose = true; // log errors
    protected boolean debug = false; // log successful POSTs and our response
    protected Queue<Command> queue; // feed requests to this queue
    protected Map<String, Class> commandMap; // translate JSON to command object
    protected int requestIndex = 0; // Count requests to assist debugging (for now)

    public MidiServiceContainer(Queue<Command> queue, Map<String, Class> commandMap) {
        if(queue == null) {
            throw new NullPointerException(
                    "MidiServiceContainer requires a Queue for consuming commands.");
        } else if(commandMap == null) {
            throw new NullPointerException(
                    "MidiServiceContainer requires a commandMap for dispatching commands.");
        }
        
        // System.out.println("MIDIServiceContainer INIT");
        
        this.queue = queue;
        this.commandMap = commandMap;

        MidiDevice inputDevice;
        
        try {
            inputDevice = MidiServiceContainer.getInputDevice();
            
            //get all transmitters
            List<javax.sound.midi.Transmitter> transmitters = inputDevice.getTransmitters();
            //and for each transmitter

            for(int j = 0; j<transmitters.size();j++) {
                //create a new receiver
                transmitters.get(j).setReceiver(
                        //using my own MidiInputReceiver
                        new MidiInputReceiver(this.queue, this.commandMap, inputDevice.getDeviceInfo().toString())
                );
            }

            javax.sound.midi.Transmitter trans = inputDevice.getTransmitter();
            trans.setReceiver(new MidiInputReceiver(this.queue, this.commandMap, inputDevice.getDeviceInfo().toString()));

            //open each device
            inputDevice.open();
            //if code gets this far without throwing an exception
            //print a success message
            System.out.println(inputDevice.getDeviceInfo()+" Was Opened");
            
        } catch (Exception exc) {
            // TODO: handle exception
        }
    }
    
   public static MidiDevice getInputDevice() throws MidiUnavailableException {
      MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
      System.out.println("Available MIDI Devices: ");
      for (int i = 0; i < infos.length; i++) {
         MidiDevice device = MidiSystem.getMidiDevice(infos[i]);

         System.out.println("MIDI Device Found:");
         System.out.println("  Name: " + device.getDeviceInfo().getName());
         System.out.println("  Description: " + device.getDeviceInfo().getDescription());
         System.out.println("  Vendor: " + device.getDeviceInfo().getVendor());
         System.out.println("");
         
         if (device.getMaxTransmitters() != 0
               && device.getDeviceInfo().getName().contains("Bus 1")) {
            System.out.println(device.getDeviceInfo().getName().toString()
                  + " was chosen");
            return device;
         }
      }
      return null;
   }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean getVerbose() {
        return verbose;
    }

    public void log(Object message) {
        System.out.println(message);
    }
}


class MidiRoutingException extends Exception {
    public MidiRoutingException(String message) { super(message); }
}


class MidiCommandParserException extends Exception {
    public MidiCommandParserException(String message) { super(message); }
}
