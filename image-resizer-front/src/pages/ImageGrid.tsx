import React, { useState, useEffect, useRef } from "react";
import { fromEvent, from, mergeMap, tap, finalize, catchError, Observable, EMPTY } from "rxjs";
import { map } from "rxjs/operators";
import { extractZip, Directory, DirKey } from "../ImageUtils.tsx";
import Button from "@mui/material/Button";
import { IconButton } from "@mui/material";
// import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import FolderIcon from "@mui/icons-material/Folder";
import "./ImageGrid.css";

type Image = {
  base64: string;
  name: string;
  imageKey: string;
  width?: number;
  height?: number;
  loaded: boolean;
};




const ImageGrid: React.FC = () => {
  const [images, setImages] = useState<Image[]>([]);
  const [subDirectories, setSubDirectories] = useState<Directory[]>([]);
  const [path, setPath]=useState<string>("/");
  const [page, setPage]=useState<number>(0);
  const [hoveredImage, setHoveredImage] = useState<Image | null>(null);
  const [hoveredImageStyle, setHoveredImageStyle] = useState<React.CSSProperties>({});
  const [hoveredImageClicked, setHoveredImageClicked] = useState(false);
  const [dirKey, setDirKey] = useState<DirKey>({
    dirKey: "root",
    name: "root",
    imageCount: 0,
    dirCount: 0
  });
  const [imageSize, setImageSize] = useState<String>("medium");
  const [sessionOnly, setSessionOnly] = useState<boolean>(true);
  const [activeSubscriptions, setActiveSubscriptions] = useState<Map<string, EventSource>>(new Map());
  const [dirEventSource, setDirEventSource] = useState<EventSource | null>(null); 
  const [isBottom, setIsBottom] = useState(false);

  const handleScroll = () => {
    const { scrollHeight, scrollTop, clientHeight } = document.documentElement;
    if (scrollTop + clientHeight >= scrollHeight - 5) { // Add a small buffer for precision
      setIsBottom(true);
    } else {
      setIsBottom(false);
    }
  };

  useEffect(() => {
    window.addEventListener("scroll", handleScroll);
    return () => {
      window.removeEventListener("scroll", handleScroll);
    };
  }, []);

  useEffect(() => {
    if (isBottom) {
      console.log("You have reached the bottom!");
      setPage((prev)=>prev+1);  
    }
  }, [isBottom]);

  const COMPLETE_REQUEST = "COMPLETE_REQUEST";
  const ERROR = "ERROR";
  const PAGE_SIZE=10;

  useEffect(() => {
    if (dirKey) {
      console.log("new root",dirKey)
      loadDirectories(sessionOnly, dirKey, imageSize);
      loadPhotos(sessionOnly, dirKey, imageSize);
    }
  }, [dirKey, page]);
  useEffect(()=>{
    setPage(0);
  },[dirKey])

  useEffect(() => {
    if (window.performance.navigation.type === 1 && !sessionStorage.getItem('refreshed')) {
      console.log("Page was refreshed");
      loadRootDirectory();
    }
  }, []); 

  useEffect(() => {

    loadPhotosByImageKey(imageSize);

  }, [imageSize])

  const loadRootDirectory=async ()=>{
    const response = await fetch(`http://localhost:8080/images/root`, {
      method: "GET",
      headers: { "Content-Type": "application/json" },
    });

    const key=await response.json();
    setDirKey({dirKey: key.directoryKey, imageCount: key.imageCount, name: key.name, dirCount: key.subDirectoriesCount});

  }

  const loadImageByImageKey = (size: String, imageKey: string): Observable<void> => {
    if (imageKey === null) return EMPTY;
    const url = `http://localhost:8080/images/resized/by-image-key?imageKey=${imageKey}&sizeString=${size}&page=${page}`;
    const eventSource = new EventSource(url);
    const imageSet = new Set<string>();

    if (activeSubscriptions.get(imageKey)) {
      const prevEventSource = activeSubscriptions.get(imageKey);
      // console.log(prevEventSource);
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
          const newImage: Image = {
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

  const loadParentDirectory=async() =>{
    const response = await fetch(`http://localhost:8080/images/parent?dirKey=${dirKey?.dirKey}`, {
      method: "GET",
      headers: { "Content-Type": "application/json" },
    });

    const parentKey=await response.json();
    activeSubscriptions.forEach((value, key)=>{
      value.close();
    })
    setActiveSubscriptions(new Map());
    setImages([]);
    setSubDirectories([]);

    setPath((prevPath) => {
      const lastSlashIndex = prevPath.lastIndexOf('/');
      const slice=prevPath.slice(0, lastSlashIndex);
      return lastSlashIndex !== -1 ? (slice!="" ? slice : "/") : prevPath;
    });

    console.log("Parent Key: ",parentKey);
    setDirKey({dirKey: parentKey.directoryKey, imageCount: parentKey.imageCount, name: parentKey.name, dirCount: parentKey.subDirectoriesCount});


  }

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
  const loadDirectories = (sessionOnly: boolean, dirKey: DirKey, size: String) => {
    console.log("Loading photos...");
    setSubDirectories([]);
    // console.log("Subdirectories: ",subDirectories);

    const url =`http://localhost:8080/images/directories/by-parent?dirKey=${dirKey.dirKey}`;

    const eventSource = new EventSource(url);
    const dirSet = new Set<string>();
    const dirCount = dirKey.dirCount;
    console.log("loadDirectories, dirKey",dirKey);
    console.log("loadDirecories",images)

    // images.forEach((image) => {
    //   const imageKey = image.imageKey;
    //   if (activeSubscriptions.get(imageKey)) {
    //     const prevEventSource = activeSubscriptions.get(imageKey);
    //     console.log(prevEventSource);
    //     prevEventSource?.close();
    //     setActiveSubscriptions((prev) => {
    //       const newMap = new Map(prev);
    //       newMap.delete(imageKey);

    //       return newMap;
    //     });
    //   }
    // }
    // );
    dirEventSource?.close();
    setDirEventSource(eventSource);
    

    const directoryStream$ = fromEvent<MessageEvent>(eventSource, "message").pipe(
      map((event) => {
        const parsedData = JSON.parse(event.data);
        return parsedData.body;
      })
    );

    const subscription = directoryStream$.subscribe({
      next: (dirData: any) => {
        if (
          dirData.name === COMPLETE_REQUEST &&
          dirData.base64 === COMPLETE_REQUEST
        ) {
          console.log("ended all");
          eventSource.close();
        } else if (
          dirData.name !== COMPLETE_REQUEST &&
          dirData.base64 !== COMPLETE_REQUEST
        ) {
          const newDirectory: Directory = {
            name: dirData.name,
            dirKey: dirData.directoryKey,
            imageCount: dirData.imageCount,
            subDirectoriesCount: dirData.subDirectoriesCount,
            directories: [], 
            images: [],
            hasParent: dirData.directoryKey != "root"
          };

          setSubDirectories((prev) => [...prev,newDirectory]);

          dirSet.add(newDirectory.dirKey);
          if (dirSet.size >= dirCount && sessionOnly) {
            console.log(dirKey.dirKey + ": ended", dirSet);
            eventSource.close();
          }
          console.log(dirSet, dirCount);

        }
      },
      error: (err) => {
        console.error("Error receiving directory data:", err);
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

  const loadPhotos = (sessionOnly: boolean, dirKey: DirKey, size: String) => {
    console.log("Loading photos...");

    const url = sessionOnly
      ? `http://localhost:8080/images/resized/by-directory?dirKey=${dirKey.dirKey}&sizeString=${size}&page=${page}`
      : `http://localhost:8080/images/resized/all?sizeString=${size}&page=${page}`;

    const eventSource = new EventSource(url);
    const imageSet = new Set<string>();
    const imageCount = Math.min(dirKey.imageCount, PAGE_SIZE);
    console.log(dirKey);

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
          const newImage: Image = {
            base64: `data:image/jpg;base64,${imageData.base64}`,
            name: imageData.name,
            imageKey: imageData.imageKey,
            width: imageData.width,
            height: imageData.height,
            loaded: true,
          };
          // console.log(newImage);

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
          console.log("imageCount: ",imageCount)
          if (imageSet.size >= imageCount) {
            console.log(dirKey.dirKey + ": ended", imageSet);
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

  const uploadPhotos = async (directory: Directory) => {
    console.log("Starting photo upload...");

    const imageCount = directory.images.length;

    try {
      const response = await uploadImagesToBackend(directory);

      if (response) {
        console.log("All photos uploaded successfully.");
        setSessionOnly(true);
        // setDirKey({ dirKey: directory.dirKey, imageCount: imageCount});
      } else {
        console.log("Failed to upload photos.");
      }
    } catch (error) {
      console.error("Error uploading photos:", error);
    }
  };

  const uploadImagesToBackend = async (directory: Directory, retries = 10): Promise<string | null> => {
    for (let attempt = 1; attempt <= retries; attempt++) {
      try {
        const isServerHealthy = await checkServerHealth();
        if (!isServerHealthy) {
          console.warn(`Server is unavailable. Attempt ${attempt} of ${retries}. Retrying...`);
          await new Promise((resolve) => setTimeout(resolve, 2000));
          continue;
        }

        const response = await fetch(`http://localhost:8080/images/upload/dir?directoryKey=${dirKey?.dirKey}`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(directory),
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
      extractZip(file,dirKey).then((loadedFile) => {
        console.log("imgs:", loadedFile);

        uploadPhotos(loadedFile);

        loadedFile.images.forEach((image) => {
          setImages((prev) => [...prev,image])
        })

        setDirKey((prev) => ({
          ...prev,
          imageCount: prev.imageCount + loadedFile.images.length,
          dirCount: prev.dirCount + loadedFile.directories.length,
        }));

        // loadDirectories(sessionOnly, dirKey, imageSize);
        // loadPhotos(sessionOnly, dirKey, imageSize);
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


  const handleMouseEnter = async (image: Image) => {
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

  const deleteDirectory = async (dirKey: string) => {
    try {
      const response = await fetch(`http://localhost:8080/images/directories/delete?dirKey=${dirKey}`, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
        },
      });
  
      if (response.ok) {
        console.log('Directory deleted successfully');

        setDirKey((prev) => ({
          ...prev,
          dirCount: prev.imageCount - 1,
        }));
      } else {
        console.error('Failed to delete directory');
      }
    } catch (error) {
      console.error('Error deleting directory:', error);
    }
  };

  const deleteImage = async (imgKey: string) => {
    try {
      const response = await fetch(`http://localhost:8080/images/delete?imageKey=${imgKey}`, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
        },
      });
  
      if (response.ok) {
        console.log('Image deleted successfully');
        setImages((prevImages) => prevImages.filter(image => image.imageKey !== imgKey));


        setDirKey((prev) => ({
          ...prev,
          imageCount: prev.imageCount - 1,
        }));
      } else {
        console.error('Failed to delete image');
      }
    } catch (error) {
      console.error('Error deleting image:', error);
    }
  };
  

  return (
    <div className="main_container">
      <div className="top-bar">
        <Button
          variant="outlined"
          onClick={() => {
            setSessionOnly(false);
            // setDirKey({ dirKey: "all", imageCount: 0 });
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
        <h2 style={{width: '100%',textAlign:'center'}}>Path: {path}</h2>
        {(dirKey?.dirKey!=null &&dirKey?.dirKey!="root") ? (
          <div  className="directory-grid-item"
          onClick={loadParentDirectory}>
            <IconButton/>
            <FolderIcon style={{
              color: "#ffb74d",
              width: `${imageSize === "small" ? 100 : imageSize === "medium" ? 200 : 300}px`,
              height: `${imageSize === "small" ? 100 : imageSize === "medium" ? 200 : 300}px`,
            }} />
            <span>Parent Folder</span>
          </div>) : null
        }
        {subDirectories.map((directory) => (
          <div
            key={directory.dirKey}
            className="directory-grid-item"
          >


            <FolderIcon style={{
              color: "#ffb74d",
              width: `${imageSize === "small" ? 100 : imageSize === "medium" ? 200 : 300}px`,
              height: `${imageSize === "small" ? 100 : imageSize === "medium" ? 200 : 300}px`,
            }} 
            onClick={() => {
              setSessionOnly(true);
              // setMainDirectory(directory);
              setImages([]);
              setSubDirectories([]);
              setDirKey({ dirKey: directory.dirKey, imageCount: directory.imageCount, name: directory.name, dirCount: directory.subDirectoriesCount });
              setPath((prev) => {if(prev!="/")
                return `${prev}/${directory.name}`;
                else return `${prev}${directory.name}`;});
                
            }}/>
            <span>{directory.name}</span>
            <Button onClick={()=>deleteDirectory(directory.dirKey)}>Delete</Button>

          </div>
        ))}
        {images.map((image) => (
          <div
            key={image.imageKey}
            className="image-grid-item"
            
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
                  <img src="/sad.png"
                    onClick={() => handleMouseEnter(image)}

                    alt="Sad face"
                    style={{
                      width: `${imageSize === "small" ? 40 : imageSize === "medium" ? 130 : 210}px`,
                      height: `${imageSize === "small" ? 40 : imageSize === "medium" ? 130 : 210}px`,
                    }}></img>
                  <p>Image "{image.name}" is damaged</p>
                  <Button onClick={()=>deleteImage(image.imageKey)}>Delete</Button>

                </div>
              ) : (image.loaded) ? (<div className="damaged-image" >
                <img
                  src={image.base64}
                  alt={`Image ${image.name}`}
                  className="image-grid-item-image"
                  style={{
                    width: `${image.width}px`,
                    height: `${image.height}px`,
                  }}
                  onClick={() => handleMouseEnter(image)}
                />
                <Button onClick={()=>deleteImage(image.imageKey)}>Delete</Button>
                </div>
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
                      onClick={() => handleMouseEnter(image)}
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
              <img src="/sad.png"
                alt="Sad face"
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
      {/* <Button onClick={()=>setPage((prev)=>prev+1)}>Load more</Button> */}
    </div>
  );
};

export default ImageGrid;
