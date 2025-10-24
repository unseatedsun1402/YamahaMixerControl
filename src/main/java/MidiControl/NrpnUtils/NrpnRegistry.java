package MidiControl.NrpnUtils;
import MidiControl.ChannelMappings.ChannelType;
import MidiControl.ChannelMappings.ControlType;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class NrpnRegistry {
    public static final NrpnRegistry INSTANCE = new NrpnRegistry();
    private final List<NrpnMapping> mappings = new ArrayList<>();

    public NrpnRegistry() {

        for (int i = 0; i < 16; i++) {
            int nrpn = 0x060 + i; // MIX FADER range
            int msb = nrpn / 128;
            int lsb = nrpn % 128;
            mappings.add(new NrpnMapping(msb, lsb, ControlType.OUTPUT_FADER, ChannelType.MIX, i, "Mix " + (i + 1) + " Fader"));
        }
        for (int i = 0; i < 8; i++) {
            int nrpn = 0x070 + i; // MATRIX FADER range
            int msb = nrpn / 128;
            int lsb = nrpn % 128;
            mappings.add(new NrpnMapping(msb, lsb, ControlType.OUTPUT_FADER, ChannelType.MATRIX, i, "Matrix " + (i + 1) + " Fader"));
        }
        for (int i = 0; i < 5; i++) {
            int nrpn = 0x079 + i; // ST FADER range
            int msb = nrpn / 128;
            int lsb = nrpn % 128;
            mappings.add(new NrpnMapping(msb, lsb, ControlType.OUTPUT_FADER, ChannelType.MASTER, i, "ST Bus " + (i + 1) + " Fader"));
        }
    }

    public static NrpnRegistry getInstance() {
        return INSTANCE;
    }

    public static Optional<NrpnMapping> resolve(int msb, int lsb) {
        Optional<NrpnMapping> result = INSTANCE.mappings.stream()
            .filter(m -> m.msb() == msb && m.lsb() == lsb)
            .findFirst();

        if (result.isEmpty()) {
            System.out.printf("Registry miss: MSB=0x%02X, LSB=0x%02X%n", msb, lsb);
        }

        return result;
    }
    public static void dump() {
        INSTANCE.mappings.forEach(m ->
            System.out.printf("MSB=0x%02X, LSB=0x%02X → %s%n", m.msb(), m.lsb(), m.label()));
    }

    public void buildProfileMapping() {
        // generateMappings for input faders
        generateMappings(56,ControlType.FADER , ChannelType.INPUT, "Input to stereo ", 0);
        // generateMappings for mix sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Mix 1 send ", 0x28EA); //mix 1 sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Mix 2 send ", 0x292A); //mix 2 sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Mix 3 send ", 0x296A); //mix 3 sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Mix 4 send ", 0x29AA); //mix 4 sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Mix 5 send ", 0x29EA); //mix 5 sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Mix 6 send ", 0x2A2A); //mix 6 sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Mix 7 send ", 0x2A6A); //mix 7 sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Mix 8 send ", 0x2AAA); //mix 8 sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Mix 9 send ", 0x07e);  //mix 9 sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Mix 10 send ", 0x0de); //mix 10 sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Mix 11 send ", 0x13e); //mix 11 sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Mix 12 send ", 0x19e); //mix 12 sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Mix 13 send ", 0x1fe); //mix 13 sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Mix 14 send ", 0x25e); //mix 14 sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Mix 15 send ", 0x2be); //mix 15 sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Mix 16 send ", 0x31e); //mix 16 sends

        // generateMappings for matrix sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Matrix 1 send ", 0x037E); //matrix 1 sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Matrix 2 send ", 0x03DE); //matrix 2 sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Matrix 3 send ", 0x043E); //matrix 3 sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Matrix 4 send ", 0x04DE); //matrix 4 sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Matrix 5 send ", 0x2AEA); //matrix 5 sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Matrix 6 send ", 0x2B2A); //matrix 6 sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Matrix 7 send ", 0x2B6A); //matrix 7 sends
        generateMappings(56, ControlType.INPUT_SEND, ChannelType.INPUT, "Matrix 8 send ", 0x2BAA); //matrix 8 sends
    }

    private void generateMappings(int numberofmappings, ControlType controlType, ChannelType channelType, String labelPrefix, int startNrpn) {
    for (int i = 0; i < numberofmappings; i++) {
            int nrpn = startNrpn + i; // INPUT FADER range
            int msb = nrpn / 128;
            int lsb = nrpn % 128;
            this.mappings.add(new NrpnMapping(msb, lsb, controlType, channelType, i, labelPrefix + (i + 1) + " control"));
            Logger.getLogger(NrpnRegistry.class.getName()).log(Level.FINE, "Registered NRPN Mapping: MSB {0}, LSB {1} for {2}", new Object[]{msb, lsb, labelPrefix + (i + 1) + " Control"});
        }
    }
    
    public List<NrpnMapping> getMappings() {
        return mappings;
    }

    public void loadFromJson(String filePath) {
        try (Reader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            NrpnMapping[] loaded = gson.fromJson(reader, NrpnMapping[].class);

            if (loaded == null) {
                System.err.println("NRPN mapping file is empty or malformed: " + filePath);
                return;
            }

            mappings.clear();
            mappings.addAll(Arrays.asList(loaded));
            System.out.println("Loaded NRPN mappings from " + filePath + " : " + loaded.length + " NRPN mappings.");
        } catch (IOException e) {
            System.err.println("Failed to load NRPN mappings: " + e.getMessage());
        }
    }

    public void loadFromClasspath(String resourcePath) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("MidiControl/nrpn_mappings.json")) {
            if (input == null) {
                System.out.println("Mapping file not found in resources.");
                return;
            }

            Gson gson = new Gson();
            NrpnMapping[] loaded = gson.fromJson(new InputStreamReader(input), NrpnMapping[].class);
            if (loaded != null) {
                mappings.clear();
                mappings.addAll(Arrays.asList(loaded));
                System.out.println("Loaded " + loaded.length + " NRPN mappings from resources.");
            } else {
                System.out.println("Mapping file was empty or malformed.");
            }
        } catch (IOException e) {
            System.err.println("Failed to load NRPN mappings: " + e.getMessage());
        }
    }


    public void exportToJson(String filePath) {
    try (FileWriter writer = new FileWriter(filePath)) {
        new GsonBuilder().setPrettyPrinting().create().toJson(mappings, writer);
        System.out.println("Exported NRPN mappings to " + filePath);
    } catch (IOException e) {
        System.err.println("NRPN export failed: " + e.getMessage());
    }
}

    public NrpnMapping lookup(int i, int j) {
        try {
            return mappings.stream()
                .filter(m -> m.msb() == i && m.lsb() == j)
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            Logger.getLogger(NrpnRegistry.class.getName()).log(Level.SEVERE, "Error looking up NRPN mapping for MSB {0}, LSB {1}: {2}", new Object[]{i, j, e.getMessage()});
            return null;
        }
    }
}
