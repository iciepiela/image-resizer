import React, { useState } from 'react';
import { fromEvent, takeWhile } from 'rxjs';
import { map } from 'rxjs/operators';
import { extractZip } from '../ImageUtils';
import './ImageGrid.css';

const ImageGrid = () => {
    const [images, setImages] = useState([]);
    const COMPLETE_REQUEST="COMPLETE_REQUEST"

    const loadPhotos = () => {
        console.log("Loading photos...")
        // setImages([])
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
                    // setImages((prevImages) => [...prevImages, newImage]);
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
        base64: image.base64
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
            .then(loadedImgs=>{
                setImages(loadedImgs);
                uploadPhotos(loadedImgs);

            })
        } else {
            alert('Please upload a valid ZIP file.');
        }
    };
    

    return (
        <div className='main_container'>
            <div className='top-bar'>
                <button onClick={loadPhotos} className='top-bar-button'>Load</button>
                <input type="file" onChange={handleFileChange} className='top-bar-input'/>
            </div>
            <div className='image-grid'>
                {images.map((image) => (
                    <div key={image.key} className='image-grid-item'
                    // style={{ margin: '10px', display: 'inline-block' }}
                    >
                        {image.loaded ? (
                            <img
                                src={image.base64}
                                alt={`Image ${image.name}`}
                                className='image-grid-item-image'
                                // style={{ width: '200px', height: 'auto', borderRadius: '8px' }}
                            />
                        ) : (
                            <div className='image-grid-placeholder'>
                                {/* style={{ width: '200px', height: '200px', display: 'flex', justifyContent: 'center', alignItems: 'center', background: '#f0f0f0', borderRadius: '8px' }} */}
                                <span>Loading image...</span>
                            </div>
                        )}
                </div>
            ))}
            </div>
        </div>
    );
};
export default ImageGrid;
