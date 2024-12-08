import JSZip from 'jszip';

export const extractZip = async (file) => {
    console.log("unzipping");
    const loadedImgs = []
    const zip = new JSZip();
    try {
        const zipContent = await zip.loadAsync(file);
        const promises = [];
        zipContent.forEach((relativePath, zipEntry) => {
            console.log("doo");
            if (isImage(zipEntry.name)) {
                const promise = zipEntry.async('base64').then((base64Data) => {
                    loadedImgs.push({
                        name: zipEntry.name,
                        base64: `data:image/${getFileExtension(zipEntry.name)};base64,${base64Data}`,
                        key: generateUniqueKey(),
                        loaded: false
                    });
                });
                promises.push(promise);
            }
        });
        await Promise.all(promises);
        console.log(loadedImgs)
        return loadedImgs;
    } catch (error) {
        alert('Error extracting ZIP file: ' + error.message);
    }
};

const isImage = (fileName) => {
    const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'];
    return imageExtensions.some((ext) => ext === getFileExtension(fileName));
};
const getFileExtension = (fileName) => {
    return fileName.split('.').pop().toLowerCase();
};
const generateUniqueKey = () => `img_${Date.now()}_${Math.random()}`;
