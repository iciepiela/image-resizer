import React, { useState, useEffect } from 'react';
import { fromEvent, takeWhile } from 'rxjs';
import { map } from 'rxjs/operators';
import { extractZip } from '../ImageUtils';

const ImageGrid = () => {
    const [images, setImages] = useState([]);
    const [sessionKey, setSessionKey] = useState();

    const COMPLETE_REQUEST = "COMPLETE_REQUEST"

    useEffect(() => {
        if (sessionKey) {
            loadPhotos();
        }
    }, [sessionKey]);

    const loadPhotos = (all) => {
        console.log("Loading photos...")
        // setImages([])
        const url = all ? `http://localhost:8080/images/resized/all` : `http://localhost:8080/images/resized?sessionKey=${sessionKey}`

        const eventSource = new EventSource(url);

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
                } else {
                    const newImage = {
                        base64: `data:image/jpg;base64,${imageData.base64}`,
                        name: imageData.name,
                        key: imageData.key,
                        loaded: true
                    };
                    console.log(newImage);
                    // setImages((prevImages) => [...prevImages, newImage]);
                    if (all) {
                        setImages((prevImages) =>
                            prevImages.some((image) => image.key === newImage.key)
                                ? prevImages.map((image) => (image))
                                : [...prevImages, newImage]
                        )
                    } else {
                        setImages((prevImages) =>
                            prevImages.some((image) => image.key === newImage.key)
                                ? prevImages.map((image) => (image.key === newImage.key ? newImage : image))
                                : [...prevImages, newImage]
                        );
                    }

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
            console.log(response);

            if (response) {
                console.log("All photos uploaded successfully.");
                setSessionKey(response);
            } else {
                console.log("Failed to upload photos.");
            }


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

            const responseBody = await response.text();
            console.log("SessionKey retrieved: ", responseBody);
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
                    setImages((prevImages) => [...prevImages, ...loadedImgs]);
                    uploadPhotos(loadedImgs);

                })
        } else {
            alert('Please upload a valid ZIP file.');
        }
    };


    return (
        <div>
            <button onClick={() => loadPhotos(true)}>Load</button>
            <input type="file" onChange={handleFileChange} />
            {images.map((image) => (
                <div key={image.key} style={{ margin: '10px', display: 'inline-block' }}>
                    {image.loaded ? (
                        <img
                            src={image.base64}
                            alt={`Image ${image.name}`}
                            style={{ width: '200px', height: 'auto', borderRadius: '8px' }}
                        />
                    ) : (
                        <div style={{ width: '200px', height: '200px', display: 'flex', justifyContent: 'center', alignItems: 'center', background: '#f0f0f0', borderRadius: '8px' }}>
                            <span>Loading image...</span>
                        </div>
                    )}
                </div>
            ))}
        </div>
    );
};
export default ImageGrid;
