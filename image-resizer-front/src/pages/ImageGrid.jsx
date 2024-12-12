import React, { useState } from 'react';
import { fromEvent, takeWhile } from 'rxjs';
import { map } from 'rxjs/operators';
import { extractZip } from '../ImageUtils';
import './ImageGrid.css';
import Button from '@mui/material/Button';

const ImageGrid = () => {
    const [images, setImages] = useState([]);
    const [hoveredImage, setHoveredImage] = useState(null);
    const [hoveredImageStyle, setHoveredImageStyle] = useState({});
    const COMPLETE_REQUEST="COMPLETE_REQUEST"

    const loadPhotos = () => {
        console.log("Loading photos...")
        const eventSource = new EventSource('http://localhost:8080/images/original');

        const imageStream$ = fromEvent(eventSource, 'message').pipe(
            map((event) => {
                const parsedData = JSON.parse(event.data)
                return parsedData.body;
            }),
            takeWhile((imageData) => imageData.name !== COMPLETE_REQUEST || imageData.base64 !== COMPLETE_REQUEST, true)
        );

        const subscription = imageStream$.subscribe({
            next: (imageData) => {
                if (imageData.name === COMPLETE_REQUEST && imageData.base64 === COMPLETE_REQUEST) {
                    console.log("ended");
                    eventSource.close();
                } else  {
                    const newImage = { 
                        base64: `data:image/jpg;base64,${imageData.base64}`, 
                        name: imageData.name,
                        key: imageData.key,
                        loaded: true
                    };
                    console.log(newImage);
                    setImages((prevImages) =>
                        prevImages.some((image) => image.key === newImage.key)
                          ? prevImages.map((image) => (image.key === newImage.key ? newImage : image))
                          : [...prevImages, newImage]
                      );
                }

            },
            error: (err) => {
                console.error('Error receiving image data:', err);
            },
            complete: () => { console.log("Complete") }
        });

        return () => {
            eventSource.close();
            subscription.unsubscribe();
        };
    };

    const uploadPhotos = async (imageList) => {
        console.log("Starting photo upload...");

        const preparedImages = imageList.map(image => ({
            key: image.key,
            name: image.name,
            base64: image.base64,
            // width: image.width,
            // height: image.height
        }));

        try {
            const response = await uploadImagesToBackend(preparedImages);

            if (response) {
                console.log("All photos uploaded successfully.");
            } else {
                console.log("Failed to upload photos.");
            }

            loadPhotos();
        } catch (error) {
            console.error("Error uploading photos:", error);
        }
    };

    const uploadImagesToBackend = async (images) => {
        try {
            const response = await fetch('http://localhost:8080/images/upload', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(images)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status}`);
            }

            const responseBody = await response.json();
            console.log("Photos uploaded successfully: ", responseBody);
            return responseBody;
        } catch (error) {
            console.error('Error during fetch:', error);
            throw error;
        }
    };

    const handleFileChange = (event) => {
        const file = event.target.files[0];
        if (file && file.name.endsWith('.zip')) {
            extractZip(file)
            .then(loadedImgs => {
                setImages(loadedImgs);
                uploadPhotos(loadedImgs);
            })
        } else {
            alert('Please upload a valid ZIP file.');
        }
    };

    const handleMouseEnter = (image, event) => {
        try {
            // Upload original image from backend
            // placeholder
            setHoveredImage(image.base64);
            const rect = event.currentTarget.getBoundingClientRect();
            setHoveredImageStyle({
                top: `${rect.bottom + window.scrollY}px`,
                left: `${(rect.left) + window.scrollX}px`,
            });
        } catch (error) {
            console.error("Error fetching hover image:", error);
        }
    };

    const closeHoverImage = () => {
        setHoveredImage(null);
    };

    return (
        <div className='main_container'>
            <div className='top-bar'>
                <Button variant="outlined" onClick={loadPhotos}>Load</Button>
                <Button
                variant="outlined"
                component="label"
                className='top-bar-button'
                sx={{marginRight: 5 }}
                >
                Upload ZIP
                <input
                    type="file"
                    accept=".zip"
                    onChange={handleFileChange}
                    style={{ display: 'none' }}
                />
            </Button>
            </div>
            <div className='image-grid'>
                {images.map((image) => {
                    return (
                        <div 
                            key={image.key} 
                            className='image-grid-item'
                            onClick={(e) => handleMouseEnter(image, e)}
                            // style={{
                            //     width: `${image.width}px`,
                            //     height: `${image.height}px`,
                            // }}
                        >
                            {image.loaded ? (
                                <img
                                    src={image.base64}
                                    alt={`Image ${image.name}`}
                                    className='image-grid-item-image'
                                />
                            ) : (
                                <div className='image-grid-placeholder'>
                                    <span>Loading image...</span>
                                </div>
                            )}
                        </div>
                    );
                })}
            </div>
            {hoveredImage && (
                <div
                    className='hover-image-window'
                    style={{ ...hoveredImageStyle, position: 'absolute' }}
                >
                    <Button variant="contained" className='close-hover-image' onClick={closeHoverImage}>X</Button>
                    <img src={hoveredImage} alt="Hovered Preview" />
                </div>
            )}
        </div>
    );
};
export default ImageGrid;
