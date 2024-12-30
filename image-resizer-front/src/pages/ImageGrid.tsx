import React, { useState, useEffect } from "react";
import { fromEvent, from, mergeMap, tap, finalize, catchError, Observable, EMPTY } from "rxjs";
import { map } from "rxjs/operators";
import { extractZip } from "../ImageUtils.tsx";
import Button from "@mui/material/Button";
import "./ImageGrid.css";

type Image = {
  base64: string;
  name: string;
  imageKey: string;
  width?: number;
  height?: number;
  loaded: boolean;
};

type SessionKey = {
  sessionKey: string | null;
  imageCount: number;
};

const ImageGrid: React.FC = () => {
  const [images, setImages] = useState<Image[]>([]);
  const [hoveredImage, setHoveredImage] = useState<string | null>(null);
  const [hoveredImageStyle, setHoveredImageStyle] = useState<React.CSSProperties>({});
  const [hoveredImageClicked, setHoveredImageClicked] = useState(false);
  const [sessionKey, setSessionKey] = useState<SessionKey | undefined>();
  const [imageSize, setImageSize] = useState<String>("small");
  const [sessionOnly, setSessionOnly] = useState<boolean>(true);

  const COMPLETE_REQUEST = "COMPLETE_REQUEST";

  useEffect(() => {
    if (sessionKey) {
      loadPhotos(sessionOnly, sessionKey, imageSize);
    }
  }, [sessionKey]);

  useEffect(() => {
    if (sessionKey) {
      loadPhotosByImageKey(imageSize);
    }
  }, [imageSize])

  const loadImageByImageKey = (size: String, imageKey: String): Observable<void> => {
    console.log(imageKey);
    if (imageKey === null) return EMPTY;
    const url = `http://localhost:8080/images/resized?imageKey=${imageKey}&sizeString=${size}`;
    const eventSource = new EventSource(url);
    const imageSet = new Set<string>();

    return fromEvent<MessageEvent>(eventSource, "message").pipe(
      map((event) => JSON.parse(event.data).body),
      tap((imageData: any) => {
        if (imageData.name !== "COMPLETE_REQUEST" && imageData.base64 !== "COMPLETE_REQUEST") {
          const newImage: Image = {
            base64: `data:image/jpg;base64,${imageData.base64}`,
            name: imageData.name,
            imageKey: imageData.imageKey,
            width: imageData.width,
            height: imageData.height,
            loaded: true,
          };

          setImages((prevImages) => {
            const newImages = prevImages.map((image) =>
              image.imageKey === newImage.imageKey
                ? { ...image, ...newImage }
                : image
            );
            return newImages;
          });

          imageSet.add(newImage.imageKey);

          if (imageSet.size >= 1) {
            console.log(imageKey + ": ended imageKey", imageSet);
            eventSource.close();
          }
        }
      }),
      catchError((err) => {
        console.error(`Error loading image for key ${imageKey}:`, err);
        return []; // Handle errors gracefully by completing the stream
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

  const loadPhotos = (sessionOnly: boolean, sessionKey: SessionKey,size: String) => {
    console.log("Loading photos...");

    const url = sessionOnly
      ? `http://localhost:8080/images/resized?sessionKey=${sessionKey.sessionKey}&sizeString=${size}`
      : `http://localhost:8080/images/resized/all?sizeString=${size}`;

    const eventSource = new EventSource(url);
    const imageSet = new Set<string>();
    const imageCount = sessionKey.imageCount;
    console.log(sessionKey);

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
          const newImage: Image = {
            base64: `data:image/jpg;base64,${imageData.base64}`,
            name: imageData.name,
            imageKey: imageData.imageKey,
            width: imageData.width,
            height: imageData.height,
            loaded: true,
          };

          setImages((prevImages) => {
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
            console.log(sessionKey.sessionKey + ": ended", imageSet);
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

  const uploadPhotos = async (imageList: Image[]) => {
    console.log("Starting photo upload...");

    const preparedImages = imageList.map((image) => ({
      imageKey: image.imageKey,
      name: image.name,
      base64: image.base64,
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
      extractZip(file).then((loadedImgs) => {
        setImages((prevImages) => [...prevImages, ...loadedImgs]);
        uploadPhotos(loadedImgs);
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
  
        if (!response.ok) {
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
  

  const handleMouseEnter = async (image: Image) => {
    setHoveredImageClicked(true);
    const fetchedImage = await fetchOriginalImage(image.imageKey);
    if (fetchedImage) {
      setHoveredImage(fetchedImage.base64);
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
            setSessionKey({sessionKey: "all", imageCount: 0 });
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
        <h1>Photos - click to make it bigger!!!</h1>
        {images.map((image) => (
          <div
            key={image.imageKey}
            className="image-grid-item"
            onClick={() => handleMouseEnter(image)}
          >
            {image.loaded ? (
              <img
                src={image.base64}
                alt={`Image ${image.name}`}
                className="image-grid-item-image"
                style={{
                  width: `${image.width}px`,
                  height: `${image.height}px`,
                }}
              />
            ) : (
              <div 
              className="image-grid-placeholder"
                style={{
                  width: `${imageSize === "small" ? 50 : imageSize === "medium" ? 200 : 300}px`,
                  height: `${imageSize === "small" ? 50 : imageSize === "medium" ? 200 : 300}px`,
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
          {hoveredImage ? (
            <img src={hoveredImage} alt="Hovered Preview" />
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
