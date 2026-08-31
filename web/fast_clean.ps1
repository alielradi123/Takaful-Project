$csharp = @"
using System;
using System.Drawing;
using System.Drawing.Imaging;

public class ImageP {
    public static void Clean(string inFile, string outFile) {
        Bitmap bmp = new Bitmap(inFile);
        Rectangle rect = new Rectangle(0, 0, bmp.Width, bmp.Height);
        BitmapData bmpData = bmp.LockBits(rect, ImageLockMode.ReadWrite, PixelFormat.Format32bppArgb);
        IntPtr ptr = bmpData.Scan0;
        int bytes = Math.Abs(bmpData.Stride) * bmp.Height;
        byte[] rgbValues = new byte[bytes];
        System.Runtime.InteropServices.Marshal.Copy(ptr, rgbValues, 0, bytes);

        for (int counter = 0; counter < rgbValues.Length; counter += 4) {
            byte b = rgbValues[counter];
            byte g = rgbValues[counter + 1];
            byte r = rgbValues[counter + 2];
            
            if (r > 230 && g > 230 && b > 230) {
                rgbValues[counter + 3] = 0; // Alpha = 0
            }
        }

        System.Runtime.InteropServices.Marshal.Copy(rgbValues, 0, ptr, bytes);
        bmp.UnlockBits(bmpData);
        bmp.Save(outFile, ImageFormat.Png);
        bmp.Dispose();
    }
}
"@

try {
    Add-Type -TypeDefinition $csharp -ReferencedAssemblies System.Drawing
    [ImageP]::Clean("c:\Users\hp\AndroidStudioProjects\Takaful\web\app-icon.webp", "c:\Users\hp\AndroidStudioProjects\Takaful\web\app-icon-transparent.png")
    Write-Host "Done"
} catch {
    Write-Host "Error: $_"
}
