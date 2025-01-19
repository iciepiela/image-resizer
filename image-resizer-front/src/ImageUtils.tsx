import JSZip from "jszip";

export type Image = {
  name: string;
  base64: string;
  imageKey: string;
  loaded: boolean;
};
export type Directory = {
  name: string | null;
  directories: Directory[];
  images: Image[];
  dirKey: string | null;
  hasParent: boolean;
  imageCount: number;
  subDirectoriesCount: number;
};

export const extractZip = async (file: File): Promise<Directory> => {
  console.log("unzipping");
  const zip = new JSZip();
  try {
    const zipContent = await zip.loadAsync(file);
    const root: Directory = { name: "", directories: [], images: [], dirKey: "root", 
      hasParent: false, subDirectoriesCount: 0, imageCount:0 };
    const promises: Promise<void>[] = [];
    zipContent.forEach((relativePath, zipEntry) => {
      const pathParts = relativePath.split("/").filter((part) => part); // Split path and remove empty parts
      const isDirectory = zipEntry.dir;

      if (isDirectory) {
        createDirectory(root, pathParts);
      } else if (isImage(zipEntry.name)) {
        
        const promise = zipEntry.async("base64").then((base64Data) => {
          const image: Image ={
            name: zipEntry.name,
            base64: `data:image/${getFileExtension(zipEntry.name)};base64,${base64Data}`,
            imageKey: generateUniqueKey("img"),
            loaded: false,
          };
          const parentDirectory = createDirectory(root, pathParts.slice(0, -1));
          parentDirectory.images.push(image);
        });
        promises.push(promise);
      }
    });
    await Promise.all(promises);
    updateDirectoryCounts(root);
    return root.directories[0];
  } catch (error) {
    alert("Error extracting ZIP file: " + error.message);
    return {
      name: "",
      directories: [],
      images: [],
      dirKey: generateUniqueKey("dir"),
      hasParent: false,
      subDirectoriesCount:0,
      imageCount: 0
    };
  }
};

const isImage = (fileName: string): boolean => {
  const imageExtensions = ["jpg", "jpeg", "png", "gif", "bmp", "webp"];
  return imageExtensions.some((ext) => ext === getFileExtension(fileName));
};

const getFileExtension = (fileName: string): string => {
  return fileName.split(".").pop()?.toLowerCase() || "";
};

const generateUniqueKey = (type: string): string => `${type}_${Date.now()}_${Math.random()}`;


const createDirectory = (root: Directory, pathParts: string[]): Directory => {
  let current = root;

  for (const part of pathParts) {
    let subdir = current.directories.find((dir) => dir.name === part);
    if (!subdir) {
      subdir = { name: part, directories: [], images: [], dirKey: generateUniqueKey("dir"), hasParent: true, imageCount:0, subDirectoriesCount:0 };
      current.directories.push(subdir);
    }
    current = subdir;
  }

  return current;
};

const updateDirectoryCounts = (directory: Directory): void => {
  let imageCount = directory.images.length;
  let subDirectoriesCount = directory.directories.length;

  for (const subdir of directory.directories) {
    updateDirectoryCounts(subdir); 
  }

  directory.imageCount = imageCount;
  directory.subDirectoriesCount = subDirectoriesCount;
};
