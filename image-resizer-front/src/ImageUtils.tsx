import JSZip from "jszip";

export type Image = {
  name: string;
  base64: string;
  imageKey: string;
  directoryKey: string;
  loaded: boolean;
};

type Directory = {
  name: string;
  directoryKey: string;
  parentKey: string | null;
  imageCount: number;
  subDirectoriesCount: number;
};

export const extractZipWithDirectories = async (file: File): Promise<{ images: Image[]; directories: Directory[] }> => {
  console.log("unzipping with directories");
  const loadedImgs: Image[] = [];
  const directories: Directory[] = [];
  const zip = new JSZip();
  const dirKeys = new Map<string, string>(); // key: dirName, value: dirKey
  try {
    const zipContent = await zip.loadAsync(file);
    const promises: Promise<void>[] = [];
    zipContent.forEach((relativePath, zipEntry) => {
      const generatedDirKey = generateUniqueKey("dir");
      if (zipEntry.dir) {
        const dirName = zipEntry.name.split("/").filter(Boolean).pop() || "";
        const parentKey = getParentKey(relativePath,dirKeys);
        if(parentKey){
          const parentDir = directories.find((dir) => dir.directoryKey === parentKey);
          if (parentDir) {
            parentDir.subDirectoriesCount++;
          }
        }
        directories.push({
          name: dirName,
          directoryKey: generatedDirKey,
          parentKey: parentKey || "",
          imageCount: 0,
          subDirectoriesCount: 0,
        });
        dirKeys.set(dirName, generatedDirKey);
      } else if (isImage(zipEntry.name)) {
        const directoryKey = getDirectoryKey(relativePath,dirKeys);
        if(directoryKey){
          const directory = directories.find((dir) => dir.directoryKey === directoryKey);
          if (directory) {
            directory.imageCount++;
          }
        }
        const promise = zipEntry.async("base64").then((base64Data) => {
          loadedImgs.push({
            name: zipEntry.name.split("/").filter(Boolean).pop() || "",
            base64: `data:image/${getFileExtension(zipEntry.name)};base64,${base64Data}`,
            imageKey: generateUniqueKey("img"),
            directoryKey: directoryKey || "",
            loaded: false,
          });
        });
        promises.push(promise);
      }
    });
    await Promise.all(promises);
    console.log({ images: loadedImgs, directories });
    return { images: loadedImgs, directories };
  } catch (error) {
    alert("Error extracting ZIP file: " + error.message);
    return { images: [], directories: [] };
  }
};

const getDirectoryKey = (relativePath: string,dirKeys: Map<string,string>): string | null => {
  const parts = relativePath.split("/");
  const name = parts.length < 2 ? null : parts[parts.length - 2];
  if(name!==null){
    return dirKeys.get(name) || null;
  }
  return null;
}

// it should get third from right
const getParentKey = (relativePath: string,dirKeys: Map<string,string>): string | null => {
  const parts = relativePath.split("/");
  const name = parts.length < 3 ? null : parts[parts.length - 3];
  if(name!==null){
    return dirKeys.get(name) || null;
  }
  return null;
};
  

// export const extractZip = async (file: File): Promise<Image[]> => {
//   console.log("unzipping");
//   const loadedImgs: Image[] = [];
//   const zip = new JSZip();
//   try {
//     const zipContent = await zip.loadAsync(file);
//     const promises: Promise<void>[] = [];
//     zipContent.forEach((relativePath, zipEntry) => {
//       if (isImage(zipEntry.name)) {
//         const promise = zipEntry.async("base64").then((base64Data) => {
//           loadedImgs.push({
//             name: zipEntry.name,
//             base64: `data:image/${getFileExtension(zipEntry.name)};base64,${base64Data}`,
//             imageKey: generateUniqueKey(),
//             loaded: false,
//           });
//         });
//         promises.push(promise);
//       }
//     });
//     await Promise.all(promises);
//     console.log(loadedImgs);
//     return loadedImgs;
//   } catch (error) {
//     alert("Error extracting ZIP file: " + error.message);
//     return [];
//   }
// };

const isImage = (fileName: string): boolean => {
  const imageExtensions = ["jpg", "jpeg", "png", "gif", "bmp", "webp"];
  return imageExtensions.some((ext) => ext === getFileExtension(fileName));
};

const getFileExtension = (fileName: string): string => {
  return fileName.split(".").pop()?.toLowerCase() || "";
};

const generateUniqueKey = (type: string): string => `${type}_${Date.now()}_${Math.random()}`;
