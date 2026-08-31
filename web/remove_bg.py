from PIL import Image
import sys

def remove_white_bg(image_path):
    try:
        img = Image.open(image_path).convert("RGBA")
        datas = img.getdata()
        
        newData = []
        for item in datas:
            # Check if pixel is close to white
            if item[0] > 230 and item[1] > 230 and item[2] > 230:
                newData.append((255, 255, 255, 0)) # Transparent
            else:
                newData.append(item)
                
        img.putdata(newData)
        img.save(image_path, "WEBP")
        print("Successfully removed white background.")
    except Exception as e:
        print(f"Error processing image: {e}")

remove_white_bg(r"c:\Users\hp\AndroidStudioProjects\Takaful\web\app-icon.webp")
