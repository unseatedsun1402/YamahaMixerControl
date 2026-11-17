# NRPN hardware learning

`MidiControl.TestUtilities.NrpnLearnMain` is an interactive test-source utility.
Run its main method from the IDE with the working directory set to the project
root, so it can read `m7cl_nrpn.csv`. Choose a JSON report to create or continue,
select a MIDI input, and optionally enable a MIDI output. Continuing a report
requires the same desk model and reference source. Untested entries from older
reports are removed; existing evidence is retained and new evidence is added.
Production mapping files are not valid reports and cannot be opened this way.

The catalog is a transcription of the M7CL owner's manual NRPN table, pages
274-276. It is not an 01V96i table; check the transcription against the manual
and treat its addresses as candidates on other desks. Filter by table label and
select the numbered row; repeated labels are distinguished by address range.
No canonical name is entered or inferred.

**Do not probe a live system.** A probe can change a fader, mute, phantom power,
or another unexpected control. It does not restore the previous setting. In
verify mode choose the table instance, MIDI channel, CC6 value and optional
CC38 value; the tool displays the exact address and bytes and sends only if
you type `SEND`. Entering the bytes separately permits CC6-only, split 14-bit,
or repeated-byte probes without assuming which encoding the desk uses.
The table does not supply safe probe values or establish the desk's encoding.
Answer whether the intended control responded; a negative answer offers learning.
A negative result does not prove that the address is wrong: the data encoding,
value, MIDI channel or desk configuration could also be responsible.
One successful probe confirms only the tested instance, not the whole range.

In learn mode move the selected control over its range and then move its other
instances. Manipulate only that control while recording, and press Enter to
finish. The tool prints one summary per observed MIDI channel/address, including
raw CC6 and CC38 ranges and combined 14-bit ranges when both were observed
together. Missing values are not guessed. The report retains individual
observations, plus lowest and highest observed addresses per MIDI channel; the
highest is the assumed last address, not proof that intermediate addresses
exist. Unrelated NRPN traffic during capture is also recorded.

The report contains only controls with individual user-confirmed or
not-confirmed probes, or learned observations. A control's `reference_candidate`
status describes the table address, not the outcome of a probe; see each probe's
status and each observation separately. It is evidence for later review, not a
generated production mapping; no SysEx matching or value mode is inferred.

## Generate 01V96i mappings

From the project root, run:

```
python src\main\python\generate_01v96i_nrpn_block.py --report nrpn_learn_report.json
```

The generator writes a separate `01v96i_nrpn_learned_mappings.json` and refuses
to overwrite an existing file. Use `--output PATH` to choose another new file.
It matches the report's table ranges to the M7CL block catalog, translates known
01V96i naming differences, and checks the resulting names against the 01V96i
SysEx definitions. Controls without a unique match are reported and skipped.

For each matched control, review the addresses and choose the NRPN value mode or
skip it. If observations fit within the input-channel range starting at the M7CL
table start, the generator uses that start; otherwise it assumes the lowest
observed address is the first instance. It assumes contiguous addresses through
the highest observation. These are assumptions, not proof of channel identity or
value encoding. Observations outside the selected table range produce a warning.
Confirmed probes contribute only their tested instance; failed probes do not
generate mappings. Conflicting probes, multiple MIDI channels, duplicate
addresses, and ranges larger than the SysEx control's channel count are rejected
or skipped. Review the output before using it as a production mapping.

The 01V96i resource includes six captured EQ and attenuator blocks as
`NRPN_14BIT`, consistent with the client's 14-bit input scaling. A low-gain
capture while channels 31 and 32 were paired observed `149C` and `149D`.
The low-gain block starts at `147E`, assuming an address stride of one for
channels 1-32. The other addresses and outgoing value encoding still need
hardware verification. The former low-gain start `19E8` came from a capture
whose control identity was uncertain; the M7CL label for that address does not
establish its function on the 01V96i.
