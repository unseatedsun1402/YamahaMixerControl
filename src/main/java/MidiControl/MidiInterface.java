package MidiControl;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


/**
 *
 * @author ethanblood
 */
public interface MidiInterface {
    
    /**
     * Function for discovering midi devices available to the system and returns a MidiDevice.Info[]
     * 
     * @return MidiDevice.Info[]
     * @throws MidiUnavailableException
     */
    public static MidiDevice.Info[] discoverDevices() throws MidiUnavailableException{
        MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();
		if (devices.length != 0) {
            return devices;
        }
        System.err.println("No MIDI devices found");
        return null;
    }

    /**
     * discovers attached devices and presents selection via user input
     * @return MidiDevice
     * @throws MidiUnavailableException
     */
    public static MidiDevice selectMidiDevice() throws MidiUnavailableException{
        MidiDevice.Info[] devices = discoverDevices();
        int enumeration = 0;
        List<Integer> validenum = new ArrayList<>();
        MidiDevice device;
        for(MidiDevice.Info info : devices)
        {
            device = MidiSystem.getMidiDevice(info);
            if(!(device instanceof Synthesizer))
            {
                if(!(device instanceof Sequencer))
                {
                    
                    if(!(validenum == null))
                    {
                    validenum.add(enumeration);
                    }
                }
                
            }
            enumeration ++;
        }

        Scanner selection = new Scanner(System.in);
        int select = Integer.parseInt(selection.next());
        selection.close();

        if(!(validenum.contains(select))){
            System.err.println("Error: Input out of range");
            return null;
        }
        return MidiSystem.getMidiDevice(devices[select]);
    }


    /**
     * Public function for fetching the in use Midi connection
     * @return
     * @throws MidiUnavailableException
     */
    public static MidiDevice getMidiDevice(int dev) throws MidiUnavailableException{
        MidiDevice.Info[] devices = discoverDevices();
        MidiDevice device = MidiSystem.getMidiDevice(devices[dev]);
        return device;
    }

    
    /**
     * Gets a transmitter using the provided device info from attached system devices
     * @return MidiDevice.Transmitter
     * @throws MidiUnavailableException 
     */
    public static javax.sound.midi.Transmitter getTransmitter(int dev) throws MidiUnavailableException{
        MidiDevice device = MidiSystem.getMidiDevice(discoverDevices()[dev]);
        Transmitter tx = device.getTransmitter();
        
        if(!(tx == null)){
            return tx;
        }
        return null;
    }
    
    /**
     * Gets MidiDevice Receiver (midi In port)
     * @return MidiDevice.Receiver
     * @throws MidiUnavailableException 
     */
    public static javax.sound.midi.Receiver getReceiver(int dev) throws MidiUnavailableException{
        MidiDevice device = MidiSystem.getMidiDevice(discoverDevices()[dev]);
        Receiver rcvr = device.getReceiver();
        
        if(!(rcvr == null)){
            return rcvr;
        }
        return null;
    }
            
}