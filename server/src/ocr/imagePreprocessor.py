#!/usr/bin/env python3
"""
Lightweight image preprocessor using OpenCV.
Reads raw image bytes from stdin and writes preprocessed image bytes to stdout.
Zero temporary files created.
"""

import sys
import argparse
import numpy as np
import cv2

def parse_args():
    parser = argparse.ArgumentParser(description="Lightweight OpenCV image preprocessor")
    parser.add_argument("--grayscale", action="store_true", default=True, help="Convert image to grayscale")
    parser.add_argument("--no-grayscale", dest="grayscale", action="store_false", help="Keep original color channels")
    parser.add_argument("--resize-max-dim", type=int, default=2000, help="Resize bounding box maximum dimension")
    parser.add_argument("--min-dim-upscale", type=int, default=300, help="Upscale if both width and height are below this value")
    parser.add_argument("--adaptive-threshold", action="store_true", default=False, help="Apply adaptive thresholding")
    parser.add_argument("--denoise", action="store_true", default=False, help="Apply median blur noise reduction")
    parser.add_argument("--output-format", type=str, default=".png", help="Output image format extension (.png, .jpg)")
    return parser.parse_args()

def preprocess_image(image_bytes, args):
    # Decode image from buffer
    nparr = np.frombuffer(image_bytes, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_UNCHANGED)

    if img is None:
        raise ValueError("UNREADABLE_IMAGE: Unable to decode image buffer")

    # 1. Grayscale conversion if requested
    if args.grayscale:
        if len(img.shape) == 3:
            if img.shape[2] == 4:
                # Handle alpha channel
                img = cv2.cvtColor(img, cv2.COLOR_BGRA2GRAY)
            else:
                img = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    elif len(img.shape) == 3 and img.shape[2] == 4:
        img = cv2.cvtColor(img, cv2.COLOR_BGRA2BGR)

    h, w = img.shape[:2]

    # 2. Resizing where appropriate (upscale if too small, downscale if too large)
    if w < args.min_dim_upscale and h < args.min_dim_upscale:
        scale = max(args.min_dim_upscale / w, args.min_dim_upscale / h)
        new_w, new_h = int(w * scale), int(h * scale)
        img = cv2.resize(img, (new_w, new_h), interpolation=cv2.INTER_CUBIC)
    elif max(w, h) > args.resize_max_dim:
        scale = args.resize_max_dim / float(max(w, h))
        new_w, new_h = int(w * scale), int(h * scale)
        img = cv2.resize(img, (new_w, new_h), interpolation=cv2.INTER_AREA)

    # 3. Noise reduction when requested
    if args.denoise:
        # Lightweight median filter (kernel 3x3)
        img = cv2.medianBlur(img, 3)

    # 4. Adaptive thresholding only when requested
    if args.adaptive_threshold:
        if len(img.shape) == 3:
            gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        else:
            gray = img
        img = cv2.adaptiveThreshold(
            gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11, 2
        )

    # Encode back to format
    ext = args.output_format if args.output_format.startswith(".") else f".{args.output_format}"
    success, encoded = cv2.imencode(ext, img)
    if not success:
        raise ValueError("ENCODE_FAILED: Could not encode preprocessed image")

    return encoded.tobytes()

def main():
    args = parse_args()

    # Read binary stream from stdin
    input_bytes = sys.stdin.buffer.read()
    if not input_bytes:
        sys.stderr.write("EMPTY_INPUT: No image bytes received on stdin\n")
        sys.exit(1)

    try:
        output_bytes = preprocess_image(input_bytes, args)
        sys.stdout.buffer.write(output_bytes)
        sys.stdout.buffer.flush()
    except Exception as e:
        sys.stderr.write(f"PREPROCESS_ERROR: {str(e)}\n")
        sys.exit(1)

if __name__ == "__main__":
    main()
