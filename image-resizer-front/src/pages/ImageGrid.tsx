import React, { useState, useEffect, useRef } from "react";
import { fromEvent, from, mergeMap, tap, finalize, catchError, Observable, EMPTY } from "rxjs";
import { map } from "rxjs/operators";
import { extractZipWithDirectories, Image } from "../ImageUtils.tsx";
import Button from "@mui/material/Button";
import FolderIcon from '@mui/icons-material/Folder';
import { IconButton } from "@mui/material";
import "./ImageGrid.css";
import sad from "../sad.png";


type ImageDto = {
  base64: string;
  name: string;
  imageKey: string;
  width?: number;
  height?: number;
  loaded: boolean;
};

type DirectoryDto = {
  name: string;
  directoryKey: string;
  parentKey: string | null;
  imageCount: number;
  subDirectoriesCount: number;
}

type SessionKey = {
  sessionKey: string | null;
  imageCount: number;
};

const ImageGrid: React.FC = () => {
  const [images, setImages] = useState<ImageDto[]>([]);
  const [mainDirectory, setMainDirectory] = useState<DirectoryDto>();
  const [subDirectories, setSubDirectories] = useState<DirectoryDto[]>([]);
  const [hoveredImage, setHoveredImage] = useState<ImageDto | null>(null);
  const [hoveredImageStyle, setHoveredImageStyle] = useState<React.CSSProperties>({});
  const [hoveredImageClicked, setHoveredImageClicked] = useState(false);
  const [sessionKey, setSessionKey] = useState<SessionKey | undefined>();
  const [imageSize, setImageSize] = useState<String>("small");
  const [sessionOnly, setSessionOnly] = useState<boolean>(true);
  const [activeSubscriptions, setActiveSubscriptions] = useState<Map<string, EventSource>>(new Map());
  const [directoryActiveSubscriptions, setDirectoryActiveSubscriptions] = useState<Map<string, EventSource>>(new Map());
  const [firstPart, setFirstPart] = useState<boolean>(true);


  const COMPLETE_REQUEST = "COMPLETE_REQUEST";
  const ERROR = "ERROR";

  // useEffect(() => {
  //   if (sessionKey) {
  //     loadPhotos(sessionOnly, sessionKey, imageSize);
  //   }
  // }, [sessionKey]);

  useEffect(() => {

    loadPhotosByImageKey(imageSize);

  }, [imageSize])

  useEffect(() => {
    console.log("Main Directory", mainDirectory);
    loadDirectories(mainDirectory?.directoryKey || "");
    if (mainDirectory && !mainDirectory.parentKey) {
      setFirstPart(true);
    } else {
      setFirstPart(false);
    }
    if (mainDirectory) {
      loadImagesByDirectoryKey(imageSize, mainDirectory.directoryKey);
    }
  }, [mainDirectory]);

  const getParentDirectory = async (directoryKey: string, retries: number) => {
    for (let attempt = 1; attempt <= retries; attempt++) {
      try {
        const isServerHealthy = await checkServerHealth();
        if (!isServerHealthy) {
          console.warn(`Server is unavailable. Attempt ${attempt} of ${retries}. Retrying...`);
          await new Promise((resolve) => setTimeout(resolve, 2000));
          continue;
        }

        const response = await fetch(
          `http://localhost:8080/images/directories/parent?directoryKey=${directoryKey}`,
          {
            method: "GET",
            headers: { "Content-Type": "application/json" },
          }
        );
        console.log("Response", response);
        if (!response.ok && response.status !== 404) {
          throw new Error(`HTTP error! Status: ${response.status}`);
        }

        const directoryData = await response.json();
        console.log("Fetched parent", directoryData);
        setMainDirectory(directoryData);

      } catch (error) {
        console.error(`Error fetching parent attempt ${attempt}:`, error);

        if (attempt === retries) {
          console.error("Max retries reached. Fetch failed.");
          throw error;
        }

        console.warn(`Retrying fetch... (${attempt + 1}/${retries})`);
        await new Promise((resolve) => setTimeout(resolve, 2000));
      }
    }
    return null;
  };
  const getFirstDirectoryCount= (sessionKey: string,sessionOnly: boolean): number => {
    
    const url = sessionOnly ? 
    `http://localhost:8080/images/directories/first-by-session?sessionKey=${sessionKey}` : 
    `http://localhost:8080/images/directories/first-all`;
    fetch(url)
      .then((response) => response.json())
      .then((data) => {
        return data.subDirectoriesCount;
      })
      .catch((error) => {
        console.error("Error fetching first directory:", error);
        return 0;
      });
    return 0;
  }

  const loadDirectories = (parentKey: string) => {
    console.log("SubDirectories", subDirectories);
    setSubDirectories([]);
    let directoryCount=mainDirectory?.subDirectoriesCount || 0;
    if(parentKey === null) {
      console.log("AGAGA")
      directoryCount=getFirstDirectoryCount(sessionKey?.sessionKey || "", sessionOnly);
    }
    console.log("Directory Count", directoryCount);
    const url = `http://localhost:8080/images/directories/by-parent-key?directoryKey=${parentKey}`;
    const eventSource = new EventSource(url);
    const dirSet = new Set<string>();
    for(let i = 0; i < directoryCount; i++) {
    if (directoryActiveSubscriptions.get(parentKey)) {
      const prevEventSource = directoryActiveSubscriptions.get(parentKey);
      console.log(prevEventSource);
      prevEventSource?.close();
      setDirectoryActiveSubscriptions((prev) => {
        const newMap = new Map(prev);
        newMap.delete(parentKey);
        return newMap;
      });
    }
    setDirectoryActiveSubscriptions((prev) => {
      const newMap = new Map(prev);
      newMap.set(parentKey, eventSource);
      return newMap;
    });
  }

    const imageStream$ = fromEvent<MessageEvent>(eventSource, "message").pipe(
      map((event) => {
        const parsedData = JSON.parse(event.data);
        return parsedData.body;
      })
    );
    const subscription = imageStream$.subscribe({
      next: (directoryData: any) => {
        if (
          directoryData.name !== COMPLETE_REQUEST &&
          directoryData.base64 !== COMPLETE_REQUEST
        ) {
          const newDirectory: DirectoryDto = {
            name: directoryData.name,
            directoryKey: directoryData.directoryKey,
            parentKey: directoryData.parentKey,
            imageCount: directoryData.imageCount,
            subDirectoriesCount: directoryData.subDirectoriesCount
          };

          setSubDirectories((prevDirectories) => {
            const directoryExists = prevDirectories.some(
              (directory) => 
                directory.directoryKey === newDirectory.directoryKey &&
                directory.name === newDirectory.name &&
                directory.parentKey === newDirectory.parentKey &&
                directory.subDirectoriesCount === newDirectory.subDirectoriesCount
            );


            if (!directoryExists) {
              const newDirectories = prevDirectories.map((directory) =>
                directory.directoryKey === newDirectory.directoryKey
                  ? { ...directory, ...newDirectory }
                  : directory
              );

              const isReplaced = newDirectories.some((directory) => directory.directoryKey === newDirectory.directoryKey);

              if (!isReplaced) {
                return [...newDirectories, newDirectory];
              }

              return newDirectories;
            }

            return prevDirectories;
          });

          dirSet.add(newDirectory.directoryKey);
          if (mainDirectory && dirSet.size >= directoryCount) {
            console.log("ended all");
            eventSource.close();
          }
        }
      },
      error: (err) => {
        console.error("Error receiving subDirectory data:", err);
      },
      complete: () => {
        console.log("Complete");
      },
    });
    console.log("SubDirectories", subDirectories);
    return () => {
      console.log("closing");
      eventSource.close();
      subscription.unsubscribe();
    };
  };

  const loadImagesByDirectoryKey = (size: String, directoryKey: string)=> {
    console.log("directoryKey", directoryKey,mainDirectory);
    if(!mainDirectory) return;
    const url = `http://localhost:8080/images/resized/by-directory?sizeString=${size}&directoryKey=${directoryKey}`;
    const eventSource = new EventSource(url);
    const imageSet = new Set<string>();
    const imageCount = mainDirectory.imageCount;

    images.forEach((image) => {
      const imageKey = image.imageKey;
      if (activeSubscriptions.get(imageKey)) {
        const prevEventSource = activeSubscriptions.get(imageKey);
        console.log(prevEventSource);
        prevEventSource?.close();
        setActiveSubscriptions((prev) => {
          const newMap = new Map(prev);
          newMap.delete(imageKey);

          return newMap;
        });
      }
      setActiveSubscriptions((prev) => {
        const newMap = new Map(prev);
        newMap.set(imageKey, eventSource);

        return newMap;
      });
    }
    );

    const imageStream$ = fromEvent<MessageEvent>(eventSource, "message").pipe(
      map((event) => {
        const parsedData = JSON.parse(event.data);
        return parsedData.body;
      })
    );

    const subscription = imageStream$.subscribe({
      next: (imageData: any) => {
        if (
          imageData.name === COMPLETE_REQUEST &&
          imageData.base64 === COMPLETE_REQUEST &&
          !sessionOnly
        ) {
          console.log("ended all");
          eventSource.close();
        } else if (
          imageData.name !== COMPLETE_REQUEST &&
          imageData.base64 !== COMPLETE_REQUEST
        ) {
          const newImage: ImageDto = {
            base64: `data:image/jpg;base64,${imageData.base64}`,
            name: imageData.name,
            imageKey: imageData.imageKey,
            width: imageData.width,
            height: imageData.height,
            loaded: true,
          };

          setImages((prevImages) => {
            if (size !== imageSize) return prevImages;

            const imageExists = prevImages.some(
              (image) =>
                image.imageKey === newImage.imageKey &&
                image.width === newImage.width &&
                image.height === newImage.height &&
                image.loaded === true
            );


            if (!imageExists) {
              const newImages = prevImages.map((image) =>
                image.imageKey === newImage.imageKey
                  ? { ...image, ...newImage }
                  : image
              );

              const isReplaced = newImages.some((image) => image.imageKey === newImage.imageKey);

              if (!isReplaced) {
                return [...newImages, newImage];
              }

              return newImages;
            }

            return prevImages;
          });

          imageSet.add(newImage.imageKey);
          if (imageSet.size >= imageCount && sessionOnly) {
            eventSource.close();
          }
        }
      },
      error: (err) => {
        console.error("Error receiving image data:", err);
      },
      complete: () => {
        console.log("Complete");
      },
    });

    return () => {
      console.log("closing");
      eventSource.close();
      subscription.unsubscribe();
    };
  };

  const loadImageByImageKey = (size: String, imageKey: string): Observable<void> => {
    if (imageKey === null) return EMPTY;
    const url = `http://localhost:8080/images/resized/by-image-key?imageKey=${imageKey}&sizeString=${size}`;
    const eventSource = new EventSource(url);
    const imageSet = new Set<string>();

    if (activeSubscriptions.get(imageKey)) {
      const prevEventSource = activeSubscriptions.get(imageKey);
      console.log(prevEventSource);
      prevEventSource?.close();
      setActiveSubscriptions((prev) => {
        const newMap = new Map(prev);
        newMap.delete(imageKey);

        return newMap;
      });
    }
    setActiveSubscriptions((prev) => {
      const newMap = new Map(prev);
      newMap.set(imageKey, eventSource);

      return newMap;
    });
    return fromEvent<MessageEvent>(eventSource, "message").pipe(
      map((event) => JSON.parse(event.data).body),
      tap((imageData: any) => {
        if (imageData.name !== "COMPLETE_REQUEST" && imageData.base64 !== "COMPLETE_REQUEST") {
          const newImage: ImageDto = {
            base64: `data:image/jpg;base64,${imageData.base64}`,
            name: imageData.name,
            imageKey: imageData.imageKey,
            width: imageData.width,
            height: imageData.height,
            loaded: true,
          };

          setImages((prevImages) => {
            if (size !== imageSize) return prevImages;
            const newImages = prevImages.map((image) =>
              image.imageKey === newImage.imageKey
                ? { ...image, ...newImage }
                : image
            );
            return newImages;
          });

          imageSet.add(newImage.imageKey);

          if (imageSet.size >= 1) {
            // console.log(imageKey + ": ended imageKey", imageSet);
            eventSource.close();
          }
        }
      }),
      catchError((err) => {
        console.error(`Error loading image for key ${imageKey}:`, err);
        return EMPTY;
      }),
      finalize(() => {
        console.log(`Stream for imageKey ${imageKey} closed`);
        eventSource.close();
      })
    );
  };

  const loadPhotosByImageKey = (size: String): void => {
    console.log("Loading photos by imageKey...");
    console.log(images)
    setImages((prevImages) =>
      prevImages.map((image) => ({
        ...image,
        loaded: false,
      }))
    );
    const imageStream$ = from(images).pipe(
      mergeMap((image) =>
        loadImageByImageKey(size, image.imageKey).pipe(
          catchError((err) => {
            console.error(`Error processing image key ${image.imageKey}:`, err);
            return EMPTY;
          })
        )
      ),
      finalize(() => {
        console.log("All images processed");
      })
    );

    imageStream$.subscribe({
      complete: () => console.log("All image streams completed"),
    });
  };
  const checkServerHealth = async (): Promise<boolean> => {
    try {
      const response = await fetch("http://localhost:8080/images/health", {
        method: "GET",
      });

      return response.ok;
    } catch (error) {
      console.error("Server health check failed:", error);
      return false;
    }
  };

  // const loadPhotos = (sessionOnly: boolean, sessionKey: SessionKey, size: String) => {
  //   console.log("Loading photos...");

  //   const url = sessionOnly
  //     ? `http://localhost:8080/images/resized/by-session?sessionKey=${sessionKey.sessionKey}&sizeString=${size}`
  //     : `http://localhost:8080/images/resized/all?sizeString=${size}`;

  //   const eventSource = new EventSource(url);
  //   const imageSet = new Set<string>();
  //   const imageCount = sessionKey.imageCount;
  //   console.log(sessionKey);

  //   images.forEach((image) => {
  //     const imageKey = image.imageKey;
  //     if (activeSubscriptions.get(imageKey)) {
  //       const prevEventSource = activeSubscriptions.get(imageKey);
  //       console.log(prevEventSource);
  //       prevEventSource?.close();
  //       setActiveSubscriptions((prev) => {
  //         const newMap = new Map(prev);
  //         newMap.delete(imageKey);

  //         return newMap;
  //       });
  //     }
  //     setActiveSubscriptions((prev) => {
  //       const newMap = new Map(prev);
  //       newMap.set(imageKey, eventSource);

  //       return newMap;
  //     });
  //   }
  //   );

  //   const imageStream$ = fromEvent<MessageEvent>(eventSource, "message").pipe(
  //     map((event) => {
  //       const parsedData = JSON.parse(event.data);
  //       return parsedData.body;
  //     })
  //   );

  //   const subscription = imageStream$.subscribe({
  //     next: (imageData: any) => {
  //       if (
  //         imageData.name === COMPLETE_REQUEST &&
  //         imageData.base64 === COMPLETE_REQUEST &&
  //         !sessionOnly
  //       ) {
  //         console.log("ended all");
  //         eventSource.close();
  //       } else if (
  //         imageData.name !== COMPLETE_REQUEST &&
  //         imageData.base64 !== COMPLETE_REQUEST
  //       ) {
  //         const newImage: Image = {
  //           base64: `data:image/jpg;base64,${imageData.base64}`,
  //           name: imageData.name,
  //           imageKey: imageData.imageKey,
  //           width: imageData.width,
  //           height: imageData.height,
  //           loaded: true,
  //         };

  //         setImages((prevImages) => {
  //           if (size !== imageSize) return prevImages;

  //           const imageExists = prevImages.some(
  //             (image) =>
  //               image.imageKey === newImage.imageKey &&
  //               image.width === newImage.width &&
  //               image.height === newImage.height &&
  //               image.loaded === true
  //           );


  //           if (!imageExists) {
  //             const newImages = prevImages.map((image) =>
  //               image.imageKey === newImage.imageKey
  //                 ? { ...image, ...newImage }
  //                 : image
  //             );

  //             const isReplaced = newImages.some((image) => image.imageKey === newImage.imageKey);

  //             if (!isReplaced) {
  //               return [...newImages, newImage];
  //             }

  //             return newImages;
  //           }

  //           return prevImages;
  //         });

  //         imageSet.add(newImage.imageKey);
  //         if (imageSet.size >= imageCount && sessionOnly) {
  //           console.log(sessionKey.sessionKey + ": ended", imageSet);
  //           eventSource.close();
  //         }
  //       }
  //     },
  //     error: (err) => {
  //       console.error("Error receiving image data:", err);
  //     },
  //     complete: () => {
  //       console.log("Complete");
  //     },
  //   });

  //   return () => {
  //     console.log("closing");
  //     eventSource.close();
  //     subscription.unsubscribe();
  //   };
  // };

  const uploadDirectories = async (directoryList: DirectoryDto[]) => {
    console.log("Starting directory upload...");

    const preparedDirectories = directoryList.map((directory) => ({
      name: directory.name,
      directoryKey: directory.directoryKey,
      parentKey: directory.parentKey,
      imageCount: directory.imageCount,
      subDirectoriesCount: directory.subDirectoriesCount
    }));

    try {
      const response = await uploadDirectoriesToBackend(preparedDirectories);

      if (response) {
        console.log("All directories uploaded successfully.");
      } else {
        console.log("Failed to upload directories.");
      }
    } catch (error) {
      console.error("Error uploading directories:", error);
    }
  }

  const uploadDirectoriesToBackend = async (directories: any[], retries = 10): Promise<boolean> => {
    for (let attempt = 1; attempt <= retries; attempt++) {
      try {
        const isServerHealthy = await checkServerHealth();
        if (!isServerHealthy) {
          console.warn(`Server is unavailable. Attempt ${attempt} of ${retries}. Retrying...`);
          await new Promise((resolve) => setTimeout(resolve, 2000));
          continue;
        }

        const response = await fetch("http://localhost:8080/images/directories/upload", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(directories),
        });

        if (!response.ok) {
          throw new Error(`HTTP error! Status: ${response.status}`);
        }

        return true;
      } catch (error) {
        console.error(`Error during directory upload attempt ${attempt}:`, error);

        if (attempt === retries) {
          console.error("Max retries reached. Upload failed.");
          throw error;
        }

        console.warn(`Retrying directory upload... (${attempt + 1}/${retries})`);
        await new Promise((resolve) => setTimeout(resolve, 2000));
      }
    }

    return false;
  }

  const uploadPhotos = async (imageList: Image[]) => {
    console.log("Starting photo upload...");

    const preparedImages = imageList.map((image) => ({
      imageKey: image.imageKey,
      name: image.name,
      base64: image.base64,
      directoryKey: image.directoryKey,
    }));
    const imageCount = preparedImages.length;

    try {
      const response = await uploadImagesToBackend(preparedImages);

      if (response) {
        console.log("All photos uploaded successfully.");
        setSessionOnly(true);
        setSessionKey({ sessionKey: response, imageCount: imageCount });
      } else {
        console.log("Failed to upload photos.");
      }
    } catch (error) {
      console.error("Error uploading photos:", error);
    }
  };

  const uploadImagesToBackend = async (images: any[], retries = 10): Promise<string | null> => {
    for (let attempt = 1; attempt <= retries; attempt++) {
      try {
        const isServerHealthy = await checkServerHealth();
        if (!isServerHealthy) {
          console.warn(`Server is unavailable. Attempt ${attempt} of ${retries}. Retrying...`);
          await new Promise((resolve) => setTimeout(resolve, 2000));
          continue;
        }

        const response = await fetch("http://localhost:8080/images/upload", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(images),
        });

        if (!response.ok) {
          throw new Error(`HTTP error! Status: ${response.status}`);
        }

        const responseBody = await response.text();
        console.log("SessionKey retrieved: ", responseBody);
        return responseBody;

      } catch (error) {
        console.error(`Error during upload attempt ${attempt}:`, error);

        if (attempt === retries) {
          console.error("Max retries reached. Upload failed.");
          throw error;
        }

        console.warn(`Retrying upload... (${attempt + 1}/${retries})`);
        await new Promise((resolve) => setTimeout(resolve, 2000));
      }
    }

    return null;
  };


  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file && file.name.endsWith(".zip")) {
      extractZipWithDirectories(file).then(({ images: loadedImgs, directories: loadDirectories }) => {
        console.log("Extracted images:", loadedImgs);
        console.log("Extracted directories:", loadDirectories);
  
        if (!mainDirectory) {
          setSubDirectories((prevDirectories) => [...prevDirectories, ...loadDirectories]);
        }
        uploadDirectories(loadDirectories);
        // uploadPhotos(loadedImgs);
      });
    } else {
      alert("Please upload a valid ZIP file.");
    }
  };
  


  const fetchOriginalImage = async (imageKey: string, retries = 10): Promise<any | null> => {
    for (let attempt = 1; attempt <= retries; attempt++) {
      try {
        const isServerHealthy = await checkServerHealth();
        if (!isServerHealthy) {
          console.warn(`Server is unavailable. Attempt ${attempt} of ${retries}. Retrying...`);
          await new Promise((resolve) => setTimeout(resolve, 2000));
          continue;
        }

        const response = await fetch(
          `http://localhost:8080/images/original?imageKey=${imageKey}`,
          {
            method: "GET",
            headers: { "Content-Type": "application/json" },
          }
        );

        if (!response.ok && response.status !== 404) {
          throw new Error(`HTTP error! Status: ${response.status}`);
        }

        const imageData = await response.json();
        console.log("Fetched original image data:", imageData);
        return imageData;

      } catch (error) {
        console.error(`Error fetching image attempt ${attempt}:`, error);

        if (attempt === retries) {
          console.error("Max retries reached. Fetch failed.");
          throw error;
        }

        console.warn(`Retrying fetch... (${attempt + 1}/${retries})`);
        await new Promise((resolve) => setTimeout(resolve, 2000));
      }
    }

    return null;
  };


  const handleMouseEnter = async (image: ImageDto) => {
    setHoveredImageClicked(true);
    const fetchedImage = await fetchOriginalImage(image.imageKey);
    if (fetchedImage) {
      setHoveredImage(fetchedImage);
      setHoveredImageStyle({
        width: `${fetchedImage.width}px`,
        height: `${fetchedImage.height}px`,
      });
    }
  };

  const closeHoverImage = () => {
    setHoveredImage(null);
    setHoveredImageStyle({});
    setHoveredImageClicked(false);
  };

  return (
    <div className="main_container">
      <div className="top-bar">
        <Button
          variant="outlined"
          onClick={() => {
            setSessionOnly(false);
            setSessionKey({ sessionKey: "all", imageCount: 0 });
          }}
        >
          Load
        </Button>
        <Button
          variant="outlined"
          onClick={() => {
            setImageSize("small");
          }
          }
          className="top-bar-button"
          sx={{
            backgroundColor: imageSize === "small" ? "#d0e0e0" : "transparent"
          }}
        >
          Small
        </Button>
        <Button
          variant="outlined"
          onClick={() => {
            setImageSize("medium");
          }}
          className="top-bar-button"
          sx={{
            backgroundColor: imageSize === "medium" ? "#d0e0e0" : "transparent"
          }}
        >
          Medium
        </Button>
        <Button
          variant="outlined"
          onClick={() => {
            setImageSize("large");
          }}
          className="top-bar-button"
          sx={{
            backgroundColor: imageSize === "large" ? "#d0e0e0" : "transparent"
          }}
        >
          Large
        </Button>
        <Button
          variant="outlined"
          component="label"
          className="top-bar-button"
          sx={{ marginRight: 5 }}
        >
          Upload ZIP
          <input
            type="file"
            accept=".zip"
            onChange={handleFileChange}
            style={{ display: "none" }}
          />
        </Button>
      </div>
      <div className="image-grid">
        <h1>Photos - click to make them bigger!!!</h1>
        {(mainDirectory?.parentKey || firstPart) ? (
          <div
          onClick={() => getParentDirectory(mainDirectory?.parentKey || "", 10)}>
          <IconButton/>
          <FolderIcon style={{
            color: "#ffb74d",
            width: `${imageSize === "small" ? 100 : imageSize === "medium" ? 200 : 300}px`,
            height: `${imageSize === "small" ? 100 : imageSize === "medium" ? 200 : 300}px`,
          }} />
          <span>Parent Folder</span>
          </div>) : null}
        <div className="directory-grid">
          {subDirectories.map((directory) => (
            <div
              key={directory.directoryKey}
              className="directory-grid-item"
              onClick={() => setMainDirectory(directory)}
            >
              <FolderIcon style={{
              color: "#ffb74d",
              width: `${imageSize === "small" ? 100 : imageSize === "medium" ? 200 : 300}px`,
              height: `${imageSize === "small" ? 100 : imageSize === "medium" ? 200 : 300}px`,
              }} />
            <span>{directory.name}</span>
            </div>
          ))}
        </div>
        {images.map((image) => (
          <div
            key={image.imageKey}
            className="image-grid-item"
            onClick={() => handleMouseEnter(image)}
          >
            {(image.loaded && image.base64.includes(ERROR)) ?
              (
                <div
                  className="image-grid-placeholder damaged"
                  style={{
                    width: `${imageSize === "small" ? 100 : imageSize === "medium" ? 200 : 300}px`,
                    height: `${imageSize === "small" ? 100 : imageSize === "medium" ? 200 : 300}px`,
                  }}
                >
                  <img src={sad}                   
                  style={{
                    width: `${imageSize === "small" ? 40 : imageSize === "medium" ? 130 : 210}px`,
                    height: `${imageSize === "small" ? 40 : imageSize === "medium" ? 130 : 210}px`,
                  }}></img>
                  <p>Image "{image.name}" is damaged</p>

                </div>
              ) : (image.loaded) ? (
                <img
                  src={image.base64}
                  alt={`Image ${image.name}`}
                  className="image-grid-item-image"
                  style={{
                    width: `${image.width}px`,
                    height: `${image.height}px`,
                  }}
                />
              )
                : (
                  <div
                    className="image-grid-placeholder"
                    style={{
                      width: `${imageSize === "small" ? 100 : imageSize === "medium" ? 200 : 300}px`,
                      height: `${imageSize === "small" ? 100 : imageSize === "medium" ? 200 : 300}px`,
                    }}
                  >
                    <div
                      className="loader"
                      style={{
                        width: `${imageSize === "small" ? 12 : imageSize === "medium" ? 50 : 75}px`,
                        height: `${imageSize === "small" ? 12 : imageSize === "medium" ? 50 : 75}px`,
                      }}
                    ></div>
                  </div>
                )}
          </div>
        ))}
      </div>
      {hoveredImageClicked && (
        <div className="hover-image-window">
          <Button
            variant="contained"
            className="close-hover-image"
            onClick={closeHoverImage}
          >
            X
          </Button>
          {(hoveredImage && hoveredImage.base64.includes(ERROR)) ? (
            <div
              className="damaged-image">
                  <img src={sad}                   
                  style={{
                    width: `400px`,
                    height: `400px`,
                  }}></img>
                  <p>Image "{hoveredImage.name}" is damaged</p>
            </div>
          ) :
            (hoveredImage) ?
              (
                <img src={hoveredImage.base64} alt="Hovered Preview" />
              ) : (
                <div className="hover-image-grid-placeholder">
                  <div className="loader"></div>
                </div>
              )}
        </div>
      )}
    </div>
  );
};

export default ImageGrid;
