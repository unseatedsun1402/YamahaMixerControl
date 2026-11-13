# M7CL simulator

Select **M7CL SIL Simulator** as both the MIDI input and output and choose
the M7CL console profile in settings. The simulator answers fader probes and
meter requests for supported individual meter points, producing synthetic
7-bit values. All-point responses are not implemented.

The simulator assumes a meter request starts or refreshes a roughly 30 Hz
stream lasting 10 seconds. This timing and the all-channel/point ordering
have not been verified against M7CL hardware.
