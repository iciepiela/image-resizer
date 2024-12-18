import React, { useState, useEffect } from "react";
import { fromEvent, takeWhile } from "rxjs";
import { map } from "rxjs/operators";
import { extractZip } from "../ImageUtils";
import Button from "@mui/material/Button";
import "./ImageGrid.css";
import { EventSourcePolyfill } from 'event-source-polyfill';

const ImageGrid = () => {
  const [images, setImages] = useState([]);
  const [hoveredImage, setHoveredImage] = useState(null);
  const [hoveredImageStyle, setHoveredImageStyle] = useState({});
  const [hoveredImageClicked, setHoveredImageClicked] = useState(false);
  const [sessionKey, setSessionKey] = useState();

  const COMPLETE_REQUEST = "COMPLETE_REQUEST";

  useEffect(() => {
    if (sessionKey) {
      loadPhotos();
    }
  }, [sessionKey]);


  const loadPhotos = (all) => {
    console.log("Loading photos...");
    const url = all
      ? `http://localhost:8080/images/resized/all`
      : `http://localhost:8080/images/resized?sessionKey=${sessionKey}`;

    const eventSource = new EventSource(url);

    const imageStream$ = fromEvent(eventSource, "message").pipe(
      map((event) => {
        const parsedData = JSON.parse(event.data);
        return parsedData.body;
      }),
      takeWhile(
        (imageData) =>
          imageData.name !== COMPLETE_REQUEST ||
          imageData.base64 !== COMPLETE_REQUEST,
        true,
      ),
    );

    const subscription = imageStream$.subscribe({
      next: (imageData) => {
        if (
          imageData.name === COMPLETE_REQUEST &&
          imageData.base64 === COMPLETE_REQUEST
        ) {
          console.log("ended");
          eventSource.close();
        } else {
          const newImage = {
            base64: `data:image/jpg;base64,${imageData.base64}`,
            name: imageData.name,
            imageKey: imageData.imageKey,
            width: imageData.width,
            height: imageData.height,
            loaded: true,
          };
          if (all) {
            setImages((prevImages) =>
              prevImages.some((image) => image.imageKey === newImage.imageKey)
                ? prevImages.map((image) => image)
                : [...prevImages, newImage],
            );
          } else {
            setImages((prevImages) =>
              prevImages.some((image) => image.imageKey === newImage.imageKey)
                ? prevImages.map((image) =>
                  image.imageKey === newImage.imageKey ? newImage : image,
                )
                : [...prevImages, newImage],
            );
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
      eventSource.close();
      subscription.unsubscribe();
    };
  };

  const uploadPhotos = async (imageList) => {

    for (let i = imageList.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));

      [imageList[i], imageList[j]] = [imageList[j], imageList[i]];
    }
    console.log("Starting photo upload...");

    const preparedImages = imageList.map((image) => ({
      imageKey: image.imageKey,
      name: image.name,
      base64: image.base64,
    }));

    const url = `http://localhost:8080/images/upload`;

    const response = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(preparedImages),
    });
    console.log(preparedImages)

    const reader = response.body.getReader();
    let done = false;
    var i = 0;
    var accumulatedData = "";
    while (!done) {
      const { chunkDone, value } = await reader.read();
      done = chunkDone || done;
      console.log(done)
      const decodedData = new TextDecoder().decode(value);
      console.log(accumulatedData)
      accumulatedData += decodedData.trim();

      console.log(accumulatedData)


      if (accumulatedData.includes("statusCode")) {


        const data = accumulatedData.split(/"statusCodeValue":200}/)[0].split(/data:(.*)/s)[1] + "\"statusCodeValue\":200}"
        accumulatedData = accumulatedData.split(/"statusCodeValue":200}(.*)/s)[1];
        console.log("data", data)
        if (data.includes(COMPLETE_REQUEST)) break;

        
        const parsedData = JSON.parse(data).body;
        console.log(parsedData.name)


        const newImage = {
          base64: `data:image/jpg;base64,${parsedData.base64}`,
          name: parsedData.name,
          imageKey: parsedData.imageKey,
          width: parsedData.width,
          height: parsedData.height,
          loaded: true,
        };

        setImages((prevImages) =>
          prevImages.some((image) => image.imageKey === newImage.imageKey)
            ? prevImages.map((image) =>
              image.imageKey === newImage.imageKey ? newImage : image
            )
            : [...prevImages, newImage]
        );

      }

    }

    console.log("Upload complete");
  };




  const handleFileChange = (event) => {
    const file = event.target.files[0];
    if (file && file.name.endsWith(".zip")) {
      extractZip(file).then((loadedImgs) => {
        setImages((prevImages) => [...prevImages, ...loadedImgs]);
        uploadPhotos(loadedImgs);
      });
    } else {
      alert("Please upload a valid ZIP file.");
    }
  };

  const fetchOriginalImage = async (imageKey) => {
    try {
      const response = await fetch(
        `http://localhost:8080/images/original?imageKey=${imageKey}`,
        {
          method: "GET",
          headers: { "Content-Type": "application/json" },
        },
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

  const handleMouseEnter = async (image) => {
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
        <Button variant="outlined" onClick={loadPhotos}>
          Load
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
        <h1>Photos - click to make it bigger</h1>
        {images.map((image) => (
          <div
            key={image.imageKey}
            className="image-grid-item"
            onClick={(e) => handleMouseEnter(image, e)}
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
              <div className="image-grid-placeholder">
                <div className="loader"></div>
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
