# Meter requests and updates

The M7CL broadcasts meter data at approximately 30 Hz for ten seconds after a
meter request. A new request refreshes that interval and selects the requested
meters. The M7CL simulator models this behavior; the 01V96i simulator currently
uses the same timing assumption, which still needs verification on real hardware.
The datasheet request/answer examples describe the message format, not a
one-response-only polling protocol.

The mixer and meter pages send a keep-alive when connected and every 10 seconds.
The server does not run a separate meter-request timer: it rate-limits incoming
keep-alives across clients and sends the current selection. Its 9-second
minimum avoids suppressing a 10-second client refresh because of timer jitter.
The parser's 333 ms scheduled task only batches received updates for the GUI;
it does not send MIDI requests. Browser throttling or network delays can still
interrupt a stream before the next keep-alive.

M7CL requests use model byte `11`; the documented meter responses use `0F`.
The 01V96i uses `1A` for both. The M7CL all-channel selector is start `7F`
with count `00 00`; the response starts at channel `00`. Its input all-point
response contains nine values per channel. The simulator tests cover the
datasheet's example message lengths and selectors, not physical hardware.
Meter requests use the MIDI device number from the current registry's SysEx
mappings, including the channel found during desk discovery. Replacing or
reloading the registry clears the old selection and queued meter updates and
detaches the previous registry's rehydration listeners.

The GUI's level parser does not currently decode M7CL sigma-clip bitmaps or
01V96i all-point responses; explicit GUI requests for these formats are
rejected rather than displaying them as channel levels. The 01V96i simulator
does not generate all-point responses. The 01V96i table calls the two request
count bytes "desired size"; their interpretation and all-point ordering should
be checked against an actual desk before treating the simulator as authoritative.
The level conversion and behavior during delayed in-flight MIDI messages have
not been verified against physical desks.

## Profile switching and MIDI failures

Selecting a desk profile reloads its mappings even if a MIDI port cannot be
opened. The settings response reports the input, output, and mapping results
separately; the MIDI error and port-open exception are recorded in the server
log. Liveness reports the selected desk as disconnected until both ports are
available, without repeatedly resetting an incomplete device pair. Select
working ports again after resolving a port-open failure; a successful profile
switch alone does not mean MIDI communication is working.
