import json
import re
import sys


EXECUTABLE = re.compile(
    r"""(?i)(?:^|[\s"';&|/\\])(?:node|npm|npx)(?:\.exe|\.cmd|\.ps1)?(?=$|[\s"';&|])"""
)
SHELL_TOOL = re.compile(r"(?i)(terminal|shell|powershell|command|exec|run)")
COMMAND_KEYS = {"command", "cmd", "commandline", "shellcommand", "script"}


def command_values(value):
    if isinstance(value, dict):
        for key, nested in value.items():
            if key.lower() in COMMAND_KEYS and isinstance(nested, str):
                yield nested
            elif isinstance(nested, (dict, list)):
                yield from command_values(nested)
    elif isinstance(value, list):
        for nested in value:
            yield from command_values(nested)


def main():
    try:
        event = json.load(sys.stdin)
    except (json.JSONDecodeError, UnicodeDecodeError):
        print("{}", flush=True)
        return

    tool_name = event.get("toolName", "")
    if not isinstance(tool_name, str) or not SHELL_TOOL.search(tool_name):
        print("{}", flush=True)
        return

    tool_args = event.get("toolArgs", {})
    if isinstance(tool_args, str):
        commands = [tool_args]
    else:
        commands = list(command_values(tool_args))

    if any(EXECUTABLE.search(command) for command in commands):
        print(
            json.dumps(
                {
                    "permissionDecision": "deny",
                    "permissionDecisionReason": (
                        "Node.js, npm, and npx commands are blocked by this "
                        "workspace policy. Use Gradle/Java for backend validation "
                        "and Python or browser tools for frontend checks."
                    ),
                }
            ),
            flush=True,
        )
        return

    print("{}", flush=True)


if __name__ == "__main__":
    main()
