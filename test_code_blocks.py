#!/usr/bin/env python3
import re
import subprocess
import tempfile
import os

# Path to the markdown file with examples
markdown_path = "docs/03-example-models.md"
# Path to your Codephy validator JAR file
validator_jar = "validator/java/target/codephy-validator-0.1.0-jar-with-dependencies.jar"

# Read the markdown file
with open(markdown_path, "r") as f:
    content = f.read()

# Regular expression to match JSON code blocks marked with ```json
code_block_pattern = re.compile(r"```json(.*?)```", re.DOTALL)
code_blocks = code_block_pattern.findall(content)

if not code_blocks:
    print("No JSON code blocks found.")
    exit(1)

# Process each code block
for idx, block in enumerate(code_blocks, start=1):
    # Clean up the block (strip leading/trailing whitespace)
    json_text = block.strip()
    
    # Write the code block to a temporary file
    with tempfile.NamedTemporaryFile(delete=False, suffix=".json", mode="w") as tmp:
        tmp.write(json_text)
        tmp_filename = tmp.name

    # Run the validator on the temporary file
    try:
        result = subprocess.run(
            ["java", "-jar", validator_jar, tmp_filename],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            check=False
        )
        print(f"--- Validation result for code block {idx} ---")
        print(result.stdout)
        if result.stderr:
            print("Errors:")
            print(result.stderr)
    finally:
        # Remove the temporary file
        os.remove(tmp_filename)
