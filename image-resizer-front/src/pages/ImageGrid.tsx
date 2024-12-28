import React, { useState, useEffect } from "react";
import { fromEvent } from "rxjs";
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
  const [notSessionOnly, setNotSessionOnly] = useState<boolean>(false);

  const COMPLETE_REQUEST = "COMPLETE_REQUEST";

  useEffect(() => {
    if (sessionKey) {
      loadPhotos(false, sessionKey, imageSize);
    }
  }, [sessionKey]);

  const loadPhotos = (all: boolean, sessionKey: SessionKey,size: String) => {
    console.log("Loading photos...");
    const url = all
      ? `http://localhost:8080/images/resized/all?sizeString=${size}`
      : `http://localhost:8080/images/resized?sessionKey=${sessionKey.sessionKey}&sizeString=${size}`;
    const eventSource = new EventSource(url);
    const imageSet = new Set<string>();
    const imageCount = sessionKey.imageCount;

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
          all
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
              (image) => image.imageKey === newImage.imageKey && image.loaded === true
            );

            if (!imageExists) {
              const newImages = prevImages.some((image) => image.imageKey === newImage.imageKey)
                ? prevImages.map((image) =>
                    image.imageKey === newImage.imageKey ? newImage : image
                  )
                : [...prevImages, newImage];

              return newImages;
            }

            return prevImages;
          });

          imageSet.add(newImage.imageKey);
          if (imageSet.size >= imageCount && !all) {
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
        setSessionKey({ sessionKey: response, imageCount: imageCount });
      } else {
        console.log("Failed to upload photos.");
      }
    } catch (error) {
      console.error("Error uploading photos:", error);
    }
  };

  const uploadImagesToBackend = async (images: any[]) => {
    try {
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
      console.error("Error during fetch:", error);
      throw error;
    }
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

  const fetchOriginalImage = async (imageKey: string) => {
    try {
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
      return imageData;
    } catch (error) {
      console.error("Error fetching original image:", error);
      return null;
    }
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
            setNotSessionOnly(true);
            loadPhotos(true, { sessionKey: "all", imageCount: 0 }, imageSize)
            setSessionKey({sessionKey: "all", imageCount: 0 });
          }}
        >
          Load
        </Button>
        <Button
          variant="outlined"
          onClick={() => {
            setImageSize("small");
            setImages([]);
            if (sessionKey) {
              loadPhotos(notSessionOnly, { sessionKey: sessionKey.sessionKey, imageCount: 0 }, "small");
            }
            }
          }
          className="top-bar-button"
        >
          Small
        </Button>
        <Button
          variant="outlined"
          onClick={() => {
            setImageSize("medium");
            setImages([]);
            if (sessionKey) {
              loadPhotos(notSessionOnly, { sessionKey: sessionKey.sessionKey, imageCount: 0 }, "medium");
            }
          }}
          className="top-bar-button"
        >
          Medium
        </Button>
        <Button
          variant="outlined"
          onClick={() => {
            setImageSize("large");
            setImages([]);
            if (sessionKey) {
              loadPhotos(notSessionOnly, { sessionKey: sessionKey.sessionKey, imageCount: 0 }, "large");
            }
          }}
          className="top-bar-button"
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
