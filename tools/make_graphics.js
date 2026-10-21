// Draws the Techno TV app and store graphics from assets/arc_reactor.png (macOS):
//   osascript -l JavaScript tools/make_graphics.js <reactor.png> <out.png> <width> <height> [label]
// Navy gradient, the reactor with a soft glow, and "TECHNO TV" lettering when there's room.
ObjC.import('AppKit');

function run(argv) {
  const [src, out] = argv;
  const w = parseInt(argv[2]), h = parseInt(argv[3]);
  const showText = argv[4] !== 'nolabel';
  const reactor = $.NSImage.alloc.initWithContentsOfFile(src);

  const rep = $.NSBitmapImageRep.alloc.initWithBitmapDataPlanesPixelsWidePixelsHighBitsPerSampleSamplesPerPixelHasAlphaIsPlanarColorSpaceNameBytesPerRowBitsPerPixel(
    null, w, h, 8, 4, true, false, $.NSDeviceRGBColorSpace, 0, 0);
  $.NSGraphicsContext.saveGraphicsState;
  $.NSGraphicsContext.setCurrentContext($.NSGraphicsContext.graphicsContextWithBitmapImageRep(rep));

  const bg = $.NSGradient.alloc.initWithStartingColorEndingColor(
    $.NSColor.colorWithSRGBRedGreenBlueAlpha(0.04, 0.12, 0.26, 1),
    $.NSColor.colorWithSRGBRedGreenBlueAlpha(0.01, 0.02, 0.05, 1));
  bg.drawInRectAngle($.NSMakeRect(0, 0, w, h), -30);

  const wide = w / h > 1.3;
  const size = wide ? h * 0.62 : Math.min(w, h) * (showText ? 0.6 : 0.78);
  const cx = wide && showText ? w * 0.24 : w / 2;
  const cy = wide || !showText ? h / 2 : h * 0.58;

  const glow = $.NSGradient.alloc.initWithStartingColorEndingColor(
    $.NSColor.colorWithSRGBRedGreenBlueAlpha(0.3, 0.64, 1, 0.55),
    $.NSColor.colorWithSRGBRedGreenBlueAlpha(0.3, 0.64, 1, 0));
  glow.drawFromCenterRadiusToCenterRadiusOptions($.NSMakePoint(cx, cy), 0, $.NSMakePoint(cx, cy), size * 0.85, 0);
  reactor.drawInRectFromRectOperationFraction($.NSMakeRect(cx - size / 2, cy - size / 2, size, size),
    $.NSZeroRect, $.NSCompositingOperationSourceOver, 1);

  if (showText) {
    const para = $.NSMutableParagraphStyle.alloc.init;
    para.alignment = wide ? $.NSTextAlignmentLeft : $.NSTextAlignmentCenter;
    const font = $.NSFont.systemFontOfSizeWeight(wide ? h * 0.115 : h * 0.1, -0.4);
    const shadow = $.NSShadow.alloc.init;
    shadow.shadowColor = $.NSColor.colorWithSRGBRedGreenBlueAlpha(0.18, 0.48, 1, 1);
    shadow.shadowBlurRadius = h * 0.03;
    const attrs = $.NSMutableDictionary.alloc.init;
    attrs.setObjectForKey(font, $.NSFontAttributeName);
    attrs.setObjectForKey($.NSColor.whiteColor, $.NSForegroundColorAttributeName);
    attrs.setObjectForKey(para, $.NSParagraphStyleAttributeName);
    attrs.setObjectForKey($(h * 0.012), $.NSKernAttributeName);
    attrs.setObjectForKey(shadow, $.NSShadowAttributeName);
    const textRect = wide
      ? $.NSMakeRect(w * 0.45, h * 0.44, w * 0.53, h * 0.18)
      : $.NSMakeRect(0, h * 0.08, w, h * 0.16);
    $('TECHNO TV').drawInRectWithAttributes(textRect, attrs);
    if (wide && w >= 1000) { // tagline only where it fits
      const sub = $.NSMutableDictionary.alloc.init;
      sub.setObjectForKey($.NSFont.systemFontOfSizeWeight(h * 0.038, -0.2), $.NSFontAttributeName);
      sub.setObjectForKey($.NSColor.colorWithSRGBRedGreenBlueAlpha(0.75, 0.89, 1, 0.85), $.NSForegroundColorAttributeName);
      sub.setObjectForKey($(h * 0.006), $.NSKernAttributeName);
      $('AD-FREE HOME SCREEN FOR GOOGLE TV').drawInRectWithAttributes($.NSMakeRect(w * 0.455, h * 0.36, w * 0.53, h * 0.07), sub);
    }
  }
  $.NSGraphicsContext.restoreGraphicsState;
  rep.representationUsingTypeProperties($.NSBitmapImageFileTypePNG, $()).writeToFileAtomically(out, true);
  return out;
}
