#!/usr/bin/env python3
"""
WinDroid Master Verification and Diagnostic Test Runner.
Executes all subsystem test suites and validates host PRoot/Linux capabilities.
"""

import sys
import os
import subprocess
import time

def run_suite(title: str, script_name: str) -> bool:
    print("\n" + "=" * 60)
    print(f"  RUNNING TEST SUITE: {title}")
    print("=" * 60)
    start_time = time.time()
    tools_dir = os.path.dirname(os.path.abspath(__file__))
    script_path = os.path.join(tools_dir, script_name)

    result = subprocess.run([sys.executable, script_path], capture_output=False)
    elapsed = time.time() - start_time
    status = "PASSED" if result.returncode == 0 else "FAILED"
    print(f"--> {title}: {status} (took {elapsed:.3f}s)")
    return result.returncode == 0

def test_host_proot():
    print("\n" + "=" * 60)
    print("  CHECKING HOST PROOT SANDBOX COMPATIBILITY")
    print("=" * 60)
    start_time = time.time()
    try:
        proc = subprocess.run(
            ["proot", "-0", "-b", "/dev", "-b", "/proc", "-b", "/sys", "/bin/echo", "PROOT_OK"],
            capture_output=True,
            text=True,
            timeout=5
        )
        elapsed = time.time() - start_time
        if "PROOT_OK" in proc.stdout:
            print("--> Host PRoot Execution: PASSED (Kernel sandbox supported)")
            return True
        else:
            print(f"--> Host PRoot Execution: FAILED ({proc.stderr.strip()})")
            return False
    except Exception as e:
        print(f"--> Host PRoot Execution: FAILED ({e})")
        return False

def main():
    print("############################################################")
    print("       WinDroid Automated Subsystem Test Harness            ")
    print("############################################################")

    results = []
    results.append(("PE Header & Compatibility Engine", run_suite("PE Parser & Compatibility Engine", "test_pe_scanner.py")))
    results.append(("Container Manager & Sandboxing", run_suite("Container Manager & Sandboxing", "test_container_manager.py")))
    results.append(("Host PRoot Sandbox Compatibility", test_host_proot()))

    print("\n" + "#" * 60)
    print("                   TEST SUMMARY RESULTS                    ")
    print("#" * 60)
    all_passed = True
    for name, passed in results:
        status_str = "✓ PASS" if passed else "✗ FAIL"
        if not passed:
            all_passed = False
        print(f"  {status_str:8} | {name}")
    print("#" * 60)

    if all_passed:
        print("\n🎉 ALL TESTS PASSED! WinDroid subsystems are functional.\n")
        return 0
    else:
        print("\n⚠️ SOME TESTS FAILED.\n")
        return 1

if __name__ == "__main__":
    sys.exit(main())
