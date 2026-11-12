# Hardware Client Framework - Phase 1 Implementation

## Overview

Phase 1 of the hardware client framework has been implemented to support Mackie Control Universal (MCU) surfaces and other hardware controllers without modifying the existing Yamaha protocol layer. The framework provides a clean abstraction for hardware clients to interact with the canonical control model.

## Architecture

### Key Components

1. **IControlClient Interface** - Base interface for all hardware clients
2. **HardwareClientManager** - Central dispatcher managing all clients
3. **MidiPortPair** - Helper for independent MIDI device pairs
4. **MackieMCUClient** - MCU protocol implementation with proper pitch bend faders
5. **HardwareClientService** - Service wrapper for MidiServer integration
6. **HardwareClientConfig** - Configuration management

### Design Patterns

- **Per-instance ControlListener pattern** - Each client attaches to control instances directly
- **Thread-local origin suppression** - Prevents echo loops by tracking change origins
- **Registry reload listener** - Survives canonical registry rebuilds
- **Independent MIDI ports** - Separate from Yamaha desk MidiIOManager

## MCU Protocol Details

The Mackie MCU implementation uses the correct MCU protocol (not HUI):

- **Faders**: Pitch Bend messages (0xE0) on channels 0-7 (14-bit resolution)
- **Mute/Solo/Select**: Note On/Off messages with specific note mappings
- **Bank Switching**: CC messages 0x20-0x27 for bank selection
- **LED Feedback**: Note On with velocity 127 = LED on, 0 = LED off

## Integration Points

### Control Flow

1. **Forward Path (Hardware → Software)**:
   ```
   MCU Fader → MIDI Input → MackieMCUClient → HardwareClientManager → ControlInstance.updateValue()
   ```

2. **Reverse Path (Software → Hardware)**:
   ```
   ControlInstance.onControlChanged() → HardwareClientManager → MackieMCUClient → MIDI Output → MCU Fader
   ```

### Echo Prevention

The framework uses thread-local storage to track which client originated a control change. When `ControlInstance.updateValue()` is called:

1. The originating client sets a thread-local marker
2. `HardwareClientManager.onControlChanged()` skips the originating client
3. All other clients receive the update
4. Thread-local marker is cleared

## Usage Example

```java
// Create hardware client manager
HardwareClientManager clientManager = new HardwareClientManager(canonicalRegistry);

// Create MCU client
MackieMCUClient mcuClient = new MackieMCUClient(
    "mcu-1",
    "Mackie Control Universal",
    "MCU Input Device",
    "MCU Output Device",
    clientManager
);

// Register and start
clientManager.registerClient(mcuClient);
clientManager.startAll();

// Handle bank changes
clientManager.handleHardwareBankChange("mcu-1", 
    IControlClient.BankType.CHANNELS, 1, 0);
```

## Configuration

Example configuration (would be loaded from JSON/YAML in production):

```java
HardwareClientConfig config = new HardwareClientConfig();
List<ClientConfig> enabledClients = config.getEnabledClients();

for (ClientConfig clientConfig : enabledClients) {
    if (clientConfig.getType() == IControlClient.ClientType.MCU) {
        MackieMCUClient client = new MackieMCUClient(
            clientConfig.getId(),
            clientConfig.getDisplayName(),
            clientConfig.getInputDeviceName(),
            clientConfig.getOutputDeviceName(),
            clientManager
        );
        clientManager.registerClient(client);
    }
}
```

## Integration with MidiServer

The `HardwareClientService` provides a clean interface for MidiServer integration:

```java
// In MidiServer constructor or initialization
HardwareClientService hardwareService = new HardwareClientService(canonicalRegistry);
hardwareService.initialize();

// Handle registry reloads
hardwareService.onRegistryReload(newRegistry);

// Shutdown
hardwareService.shutdown();
```

## Testing

Unit tests have been created for:
- `HardwareClientManager` - Client registration and lifecycle
- Additional tests would cover:
  - MCU protocol encoding/decoding
  - Echo prevention logic
  - Bank mapping functionality
  - MIDI port management

## Next Steps (Phase 2)

1. **OSC Client Implementation** - Open Sound Control protocol support
2. **Configuration File Loading** - JSON/YAML configuration support
3. **Dynamic Client Management** - Runtime client addition/removal
4. **Web Interface** - Hardware client configuration UI
5. **MIDI Learn** - Generic MIDI mapping support
6. **Preset Management** - Client configuration presets

## Files Created

```
src/main/java/MidiControl/HardwareClients/
├── IControlClient.java              # Base interface
├── HardwareClientManager.java       # Central dispatcher
├── MidiPortPair.java                # MIDI device helper
├── MackieMCUClient.java             # MCU protocol implementation
├── HardwareClientService.java       # MidiServer integration
└── HardwareClientConfig.java        # Configuration management

src/test/java/MidiControl/HardwareClients/
└── HardwareClientManagerTest.java   # Unit tests

docs/
└── HardwareClientFramework.md       # This documentation
```

## Protocol Notes

- **MCU vs HUI**: MCU uses pitch bend for faders, HUI uses different CC messages
- **Bank Mapping**: Strips 0-7 map to canonical controls based on bank type/index
- **14-bit Resolution**: Faders use 14-bit pitch bend (0-16383), normalized to canonical range
- **Button LED Feedback**: Uses Note On/Off with velocity for LED state