import contextlib
import io
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

import generate_01v96i_nrpn_block as generator


def observation(address, channel=1):
    return {
        "midiChannel": channel, "address": address,
        "minCc6": 0, "maxCc6": 127, "minCc38": 0, "maxCc38": 127,
    }


def control(start, end, addresses, tests=None):
    return {
        "label": "test", "tableStart": start, "tableEnd": end,
        "tests": tests or [],
        "learned": [{"observations": [observation(address) for address in addresses]}]
        if addresses else [],
    }


class ReportGeneratorTest(unittest.TestCase):
    def generate(self, controls, choices):
        iterator = iter(choices)
        with contextlib.redirect_stdout(io.StringIO()):
            return generator.mappings_from_report(
                {"deskModel": "01v96i", "controls": controls},
                choose=lambda _: next(iterator),
            )

    def test_infers_missing_start_from_table_and_uses_sysex_names(self):
        mappings = self.generate([control(0x1400, 0x147D, [0x141E, 0x141F])], ["2"])
        self.assertEqual(len(mappings), 32)
        self.assertEqual(mappings[0], {
            "canonical_id": "kInputEQ.kEQLowF.0",
            "msb": "0x28", "lsb": "0x00", "value_mode": "CC6_ONLY",
        })
        self.assertEqual(mappings[-1]["canonical_id"], "kInputEQ.kEQLowF.31")
        self.assertEqual(mappings[-1]["lsb"], "0x1F")

    def test_shifted_observations_and_failed_probes_are_not_replaced_by_table(self):
        failed = {"status": "not_confirmed", "tableInstance": 1, "address": 0x147E, "midiChannel": 1}
        mappings = self.generate([control(0x147E, 0x14FB, [0x19E8, 0x19EA], [failed])], ["1"])
        self.assertEqual(len(mappings), 3)
        self.assertEqual(mappings[0]["canonical_id"], "kInputEQ.kEQLowG.0")
        self.assertEqual((mappings[0]["msb"], mappings[0]["lsb"]), ("0x33", "0x68"))

    def test_only_confirmed_probe_instance_is_generated(self):
        confirmed = {"status": "confirmed_by_user", "tableInstance": 3, "address": 0x1306, "midiChannel": 1}
        failed = {"status": "not_confirmed", "tableInstance": 1, "address": 0x1304, "midiChannel": 1}
        mappings = self.generate([control(0x1304, 0x1381, [], [confirmed, failed])], ["3"])
        self.assertEqual(len(mappings), 1)
        self.assertEqual(mappings[0]["canonical_id"], "kInputEQ.kEQOn.2")
        self.assertEqual(mappings[0]["value_mode"], "NRPN_RAW_SPLIT")

    def test_conflicting_probe_or_multiple_midi_channels_are_skipped(self):
        confirmed = {"status": "confirmed_by_user", "tableInstance": 1, "address": 0x1305, "midiChannel": 1}
        conflict = control(0x1304, 0x1381, [0x1304, 0x1305], [confirmed])
        multi_channel = control(0x1382, 0x13FF, [0x1382])
        multi_channel["learned"][0]["observations"].append(observation(0x1383, channel=2))
        probe_channel = control(0x1400, 0x147D, [0x1400], [
            {"status": "confirmed_by_user", "tableInstance": 1, "address": 0x1400, "midiChannel": 2}
        ])
        self.assertEqual(self.generate([conflict, multi_channel, probe_channel], []), [])

    def test_duplicate_addresses_fail_and_unknown_controls_are_skipped(self):
        first = control(0x1304, 0x1381, [0x1304])
        second = control(0x1382, 0x13FF, [0x1304])
        with self.assertRaisesRegex(ValueError, "duplicate mapping"):
            self.generate([first, second], ["1", "1"])
        self.assertEqual(self.generate([control(0x3000, 0x3001, [0x3000])], []), [])

    def test_cli_writes_separate_output_and_refuses_overwrite(self):
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory) / "report.json"
            output = Path(directory) / "output.json"
            report.write_text(json.dumps({
                "deskModel": "01v96i",
                "controls": [control(0x1304, 0x1381, [0x1304, 0x1305])],
            }), encoding="utf-8")
            command = [sys.executable, str(Path(generator.__file__)), "--report", str(report),
                       "--output", str(output)]
            result = subprocess.run(command, input="2\n", capture_output=True, text=True, check=True)
            self.assertIn("Written 2 learned mappings", result.stdout)
            saved = output.read_text(encoding="utf-8")
            self.assertEqual(len(json.loads(saved)), 2)
            retry = subprocess.run(command, input="", capture_output=True, text=True)
            self.assertNotEqual(retry.returncode, 0)
            self.assertIn("output already exists", retry.stderr)
            self.assertEqual(saved, output.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
