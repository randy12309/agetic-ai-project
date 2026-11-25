---
name: code-parse
description: Reconstruct editable source files from code screenshots. Use for overlapping editor captures where file paths, indentation, clipped lines, and exact literals matter.
---

# Reconstruct code from screenshots

1. **Map the captures to files.** Inventory every image in the supplied folder. Read the editor breadcrumb, active tab and line-number gutter. Group captures by project-relative path and order each group by source line. Count duplicate views toward image coverage. A filename visible only in the sidebar establishes its location but gives no evidence of its contents.

2. **Stage the extraction.** Create a temporary directory for the run. Keep the originals intact. Save an editor crop and a raw OCR draft for each image, using its original stem. Keep line numbers and the left indentation margin visible in the crops. Adjust the crop whenever the window position, zoom or playback layout changes.

3. **Reconstruct one file at a time.** Use the clearest capture for each line. Match overlapping lines by number and content. Distinguish editor wrapping from physical newlines, and sticky headings from repeated code. Preserve imports, comments, docstrings, blank lines, decorators, nested blocks and literal values. Zoom in on regex escapes, quote marks, underscores, case, numeric limits, shell continuations and whitespace inside strings.

4. **Resolve gaps against the originals.** Inspect the bottom of the previous capture and the top of the next before completing a clipped line. Use a visible caller, matching delimiter or repeated definition to resolve an ambiguous token. When the user authorizes inference, record any completion that could affect behavior, with its file, line and supporting evidence. Flag a missing range if several implementations fit. Keep the reconstruction distinct from any proposed code improvements.

5. **Write and verify.** Compare each staged file with its workspace destination before writing. Preserve unrelated edits. Check the finished file against every contributing image, including the first and last visible lines and transitions between captures. For Python, compile the source and check indentation with `tabnanny`. For a Containerfile, inspect instruction boundaries, continuation characters, copy paths and the entry point. Use the project's existing tests when dependencies are available. Matching line counts help detect omissions but cannot establish literal or behavioral fidelity.

Report the files written, the temporary extraction location, checks performed, and any inferred or unreadable lines. Distinguish syntax validation from an actual service run or container build.
