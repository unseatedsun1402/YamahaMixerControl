# Hardware Client Framework

## Phase 1 Implementation Complete

The hardware client framework Phase 1 has been successfully implemented to support external control surfaces like Mackie Control Universal (MCU) without modifying the existing Yamaha protocol layer.

## What's Implemented

### Core Components

1. **IControlClient Interface** (`IControlClient.java`)
   - Base interface for all hardware clients
   - Client types: MCU, HUI, OSC, MIDI_LEARN, CUSTOM
   - Bank types: CHANNELS, BUSSES, MATRIX, AUX, OUTPUTS, MASTER

2. **HardwareClientManager** (`HardwareClientManager.java`)
   - Central dispatcher managing all hardware clients
   - Thread-local origin tracking for echo prevention
   - Registry reload listener support
   - ControlListener pattern implementation

3. **MidiPortPair** (`MidiPortPair.java`)
   - Independent MIDI device management
   - Separate from Yamaha desk MidiIOManager
   - Clean device open/close lifecycle

4. **MackieMCUClient** (`MackieMCUClient.java`)
   - Proper MCU protocol implementation (not HUI)
   - Pitch bend for faders (channels 0-7)
   - Note On/Off for mutes/solos
   - CC for bank switching (0x20-0x27)
   - Bank mapping to canonical controls

5. **Supporting Services**
   - `HardwareClientService.java` - MidiServer integration wrapper
   - `HardwareClientConfig.java` - Configuration management
   - `HardwareClientManagerTest.java` - Unit tests

## Key Architectural Decisions

### 1. Per-Instance ControlListener Pattern
Instead of a central event bus, each client attaches directly to `ControlInstance` objects. This matches the existing pattern used by `RehydrationManager` and provides:
- Direct control instance access
- No central bottleneck
- Natural integration with existing architecture

### 2. Thread-Local Origin Suppression
Prevents echo loops by tracking which client originated a change:
```java
// In HardwareClientManager.handleHardwareInput()
origin.set(clientId);  // Set thread-local marker
ci.updateValue(newValue);  // This triggers onControlChanged()
origin.set(null);  // Clear marker

// In HardwareClientManager.onControlChanged()
if (clientId.equals(origin.get())) {
    continue;  // Skip the originating client
}
```

### 3. Independent MIDI Ports
Hardware clients use separate `MidiPortPair` instances, not the Yamaha desk `MidiIOManager`. This prevents interference with the primary desk communication.

### 4. Registry Reload Survivability
Clients survive canonical registry rebuilds by:
- Implementing `onRegistryReload()` method
- Reattaching listeners to new control instances
- Clearing and rebuilding cache mappings

## MCU Protocol Details

### Correct MCU Implementation (Not HUI)
- **Faders**: Pitch Bend (0xE0) on channels 0-7 (14-bit: 0-16383)
- **Mute**: Note On/Off with velocity (127 = LED on, 0 = LED off)
- **Solo**: Note On/Off with velocity
- **Select**: Note On/Off with velocity
- **Bank Switching**: CC 0x20-0x27

### Bank Mapping
MCU's 8 faders map to canonical controls based on:
- Bank type (CHANNELS, BUSSES, etc.)
- Bank index (0-based)
- Strip position (0-7)

Example: Channel bank 1, strip 0 → `channel.9.level` (1-based indexing)

## Usage Examples

### Basic Integration
```java
// Create manager
HardwareClientManager manager = new HardwareClientManager(registry);

// Create MCU client
MackieMCUClient mcu = new MackieMCUClient(
    "mcu-1", "Mackie Control Universal",
    "MCU Input", "MCU Output", manager
);

// Register and start
manager.registerClient(mcu);
manager.startAll();
```

### Handling Hardware Input
```java
// Hardware → Software
manager.handleHardwareInput("mcu-1", "channel.1.level", 64);

// Software → Hardware (via ControlInstance update)
ControlInstance ci = registry.resolveCanonicalId("channel.1.level");
ci.updateValue(115);  // Automatically sent to all non-originating clients
```

### Bank Management
```java
// Change bank
manager.handleHardwareBankChange("mcu-1", 
    IControlClient.BankType.CHANNELS, 2, 0);
```

## Configuration

Default configuration in `HardwareClientConfig`:
```java
ClientConfig mcuConfig = new ClientConfig();
mcuConfig.setId("mcu-1");
mcuConfig.setDisplayName("Mackie Control Universal");
mcuConfig.setType(IControlClient.ClientType.MCU);
mcuConfig.setEnabled(false);  // Disabled by default
mcuConfig.setInputDeviceName("MCU Input");
mcuConfig.setOutputDeviceName("MCU Output");
mcuConfig.setBankType(IControlClient.BankType.CHANNELS);
mcuConfig.setStartBankIndex(0);
```

## Integration with MidiServer

The `HardwareClientService` provides clean integration:
```java
public class MidiServerIntegration {
    private HardwareClientService hardwareService;
    
    public void initialize(CanonicalRegistry registry) {
        hardwareService = new HardwareClientService(registry);
        hardwareService.initialize();
    }
    
    public void onRegistryReload(CanonicalRegistry newRegistry) {
        hardwareService.onRegistryReload(newRegistry);
    }
    
    public void shutdown() {
        hardwareService.shutdown();
    }
}
```

## Testing

Unit tests verify:
- Client registration/unregistration
- Lifecycle management (start/stop)
- Registry reload handling
- Thread-local origin tracking

## Next Steps (Phase 2)

1. **OSC Client** - Open Sound Control protocol support
2. **Configuration Files** - JSON/YAML config loading
3. **Dynamic Management** - Runtime client add/remove
4. **Web Interface** - Client configuration UI
5. **MIDI Learn** - Generic MIDI mapping
6. **Presets** - Client configuration presets

## Files Created

```
src/main/java/MidiControl/HardwareClients/
├── IControlClient.java              # Base interface + enums
├── HardwareClientManager.java       # Central dispatcher (201 lines)
├── MidiPortPair.java                # MIDI device helper (155 lines)
├── MackieMCUClient.java             # MCU protocol (390 lines)
├── HardwareClientService.java       # Service wrapper (68 lines)
└── HardwareClientConfig.java        # Configuration (105 lines)

src/test/java/MidiControl/HardwareClients/
└── HardwareClientManagerTest.java   # Unit tests (86 lines)

examples/
└── HardwareClientIntegrationExample.java  # Usage example

docs/
├── HardwareClientFramework.md       # Implementation overview
└── HardwareClients/README.md        # This file
```

## Protocol References

- **MCU Protocol**: Uses pitch bend for faders (not HUI CC)
- **14-bit Resolution**: 0-16383 range for faders
- **Bank Switching**: CC 0x20-0x27 with signed relative values
- **LED Feedback**: Note velocity controls LED state

The framework is now ready for Phase 2 implementation and integration testing with actual MCU hardware.