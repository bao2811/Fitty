from pathlib import Path
import sys

from PIL import Image


def main() -> int:
    if len(sys.argv) != 3:
        print("Usage: extract_gif_first_frame.py <input_gif> <output_png>", file=sys.stderr)
        return 1

    input_path = Path(sys.argv[1])
    output_path = Path(sys.argv[2])
    output_path.parent.mkdir(parents=True, exist_ok=True)

    with Image.open(input_path) as image:
        image.seek(0)
        frame = image.convert("RGBA")
        frame.save(output_path, format="PNG")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
