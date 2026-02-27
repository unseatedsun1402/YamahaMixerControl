package MidiControl.unit.MidiDeviceManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import MidiControl.MidiDeviceManager.Configs.ConfigDirectoryResolver;

public class ConfigDirectoryTest {
    @Test
    public void getConfigDirTest(){
        Path actual = ConfigDirectoryResolver.resolveBaseDirectory();
        System.out.println("Path is: "+actual.toString());
        assertDoesNotThrow(()->actual.toAbsolutePath());
    }

    @Test
    public void getConfigDirTestFails(){
        System.setProperty("midicontrol.config.dir", "");
        Path actual = ConfigDirectoryResolver.resolveBaseDirectory();
        System.out.println("Path is: "+actual.toString());
        assertDoesNotThrow(()->actual.toAbsolutePath());
    }
}
