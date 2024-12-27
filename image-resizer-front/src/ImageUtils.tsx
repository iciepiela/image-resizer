import JSZip from "jszip";

export type Image = {
  name: string;
  base64: string;
  imageKey: string;
  loaded: boolean;
};

export const extractZip = async (file: File): Promise<Image[]> => {
  console.log("unzipping");
  const loadedImgs: Image[] = [];
  const zip = new JSZip();
  try {
    const zipContent = await zip.loadAsync(file);
    const promises: Promise<void>[] = [];
    zipContent.forEach((relativePath, zipEntry) => {
      if (isImage(zipEntry.name)) {
        const promise = zipEntry.async("base64").then((base64Data) => {
          loadedImgs.push({
            name: zipEntry.name,
            base64: `data:image/${getFileExtension(zipEntry.name)};base64,${base64Data}`,
            imageKey: generateUniqueKey(),
            loaded: false,
          });
        });
        promises.push(promise);
      }
    });
    await Promise.all(promises);
    console.log(loadedImgs);
    return loadedImgs;
  } catch (error) {
    alert("Error extracting ZIP file: " + error.message);
    return [];
  }
};

const isImage = (fileName: string): boolean => {
  const imageExtensions = ["jpg", "jpeg", "png", "gif", "bmp", "webp"];
  return imageExtensions.some((ext) => ext === getFileExtension(fileName));
};

const getFileExtension = (fileName: string): string => {
  return fileName.split(".").pop()?.toLowerCase() || "";
};

const generateUniqueKey = (): string => `img_${Date.now()}_${Math.random()}`;
