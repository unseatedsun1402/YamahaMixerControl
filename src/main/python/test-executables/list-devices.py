import mido

def list_midi_devices():
    print("Available MIDI Input Devices:")
    for name in mido.get_input_names():
        print(f"  - {name}")

    print("\nAvailable MIDI Output Devices:")
    for name in mido.get_output_names():
        print(f"  - {name}")

if __name__ == "__main__":
    list_midi_devices()

