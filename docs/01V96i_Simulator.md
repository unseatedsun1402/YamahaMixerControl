# 01V96i simulator

Select **01V96i SIL Simulator** as both the MIDI input and output and choose
the 01V96i console profile in settings. The simulator answers fader probes and
meter requests for supported individual meter points, producing synthetic
14-bit values. All-point responses are not implemented.

The simulator assumes a meter request starts or refreshes a roughly 30 Hz
stream lasting 10 seconds. This timing and the all-channel/point ordering
have not been verified against 01V96i hardware.
