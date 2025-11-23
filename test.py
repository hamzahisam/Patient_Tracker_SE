import os
from datetime import datetime

def scan_directory(root_folder):
    results = []

    for dirpath, dirnames, filenames in os.walk(root_folder):
        
        # Add folder entries
        for dirname in dirnames:
            folder_path = os.path.join(dirpath, dirname)
            results.append({
                "type": "folder",
                "name": dirname,
                "path": folder_path,
            })

        # Add file entries
        for filename in filenames:
            file_path = os.path.join(dirpath, filename)
            file_extension = os.path.splitext(filename)[1].lower().replace(".", "")
            file_size = os.path.getsize(file_path)
            modified_time = datetime.fromtimestamp(os.path.getmtime(file_path))

            results.append({
                "type": "file",
                "name": filename,
                "path": file_path,
                "extension": file_extension
            })

    return results


# ------------------------- RUN EXAMPLE -------------------------
if __name__ == "__main__":
    folder_to_scan = r"D:\SE\Project\Patient_Tracker_SE"  # change this path
    data = scan_directory(folder_to_scan)

    for item in data:
        print(item)
